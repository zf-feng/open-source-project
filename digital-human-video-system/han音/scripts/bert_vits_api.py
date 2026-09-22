# scripts/bert_vits_api.py
import sys
import json
import torch
import os
import soundfile as sf
from datetime import datetime
from tools.infer_tool import Svc, get_bert

MODEL = None
CONFIG_PATH = "./configs/config.json"
WEIGHT_PATH = "./checkpoints/G_10000.pth"

def load_model():
    global MODEL
    if MODEL is None:
        print("【日志】正在加载Bert-VITS音色模型...")
        device = "cuda" if torch.cuda.is_available() else "cpu"
        MODEL = Svc(WEIGHT_PATH, CONFIG_PATH, device=device)
        print("【日志】模型加载完成")
    return MODEL

def generate_audio(text, spk_id=0):
    model = load_model()
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_path = os.path.abspath(f"./output_audio_{timestamp}.wav")
    bert_feature = get_bert(text)
    audio_data, sample_rate = model.infer(
        text,
        spk_id=spk_id,
        bert_feat=bert_feature,
        noise_scale=0.7,
        length_scale=1.0
    )
    sf.write(output_path, audio_data, sample_rate)
    return output_path

if __name__ == "__main__":
    input_text = sys.argv[1] if len(sys.argv) > 1 else "数字人TTS语音测试"
    try:
        audio_full_path = generate_audio(input_text)
        res = {
            "status": "success",
            "audio_path": audio_full_path,
            "message": "音频生成完成，可传入Wav2Lip视频模块"
        }
    except Exception as err:
        res = {
            "status": "fail",
            "error_info": str(err)
        }
    print(json.dumps(res, ensure_ascii=False))