#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sys
import json
import os
from datetime import datetime

def generate_video(audio_path, ref_video_path=None):
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_path = f"./output/video/wav2lip_{timestamp}.mp4"
    os.makedirs("./output/video", exist_ok=True)
    
    # 创建一个空的占位文件
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write("")
    
    return output_path

if __name__ == "__main__":
    audio_path = sys.argv[1] if len(sys.argv) > 1 else "./output/audio/default.wav"
    ref_video = sys.argv[2] if len(sys.argv) > 2 else None
    
    try:
        video_path = generate_video(audio_path, ref_video)
        result = {"status": "success", "data": {"file_path": video_path}}
    except Exception as e:
        result = {"status": "error", "message": str(e)}
    
    print(json.dumps(result, ensure_ascii=False))