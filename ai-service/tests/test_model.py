from ultralytics import YOLO

MODEL_PATH = "yolov8l-seg.pt"
IMAGE_PATH = "test_image.jpg"

model = YOLO(MODEL_PATH)

results = model(IMAGE_PATH)

for result in results:

    print("Image :", IMAGE_PATH)

    if result.masks is None:
        print("Aucun masque détecté.")
        continue

    print("Nombre de masques :", len(result.masks))

    print("Classes :", result.boxes.cls)
    print("Confiances :", result.boxes.conf)