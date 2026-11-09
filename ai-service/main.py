from contextlib import asynccontextmanager
from pathlib import Path

import cv2
import numpy as np
import logging

from fastapi import FastAPI, UploadFile, File, HTTPException

from config.config_loader import load_config, get_cm_per_pixel
from services.model_service import ModelService
from services.inference_service import InferenceService
from services.measurement_service import MeasurementService
from schemas.response import AnalysisResponse, DetectionResponse


# ============================================================
# LOGGING
# ============================================================

logging.basicConfig(
    level=logging.INFO,
    format="%(levelname)s %(message)s"
)

logger = logging.getLogger(__name__)


# ============================================================
# SERVICES GLOBAUX
# Initialisés UNE SEULE FOIS au démarrage (événement lifespan),
# notamment le chargement du modèle YOLOv8-Seg — jamais par requête.
# ============================================================

config = None
cm_per_pixel = None
oversize_threshold_cm = None
model_service = None
inference_service = None
measurement_service = None


# ============================================================
# LIFESPAN - INITIALISATION AU DEMARRAGE
# ============================================================

@asynccontextmanager
async def lifespan(app: FastAPI):
    global config, cm_per_pixel
    global oversize_threshold_cm
    global model_service, inference_service, measurement_service

    logger.info("Loading configuration ...")
    config = load_config()
    cm_per_pixel = float(get_cm_per_pixel(config))

    seuil_mm = float(config["seuils_metier"]["seuil_roche_surdimensionnee_mm"])
    # Conversion mm -> cm avant comparaison (20 mm = 2.0 cm)
    oversize_threshold_cm = seuil_mm / 10.0

    logger.info(
        "Oversized threshold: %.1f mm -> %.2f cm",
        seuil_mm,
        oversize_threshold_cm
    )

    logger.info("Loading YOLOv8-Seg model ...")
    model_service = ModelService()
    model_service.load_model()
    inference_service = InferenceService(model_service)
    measurement_service = MeasurementService()

    logger.info("AI Service ready.")

    yield


# ============================================================
# FASTAPI
# ============================================================

app = FastAPI(
    title="OCP Screen Blockage Detection - AI Service",
    version="1.0.0",
    lifespan=lifespan
)


# ============================================================
# CONFIGURATION
# ============================================================

ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png"}


# ============================================================
# ROOT
# ============================================================

@app.get("/")
def root():
    return {
        "service": "AI Service",
        "status": "running"
    }


# ============================================================
# ANALYZE IMAGE
# ============================================================

@app.post("/analyze")
async def analyze(file: UploadFile = File(...)):

    logger.info("Image received")

    # --------------------------------------------------------
    # 1. Vérifier le nom du fichier
    # --------------------------------------------------------

    if not file.filename:
        logger.error("Invalid image: no file provided")

        raise HTTPException(
            status_code=400,
            detail="No file provided."
        )

    # --------------------------------------------------------
    # 2. Vérifier l'extension
    # --------------------------------------------------------

    extension = Path(file.filename).suffix.lower()

    if extension not in ALLOWED_EXTENSIONS:
        logger.error(
            "Invalid image format: %s",
            extension
        )

        raise HTTPException(
            status_code=400,
            detail="Unsupported image format. Allowed formats: JPG, JPEG, PNG."
        )

    # --------------------------------------------------------
    # 3. Lire le fichier
    # --------------------------------------------------------

    contents = await file.read()

    if not contents:
        logger.error("Invalid image: uploaded file is empty")

        raise HTTPException(
            status_code=400,
            detail="The uploaded file is empty."
        )

    # --------------------------------------------------------
    # 4. Décoder l'image avec OpenCV
    # --------------------------------------------------------

    image_array = np.frombuffer(contents, np.uint8)

    image = cv2.imdecode(
        image_array,
        cv2.IMREAD_COLOR
    )

    # --------------------------------------------------------
    # 5. Vérifier que l'image est réellement lisible
    # --------------------------------------------------------

    if image is None:
        logger.error("Invalid image: image cannot be decoded")

        raise HTTPException(
            status_code=400,
            detail="The uploaded file is not a valid or readable image."
        )

    # --------------------------------------------------------
    # 6. Vérifier que le service est prêt (modèle chargé)
    # --------------------------------------------------------

    if inference_service is None or measurement_service is None:
        logger.error("AI Service is not ready (model not loaded).")

        raise HTTPException(
            status_code=503,
            detail="AI Service is not ready. Please try again later."
        )

    # --------------------------------------------------------
    # Image validée
    # --------------------------------------------------------

    width = image.shape[1]
    height = image.shape[0]

    logger.info(
        "Image validated successfully: %s (%dx%d)",
        file.filename,
        width,
        height
    )

    # --------------------------------------------------------
    # 7. Inférence YOLOv8-Seg
    # --------------------------------------------------------

    results = inference_service.predict(image)

    # --------------------------------------------------------
    # 8. Transformation des résultats bruts en détections
    # --------------------------------------------------------

    raw_detections = inference_service.results_to_json(results)

    detections = raw_detections["detections"]

    logger.info("Inference done: %d detection(s)", len(detections))

    # --------------------------------------------------------
    # 9. Mesures : dimensions px -> cm, oversized, blockage %
    # --------------------------------------------------------

    response_detections = []
    mask_polygons = []

    for detection in detections:

        mask_polygon = detection["mask"]
        mask_polygons.append(mask_polygon)

        dimensions_px = measurement_service.calculate_dimensions(mask_polygon)

        width_cm = measurement_service.pixels_to_cm(
            dimensions_px["width_px"],
            cm_per_pixel
        )
        height_cm = measurement_service.pixels_to_cm(
            dimensions_px["height_px"],
            cm_per_pixel
        )

        oversized = measurement_service.calculate_oversized(
            width_cm,
            height_cm,
            oversize_threshold_cm
        )

        response_detections.append(
            DetectionResponse(
                class_id=detection["class_id"],
                class_name=detection["class_name"],
                confidence=detection["confidence"],
                width_cm=float(width_cm),
                height_cm=float(height_cm),
                oversized=bool(oversized),
                mask=mask_polygon
            )
        )

    blockage_percentage = measurement_service.calculate_blockage_percentage(
        mask_polygons,
        width,
        height
    )

    logger.info(
        "Blockage percentage: %.2f %%",
        blockage_percentage
    )

    # --------------------------------------------------------
    # 10. Construction de la réponse AnalysisResponse
    # --------------------------------------------------------

    response = AnalysisResponse(
        image_width=width,
        image_height=height,
        blockage_percentage=float(blockage_percentage),
        detections=response_detections
    )

    logger.info(
        "Analysis complete: %d detection(s), blockage=%.2f%%",
        len(response_detections),
        blockage_percentage
    )

    return response