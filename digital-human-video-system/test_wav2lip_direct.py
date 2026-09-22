import warnings
import os
import sys
import importlib.util
warnings.filterwarnings("ignore")
os.environ["TORCHDYNAMO_VERBOSE"] = "0"

import torch
print("torch imported", flush=True)

WAV2LIP_ROOT = r"D:\QT Code\QT1\peng视\Wav2Lip"
print(f"root: {WAV2LIP_ROOT}", flush=True)

os.chdir(WAV2LIP_ROOT)
sys.path.insert(0, WAV2LIP_ROOT)
print("chdir done", flush=True)

print("importing cv2...", flush=True)
import cv2
print("cv2 imported", flush=True)

print("importing audio...", flush=True)
import audio
print("audio imported", flush=True)

print("importing face_detection...", flush=True)
import face_detection
print("face_detection imported", flush=True)

print("importing models...", flush=True)
from models import Wav2Lip
print("models imported", flush=True)

print("importing infer...", flush=True)
infer_path = os.path.join(WAV2LIP_ROOT, "infer.py")
_spec = importlib.util.spec_from_file_location("wav2lip_infer", infer_path)
_mod = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_mod)
print("infer imported", flush=True)

print(f"version: {torch.__version__}", flush=True)
