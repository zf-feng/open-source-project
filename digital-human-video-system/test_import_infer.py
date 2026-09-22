# -*- coding: utf-8 -*-
import os, sys, json, warnings
warnings.filterwarnings("ignore")

WAV2LIP_ROOT = r"D:\QT Code\QT1\peng视\Wav2Lip"
print(f"WAV2LIP_ROOT: {WAV2LIP_ROOT}")
print(f"exists: {os.path.isdir(WAV2LIP_ROOT)}")

os.chdir(WAV2LIP_ROOT)
sys.path.insert(0, WAV2LIP_ROOT)

print("importing torch...")
import torch
print(f"torch version: {torch.__version__}")
print(f"cuda available: {torch.cuda.is_available()}")

print("importing infer module...")
import importlib.util
infer_path = os.path.join(WAV2LIP_ROOT, "infer.py")
_spec = importlib.util.spec_from_file_location("wav2lip_infer", infer_path)
_mod = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_mod)

print(f"has wav2lip_generate: {hasattr(_mod, 'wav2lip_generate')}")
print("done")
