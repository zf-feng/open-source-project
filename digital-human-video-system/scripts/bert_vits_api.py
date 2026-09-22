#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Bert-VITS 语音生成 API 桥接脚本
真实模型位于 ../han音/ 目录；模型缺失时自动降级为占位实现。
"""
import sys
import json
import os
import tempfile
import subprocess
from datetime import datetime

# 项目根目录（QT1）
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# 真实 Bert-VITS 模块根目录
BERT_VITS_ROOT = os.path.join(BASE_DIR, "han音")

# 模型与配置文件路径（请按实际情况修改）
MODEL_PATH = os.path.join(BERT_VITS_ROOT, "Data", "models", "G_78000.pth")
CONFIG_PATH = os.path.join(BERT_VITS_ROOT, "Data", "configs", "config.json")

# 运行 Bert-VITS 推理的 Python 解释器路径
# 注意：han音 需要 torch>=2.0，建议为其单独创建虚拟环境，再填写该环境的 python.exe 路径
BERT_VITS_PYTHON = r"C:\Users\hzh21\anaconda3\envs\bertvits\python.exe"

# 音色映射：界面选项 -> config.json 中的 speaker 名称
VOICE_MAP = {
    "默认男声": "丹恒",
    "默认女声": "三月七",
    "自定义克隆": "三月七",
}


def _placeholder_audio():
    """模型缺失时生成空占位音频文件，保证界面流程不中断。"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_dir = os.path.join(BASE_DIR, "output", "audio")
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, f"bert_{timestamp}.wav")
    with open(output_path, "w", encoding="utf-8") as f:
        f.write("")
    return output_path


def _load_speakers_from_config():
    """从 config.json 读取可用的 speaker 列表。"""
    if not os.path.isfile(CONFIG_PATH):
        return []
    try:
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            cfg = json.load(f)
        return list(cfg.get("data", {}).get("spk2id", {}).keys())
    except Exception:
        return []


def _build_runner_script(text, speaker, output_path, ref_audio_path=None, length_scale=1.0):
    """生成在 han音 目录下执行推理的临时 runner 脚本。"""
    return f"""# -*- coding: utf-8 -*-
import os, sys, json, warnings
warnings.filterwarnings("ignore")

# 切换到 han音 目录，使其相对路径配置生效
os.chdir({json.dumps(BERT_VITS_ROOT)})
sys.path.insert(0, {json.dumps(BERT_VITS_ROOT)})

import torch
import numpy as np
import soundfile as sf
import utils
from infer import infer, latest_version, get_net_g
from config import config

device = config.webui_config.device if hasattr(config, "webui_config") else ("cuda" if torch.cuda.is_available() else "cpu")
hps = utils.get_hparams_from_file({json.dumps(CONFIG_PATH)})
version = hps.version if hasattr(hps, "version") else latest_version
net_g = get_net_g(
    model_path={json.dumps(MODEL_PATH)},
    version=version,
    device=device,
    hps=hps,
)

ref_audio = {repr(ref_audio_path)}

audio = infer(
    text={json.dumps(text)},
    emotion="普通",
    sdp_ratio=0.5,
    noise_scale=0.6,
    noise_scale_w=0.9,
    length_scale={length_scale},
    sid={json.dumps(speaker)},
    language="ZH",
    hps=hps,
    net_g=net_g,
    device=device,
    reference_audio=ref_audio,
    style_text=None,
    style_weight=0.7,
)

# 转换为 16bit PCM 并写入文件
audio_int16 = (audio * 32767).astype(np.int16)
os.makedirs(os.path.dirname({json.dumps(output_path)}), exist_ok=True)
sf.write({json.dumps(output_path)}, audio_int16, hps.data.sampling_rate)
print(json.dumps({{"status": "success", "file_path": {json.dumps(output_path)}}}, ensure_ascii=False))
"""


def _run_real_inference(text, speaker, ref_audio_path=None, length_scale=1.0):
    """在独立子进程中调用 han音 的真实 Bert-VITS 推理。"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_dir = os.path.join(BASE_DIR, "output", "audio")
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, f"bert_{timestamp}.wav")

    runner_code = _build_runner_script(text, speaker, output_path, ref_audio_path, length_scale)
    runner_path = os.path.join(tempfile.gettempdir(), f"bert_vits_runner_{timestamp}.py")
    with open(runner_path, "w", encoding="utf-8") as f:
        f.write(runner_code)

    try:
        result = subprocess.run(
            [BERT_VITS_PYTHON, runner_path],
            capture_output=True,
            text=True,
            cwd=BERT_VITS_ROOT,
            timeout=300,
        )
    finally:
        try:
            os.remove(runner_path)
        except Exception:
            pass

    if result.returncode != 0:
        raise RuntimeError(f"Bert-VITS 推理失败: {result.stderr or result.stdout}")

    # 解析 runner 输出的 JSON
    last_line = result.stdout.strip().splitlines()[-1] if result.stdout.strip() else ""
    try:
        data = json.loads(last_line)
        return data.get("file_path", output_path)
    except json.JSONDecodeError:
        return output_path


def generate_audio(text, voice_type="默认男声", ref_audio_path=None, length_scale=1.0):
    """生成音频：优先调用真实模型，模型缺失则降级为占位实现。"""
    if not text or not text.strip():
        raise ValueError("文本不能为空")

    # 若配置文件中不存在映射的 speaker，则回退到第一个可用 speaker
    speaker = VOICE_MAP.get(voice_type, "三月七")
    available_speakers = _load_speakers_from_config()
    if available_speakers and speaker not in available_speakers:
        speaker = available_speakers[0]

    if not os.path.isfile(MODEL_PATH):
        print(
            f"[警告] 未找到 Bert-VITS 模型权重: {MODEL_PATH}，请放置模型后重试。当前返回占位音频。",
            file=sys.stderr,
        )
        return _placeholder_audio()

    return _run_real_inference(text, speaker, ref_audio_path, length_scale)


if __name__ == "__main__":
    text = sys.argv[1] if len(sys.argv) > 1 else "你好"
    voice = sys.argv[2] if len(sys.argv) > 2 else "默认男声"
    ref_audio = sys.argv[3] if len(sys.argv) > 3 else None
    if ref_audio == "":
        ref_audio = None
    length_scale = float(sys.argv[4]) if len(sys.argv) > 4 else 1.0

    try:
        audio_path = generate_audio(text, voice, ref_audio, length_scale)
        result = {"status": "success", "data": {"file_path": audio_path}}
    except Exception as e:
        result = {"status": "error", "message": str(e)}

    print(json.dumps(result, ensure_ascii=False))
