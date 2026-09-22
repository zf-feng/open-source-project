#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Wav2Lip 视频生成 API 桥接脚本
真实模型位于 ../peng视/Wav2Lip/ 目录；模型缺失时自动降级为占位实现。
"""
import sys
import json
import os
import tempfile
import warnings
import types
import importlib.util
from datetime import datetime

# 默认强制使用 CPU，避免 torch.cuda.is_available() 在某些环境下卡死
# 如需启用 GPU，可在调用前设置环境变量 CUDA_VISIBLE_DEVICES=0
os.environ.setdefault("CUDA_VISIBLE_DEVICES", "-1")

# 项目根目录（QT1）
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# 真实 Wav2Lip 模块根目录
WAV2LIP_ROOT = os.path.join(BASE_DIR, "peng视", "Wav2Lip")

# 默认参考视频/人脸素材（未上传时使用）
DEFAULT_FACE = os.path.join(BASE_DIR, "data", "reference", "ref_video.mp4")

# 模型检查点路径（请按实际情况修改）
CHECKPOINT_PATH = os.path.join(WAV2LIP_ROOT, "checkpoints", "wav2lip_gan.pth")

# 运行 Wav2Lip 推理的 Python 解释器路径
WAV2LIP_PYTHON = r"C:\Users\hzh21\anaconda3\envs\wav2lip\python.exe"


def _placeholder_video():
    """模型缺失时生成空占位视频文件，保证界面流程不中断。"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_dir = os.path.join(BASE_DIR, "output", "video")
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, f"wav2lip_{timestamp}.mp4")
    with open(output_path, "w", encoding="utf-8") as f:
        f.write("")
    return output_path


def _load_infer_module():
    """直接加载 peng视/Wav2Lip/infer.py 模块，避免子进程输出丢失。"""
    original_dir = os.getcwd()
    original_path = list(sys.path)
    try:
        os.chdir(WAV2LIP_ROOT)
        if WAV2LIP_ROOT not in sys.path:
            sys.path.insert(0, WAV2LIP_ROOT)

        warnings.filterwarnings("ignore")
        os.environ["TORCHDYNAMO_VERBOSE"] = "0"

        # numpy 兼容补丁（旧版代码可能引用 np.float/int/bool）
        import numpy as _np
        for _old, _new in [("float", float), ("int", int), ("bool", bool)]:
            if not hasattr(_np, _old):
                setattr(_np, _old, _new)

        infer_path = os.path.join(WAV2LIP_ROOT, "infer.py")
        _spec = importlib.util.spec_from_file_location("wav2lip_infer", infer_path)
        _mod = importlib.util.module_from_spec(_spec)
        _spec.loader.exec_module(_mod)
        return _mod
    finally:
        os.chdir(original_dir)
        sys.path[:] = original_path


def _run_real_inference(face_path, audio_path):
    """直接调用 peng视/Wav2Lip 的真实推理。"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_dir = os.path.join(BASE_DIR, "output", "video")
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, f"wav2lip_{timestamp}.mp4")

    _mod = _load_infer_module()
    result = _mod.wav2lip_generate(
        face_path=face_path,
        audio_path=audio_path,
        out_video_path=output_path,
        checkpoint_path=CHECKPOINT_PATH,
        resize_factor=1,
        fps=25.0,
    )
    return result if result and os.path.isfile(result) else output_path


def generate_video(audio_path, ref_video_path=None):
    """生成视频：优先调用真实模型，模型缺失则降级为占位实现。"""
    if not audio_path or not os.path.isfile(audio_path):
        raise FileNotFoundError(f"输入音频不存在: {audio_path}")

    face_path = ref_video_path if ref_video_path and os.path.isfile(ref_video_path) else DEFAULT_FACE
    if not os.path.isfile(face_path):
        print(
            f"[警告] 未找到参考视频: {face_path}，且 Wav2Lip 检查点缺失，返回占位视频。",
            file=sys.stderr,
        )
        return _placeholder_video()

    if not os.path.isfile(CHECKPOINT_PATH):
        print(
            f"[警告] 未找到 Wav2Lip 模型检查点: {CHECKPOINT_PATH}，请放置模型后重试。当前返回占位视频。",
            file=sys.stderr,
        )
        return _placeholder_video()

    return _run_real_inference(face_path, audio_path)


if __name__ == "__main__":
    default_audio = os.path.join(BASE_DIR, "output", "audio", "default.wav")
    audio_path = sys.argv[1] if len(sys.argv) > 1 else default_audio
    ref_video = sys.argv[2] if len(sys.argv) > 2 else None

    try:
        video_path = generate_video(audio_path, ref_video)
        result = {"status": "success", "data": {"file_path": video_path}}
    except Exception as e:
        import traceback
        result = {"status": "error", "message": f"{type(e).__name__}: {e}\n{traceback.format_exc()}"}

    print(json.dumps(result, ensure_ascii=False), flush=True)
