import cv2
import numpy as np


class MeasurementService:

    def calculate_dimensions(self, mask_polygon):
        if not mask_polygon:
            return {
                "width_px": 0.0,
                "height_px": 0.0
            }

        points = np.array(mask_polygon, dtype=np.float32)

        x, y, width, height = cv2.boundingRect(points)

        return {
            "width_px": float(width),
            "height_px": float(height)
        }

    def pixels_to_cm(self, pixels, cm_per_pixel):
        if pixels <= 0:
            return 0.0

        return float(pixels) * cm_per_pixel

    def calculate_oversized(self, width_cm, height_cm, max_size_cm):
        return width_cm > max_size_cm or height_cm > max_size_cm
    
    def calculate_blockage_percentage(self, mask_polygons, image_width, image_height):
        if image_width <= 0 or image_height <= 0:
            return 0.0

        # Masque global de l'image
        union_mask = np.zeros(
            (image_height, image_width),
            dtype=np.uint8
        )

        # Ajouter chaque masque dans le masque global
        for polygon in mask_polygons:

            if not polygon:
                continue

            points = np.array(
                polygon,
                dtype=np.int32
            )

            cv2.fillPoly(
                union_mask,
                [points],
                1
            )

        # Nombre de pixels occupés par l'union des masques
        occupied_pixels = int(np.count_nonzero(union_mask))

        # Nombre total de pixels de l'image
        total_pixels = image_width * image_height

        # Pourcentage de blocage
        blockage_percentage = (
            occupied_pixels / total_pixels
        ) * 100

        return float(blockage_percentage)