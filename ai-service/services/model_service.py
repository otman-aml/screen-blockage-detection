from pathlib import Path

from ultralytics import YOLO


MODEL_PATH = Path(__file__).resolve().parent.parent / "yolov8n-seg.pt"


class ModelService:

    def __init__(self):
        self.model = None

    def load_model(self):
        if not MODEL_PATH.exists():
            raise FileNotFoundError(
                f"Model file not found: {MODEL_PATH}"
            )

        self.model = YOLO(str(MODEL_PATH))

        print(f"YOLO model loaded successfully: {MODEL_PATH}")
        print(f"Model classes: {self.model.names}")

    def get_model(self):
        if self.model is None:
            raise RuntimeError("YOLO model is not loaded.")

        return self.model