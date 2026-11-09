class InferenceService:

    def __init__(self, model_service):
        self.model_service = model_service

    def predict(self, image):
        model = self.model_service.get_model()

        results = model(image)

        return results

    def results_to_json(self, results):
        detections = []

        for result in results:

            if result.boxes is None:
                continue

            for i in range(len(result.boxes)):

                class_id = int(result.boxes.cls[i].item())
                confidence = float(result.boxes.conf[i].item())

                class_name = result.names[class_id]

                mask_polygon = []

                if result.masks is not None:
                    mask_polygon = result.masks.xy[i].tolist()

                detection = {
                    "class_id": class_id,
                    "class_name": class_name,
                    "confidence": confidence,
                    "mask": mask_polygon
                }

                detections.append(detection)

        return {
            "detections": detections
        }