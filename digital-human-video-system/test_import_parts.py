# -*- coding: utf-8 -*-
import os, sys, warnings
warnings.filterwarnings("ignore")

WAV2LIP_ROOT = r"D:\QT Code\QT1\peng视\Wav2Lip"
os.chdir(WAV2LIP_ROOT)
sys.path.insert(0, WAV2LIP_ROOT)

import torch
print("torch imported")

print("importing audio...")
import audio
print("audio imported")

print("importing face_detection...")
import face_detection
print("face_detection imported")

print("importing models...")
from models import Wav2Lip
print("models imported")

print("all parts imported")
