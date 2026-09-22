# -*- coding: utf-8 -*-
import os, sys, json, warnings
warnings.filterwarnings("ignore")
os.environ["TORCHDYNAMO_VERBOSE"] = "0"

# 切换到 Wav2Lip 目录，使其相对路径配置生效
os.chdir("D:\\QT Code\\QT1\\peng\u89c6\\Wav2Lip")
sys.path.insert(0, "D:\\QT Code\\QT1\\peng\u89c6\\Wav2Lip")

# GPU 显存限制
import torch
if torch.cuda.is_available():
    torch.cuda.set_per_process_memory_fraction(0.80)

# ffmpeg 自动发现
try:
    import imageio_ffmpeg
    _ff = imageio_ffmpeg.get_ffmpeg_exe()
    os.environ["PATH"] = os.path.dirname(_ff) + os.pathsep + os.environ.get("PATH", "")
except Exception:
    pass

# torchvision / numpy 兼容补丁
import torchvision.transforms.functional as _F
import types as _types
if 'torchvision.transforms.functional_tensor' not in sys.modules:
    _shim = _types.ModuleType("torchvision.transforms.functional_tensor")
    _shim.rgb_to_grayscale = _F.rgb_to_grayscale
    sys.modules['torchvision.transforms.functional_tensor'] = _shim

import numpy as _np
for _old, _new in [('float', float), ('int', int), ('bool', bool)]:
    if not hasattr(_np, _old):
        setattr(_np, _old, _new)

# 加载 Wav2Lip infer 模块
import importlib.util
infer_path = os.path.join("D:\\QT Code\\QT1\\peng\u89c6\\Wav2Lip", "infer.py")
_spec = importlib.util.spec_from_file_location("wav2lip_infer", infer_path)
_mod = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_mod)

face_path = "D:\\QT Code\\QT1\\data\\reference\\ref_video.mp4"
audio_path = "D:\\QT Code\\QT1\\output\\audio\\bert_20260716_014511.wav"
out_path = "D:\\QT Code\\QT1\\output\\video\\wav2lip_20260716_022023.mp4"
checkpoint = "D:\\QT Code\\QT1\\peng\u89c6\\Wav2Lip\\checkpoints\\wav2lip_gan.pth"

print("[Wav2Lip] 开始加载模型...")
try:
    print(f"[Wav2Lip] 人脸素材: {face_path}")
    print(f"[Wav2Lip] 音频素材: {audio_path}")
    result = _mod.wav2lip_generate(
        face_path=face_path,
        audio_path=audio_path,
        out_video_path=out_path,
        checkpoint_path=checkpoint,
        resize_factor=1,
        fps=25.0,
    )
    print(f"[Wav2Lip] 生成完成: {result}")
    print(json.dumps({"status": "success", "file_path": result}, ensure_ascii=False))
except Exception as _e:
    print(f"[FAIL] {type(_e).__name__}: {_e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)
