from typing import List
from pydantic import BaseModel


class DetectionResponse(BaseModel):
    class_id: int
    class_name: str
    confidence: float

    width_cm: float
    height_cm: float

    oversized: bool

    mask: List[List[float]]


class AnalysisResponse(BaseModel):
    image_width: int
    image_height: int

    blockage_percentage: float

    detections: List[DetectionResponse]