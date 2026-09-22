#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI数字人视频生成系统 - Gradio WebUI
集成 Bert-VITS（语音生成）和 Wav2Lip（视频生成）
"""

import gradio as gr
import subprocess
import json
import os
from datetime import datetime

# ============================================================
#  核心生成函数
# ============================================================

def generate_digital_human(text, voice_type, ref_audio, ref_video):
    """
    生成数字人视频的主函数
    参数:
        text: 输入的文本
        voice_type: 音色选择 ("默认男声" / "默认女声" / "自定义克隆")
        ref_audio: 参考音频文件路径（自定义克隆时使用）
        ref_video: 参考视频文件路径（指定人物模型时使用）
    返回:
        生成的视频文件路径
    """
    # 1. 校验输入
    if not text or text.strip() == "":
        return None, "❌ 请输入要播报的文本内容"

    if voice_type == "自定义克隆" and ref_audio is None:
        return None, "❌ 选择「自定义克隆」音色时，必须上传参考音频"

    # 2. 调用语音生成模块 (Bert-VITS)
    try:
        audio_path = call_voice_api(text, voice_type, ref_audio)
        if not audio_path:
            return None, "❌ 语音生成失败，请检查模型服务"
    except Exception as e:
        return None, f"❌ 语音生成异常: {str(e)}"

    # 3. 调用视频生成模块 (Wav2Lip)
    try:
        video_path = call_video_api(audio_path, ref_video)
        if not video_path:
            return None, "❌ 视频生成失败，请检查模型服务"
    except Exception as e:
        return None, f"❌ 视频生成异常: {str(e)}"

    return video_path, "✅ 生成成功！"


# ============================================================
#  调用语音生成API
# ============================================================

def call_voice_api(text, voice_type, ref_audio):
    """
    调用 Bert-VITS API 生成语音
    """
    print(f"[语音生成] 文本: {text[:50]}..., 音色: {voice_type}")

    # 构建命令（路径指向 scripts/）
    script_path = "./scripts/bert_vits_api.py"
    args = ["python", script_path, text, voice_type]

    if ref_audio and voice_type == "自定义克隆":
        args.append(ref_audio)

    # 执行
    result = subprocess.run(
        args,
        capture_output=True,
        text=True,
        timeout=60
    )

    # 检查执行结果
    if result.returncode != 0:
        print(f"[语音生成] 错误: {result.stderr}")
        return None

    # 解析 JSON 输出
    try:
        data = json.loads(result.stdout)
        if data.get("status") == "success":
            return data.get("data", {}).get("file_path")
        else:
            print(f"[语音生成] 失败: {data.get('message')}")
            return None
    except json.JSONDecodeError as e:
        print(f"[语音生成] JSON解析失败: {e}")
        print(f"[语音生成] 原始输出: {result.stdout}")
        return None


# ============================================================
#  调用视频生成API
# ============================================================

def call_video_api(audio_path, ref_video):
    """
    调用 Wav2Lip API 生成视频
    """
    print(f"[视频生成] 音频: {audio_path}")

    # 构建命令（路径指向 scripts/）
    script_path = "./scripts/wav2lip_api.py"
    args = ["python", script_path, audio_path]

    if ref_video:
        args.append(ref_video)

    # 执行
    result = subprocess.run(
        args,
        capture_output=True,
        text=True,
        timeout=120
    )

    # 检查执行结果
    if result.returncode != 0:
        print(f"[视频生成] 错误: {result.stderr}")
        return None

    # 解析 JSON 输出
    try:
        data = json.loads(result.stdout)
        if data.get("status") == "success":
            return data.get("data", {}).get("file_path")
        else:
            print(f"[视频生成] 失败: {data.get('message')}")
            return None
    except json.JSONDecodeError as e:
        print(f"[视频生成] JSON解析失败: {e}")
        print(f"[视频生成] 原始输出: {result.stdout}")
        return None


# ============================================================
#  构建 Gradio WebUI 界面
# ============================================================

def build_interface():
    """构建 Gradio 界面"""
    with gr.Blocks(
        title="AI数字人视频生成系统",
        theme=gr.themes.Soft()
    ) as demo:
        # 标题
        gr.Markdown("""
        # 🎬 AI数字人视频生成系统

        输入文本，选择音色，一键生成数字人播报视频。
        支持上传参考音频进行声音克隆，上传参考视频指定人物形象。
        """)

        # 主布局：左右两列
        with gr.Row():
            # 左列：输入区域
            with gr.Column(scale=1):
                gr.Markdown("### 📝 输入设置")

                # 1. 文本输入框
                text_input = gr.Textbox(
                    label="输入文本",
                    placeholder="请输入要让数字人说的话...",
                    lines=6,
                    max_lines=10
                )

                # 2. 声音选择下拉框
                voice_select = gr.Dropdown(
                    choices=["默认男声", "默认女声", "自定义克隆"],
                    value="默认男声",
                    label="🎤 声音选择"
                )

                # 3. 参考音频上传
                ref_audio = gr.Audio(
                    label="🎵 参考音频上传（自定义克隆时使用）",
                    type="filepath",
                    sources=["upload"]
                )

                # 4. 参考视频上传
                ref_video = gr.Video(
                    label="🎥 参考视频上传（指定人物模型）",
                    sources=["upload"]
                )

                # 5. 生成按钮
                generate_btn = gr.Button(
                    "🚀 生成视频",
                    variant="primary",
                    size="lg"
                )

            # 右列：输出区域
            with gr.Column(scale=1):
                gr.Markdown("### 🎬 生成结果")

                # 6. 视频输出
                video_output = gr.Video(
                    label="生成的数字人视频",
                    autoplay=True
                )

                # 7. 状态输出
                status_output = gr.Textbox(
                    label="📊 状态",
                    interactive=False
                )

        # 使用说明
        with gr.Accordion("📖 使用说明", open=False):
            gr.Markdown("""
            **操作步骤：**
            1. 在「输入文本」框中输入想让数字人说的话
            2. 在「声音选择」下拉框中选择音色
               - 默认男声/默认女声：使用模型内置音色
               - 自定义克隆：需要上传参考音频（3-10秒的WAV文件）
            3. 可选：上传参考视频来指定人物形象
            4. 点击「生成视频」按钮，等待20-60秒
            5. 在右侧观看生成的数字人视频，可下载保存

            **提示：**
            - 建议文本长度不超过500字（约2-3分钟）
            - 参考音频格式：WAV，16kHz，单声道
            - 参考视频格式：MP4
            """)

        # ============================================================
        #  绑定事件：点击生成按钮 → 调用生成函数
        # ============================================================
        generate_btn.click(
            fn=generate_digital_human,
            inputs=[text_input, voice_select, ref_audio, ref_video],
            outputs=[video_output, status_output]
        )

    return demo


# ============================================================
#  主入口
# ============================================================

if __name__ == "__main__":
    demo = build_interface()
    demo.launch(
        server_name="0.0.0.0",   # 允许外部访问
        server_port=7860,        # 默认端口
        share=True               # 生成公网分享链接
    )