#!/usr/bin/env python
"""Camera Simulator — OCP Screen Blockage Detection (Phase 7).

Simule une camera industrielle qui envoie periodiquement des images au
backend Spring Boot (POST /api/detections). Le pipeline reel est utilise :

    image -> camera simulator -> Spring Boot -> AI Service -> YOLO
          -> PostgreSQL -> WebSocket -> Angular

Deux modes, clairement distincts dans les logs :

    PRODUCTION MODE
        1 image toutes les 30 secondes (PRODUCTION_INTERVAL_SECONDS).
        Cadence de production du systeme industriel.

    ACCELERATED DEMO MODE (--demo)
        1 image/seconde par defaut. NOT PRODUCTION CADENCE.
        Reserve aux demonstrations et tests rapides. Jamais presentee
        comme la cadence de production.

Aucune decision metier n'est prise ici : le simulateur se contente
d'envoyer les images dans l'ordre alphabetique, le classement
NORMAL/WARNING/CRITICAL est produit par Spring Boot (Phase 4).

Usage :
    python camera_simulator.py                      # production, 30 s/image
    python camera_simulator.py --interval 30        # production explicite
    python camera_simulator.py --demo               # demo acceleree 1 s
    python camera_simulator.py --interval 2 --demo  # demo 2 s/image
    python camera_simulator.py --once               # un seul passage
"""

from __future__ import annotations

import argparse
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

import requests

# --- Configuration ---------------------------------------------------------

# Dossier des images simulant la camera (images de test utilisateur).
DEFAULT_IMAGE_DIR = Path(__file__).resolve().parent.parent / "test-data" / "camera-images"

# URL du backend Spring Boot (voir backend/src/main/resources/application.yml).
DEFAULT_BACKEND_URL = "http://localhost:8080/api/detections"

# Cadence de PRODUCTION du systeme industriel : 1 image / 30 secondes.
PRODUCTION_INTERVAL_SECONDS = 30

# Cadence du mode DEMO (acceleree, NON representatif de la production).
DEFAULT_DEMO_INTERVAL_SECONDS = 1

# Image technique du cas limite "0 detection" (fond uni, hors camera-images).
EDGE_CASE_IMAGE = Path(__file__).resolve().parent.parent / "test-data" / "edge_case_empty.jpg"


def log(mode: str, message: str) -> None:
    """Journal horloge locale avec le mode bien visible."""
    stamp = datetime.now(timezone.utc).strftime("%H:%M:%S")
    print(f"[{stamp}] [{mode}] {message}", flush=True)


def list_images(image_dir: Path) -> list[Path]:
    """Liste les images a envoyer, triees par nom (image_001 -> image_006)."""
    images = sorted(
        p for p in image_dir.iterdir()
        if p.is_file() and p.suffix.lower() in {".jpg", ".jpeg", ".png"}
    )
    if not images:
        log("ERROR", f"Aucune image trouvee dans {image_dir}")
        sys.exit(1)
    return images


def send_image(backend_url: str, image: Path) -> dict | None:
    """Envoie une image au backend et retourne le resume JSON recu (ou None)."""
    try:
        with image.open("rb") as fh:
            response = requests.post(
                backend_url,
                files={"file": (image.name, fh, "image/jpeg")},
                timeout=60,
            )
    except requests.RequestException as exc:
        log("ERROR", f"Envoi impossible ({image.name}): {exc}")
        return None

    if response.status_code != 201:
        log("ERROR", f"{image.name} -> HTTP {response.status_code}: {response.text[:200]}")
        return None

    try:
        result = response.json()
    except ValueError:
        log("ERROR", f"{image.name} -> reponse non-JSON")
        return None

    log(
        "ANALYSIS",
        f"{image.name} -> HTTP 201 | id={result.get('id')} "
        f"rocks={result.get('numberOfDetections')} "
        f"blockage={result.get('blockagePercentage')}% "
        f"size={result.get('imageWidth')}x{result.get('imageHeight')}",
    )
    return result


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Simulateur de camera OCP (envoi d'images au backend).",
    )
    parser.add_argument(
        "--interval", type=float, default=PRODUCTION_INTERVAL_SECONDS,
        help=f"Secondes entre deux images (defaut : {PRODUCTION_INTERVAL_SECONDS}).",
    )
    parser.add_argument(
        "--demo", action="store_true",
        help="ACCELERATED DEMO MODE (NOT PRODUCTION CADENCE) : cadence de "
             f"demonstration (defaut {DEFAULT_DEMO_INTERVAL_SECONDS}s via --interval).",
    )
    parser.add_argument("--image-dir", type=Path, default=DEFAULT_IMAGE_DIR)
    parser.add_argument("--backend-url", default=DEFAULT_BACKEND_URL)
    parser.add_argument(
        "--once", action="store_true",
        help="Un seul passage sur le dossier puis arret.",
    )
    parser.add_argument(
        "--include-edge-case", action="store_true",
        help="Ajoute l'image technique edge_case_empty.jpg (0 detection) au cycle.",
    )
    args = parser.parse_args()

    if args.demo:
        mode = "ACCELERATED DEMO MODE (NOT PRODUCTION CADENCE)"
        interval = (args.interval if args.interval != PRODUCTION_INTERVAL_SECONDS
                    else DEFAULT_DEMO_INTERVAL_SECONDS)
    else:
        mode = "PRODUCTION MODE"
        interval = args.interval

    images = list_images(args.image_dir)
    if args.include_edge_case and EDGE_CASE_IMAGE.exists():
        images.append(EDGE_CASE_IMAGE)

    log(mode, "=" * 60)
    log(mode, f"Images: {[p.name for p in images]}")
    log(mode, f"Backend: {args.backend_url}")
    log(mode, f"Cadence: 1 image / {interval} secondes")
    if args.demo:
        log(mode, "ATTENTION : cadence acceleree de demonstration, "
                  "la cadence de production reelle est 30 s/image.")
    log(mode, "=" * 60)

    try:
        while True:
            for image in images:
                log(mode, f"Capture simulee -> {image.name}")
                send_image(args.backend_url, image)
                if image != images[-1]:
                    time.sleep(interval)
            if args.once:
                log(mode, "Un seul passage termine.")
                break
            log(mode, "Fin du dossier -> nouvelle boucle (camera continue).")
            time.sleep(interval)
    except KeyboardInterrupt:
        log(mode, "Arret demande (Ctrl+C). Simulateur termine.")


if __name__ == "__main__":
    main()
