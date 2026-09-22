#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试用 TTS 脚本
接收文本参数，返回一个模拟的音频文件路径
"""

import sys
import json
import os
from datetime import datetime

def generate_audio(text):
    """模拟语音生成，返回音频文件路径"""
    
    # 打印接收到的参数（用于调试）
    print(f"[Python] 接收到的文本: {text}", file=sys.stderr)
    
    # 生成带时间戳的文件名
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_path = f"./output_audio_{timestamp}.wav"
    
    # 创建 output 目录（如果不存在）
    os.makedirs("./output", exist_ok=True)
    
    # 创建一个空文件（模拟音频生成）
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write("")  # 占位文件
    
    # 返回 JSON 格式结果
    result = {
        "status": "success",
        "data": {
            "file_path": output_path
        },
        "message": ""
    }
    
    print(json.dumps(result, ensure_ascii=False))
    return result

if __name__ == "__main__":
    # 获取命令行参数
    if len(sys.argv) > 1:
        text = sys.argv[1]
    else:
        text = "默认文本"
    
    generate_audio(text)