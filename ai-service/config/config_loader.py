import yaml
from pathlib import Path


CONFIG_PATH = Path(__file__).parent / "config.yaml"


def load_config():
    with open(CONFIG_PATH, "r", encoding="utf-8") as file:
        return yaml.safe_load(file)


def get_cm_per_pixel(config):
    return config["calibration"]["cm_per_pixel"]