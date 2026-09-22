"""
后端 API 服务，为 Qt 客户端提供 AI 数字人视频生成接口。
监听地址：http://127.0.0.1:8000
"""
import json
import os
import subprocess
import sys
import threading
import uuid
from urllib.parse import quote

from flask import Flask, jsonify, request
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BERT_VITS_API = os.path.join(BASE_DIR, "scripts", "bert_vits_api.py")
WAV2LIP_API = os.path.join(BASE_DIR, "scripts", "wav2lip_api.py")
REF_VIDEO = os.path.join(BASE_DIR, "data", "reference", "ref_video.mp4")

BERT_VITS_PYTHON = r"C:\Users\hzh21\anaconda3\envs\bertvits\python.exe"
WAV2LIP_PYTHON = r"C:\Users\hzh21\anaconda3\envs\wav2lip\python.exe"

tasks = {}
lock = threading.Lock()


def _to_local_url(path: str) -> str:
    """把本地路径转成 file:/// URL，方便 Qt 客户端播放。"""
    path = os.path.abspath(path).replace("\\", "/")
    return "file:///" + quote(path, safe="/:")


def _parse_json_output(stdout: str):
    """从子进程 stdout 中提取 JSON 并解析。

    子进程（如 wav2lip_api / bert_vits_api）在输出最终 JSON 之前，
    可能往 stdout 打印了其他日志（模型加载信息、进度等），直接
    json.loads 整个 stdout 会失败。这里取最后一行以 '{' 开头的
    内容作为 JSON 解析，保证健壮性。
    """
    if not stdout or not stdout.strip():
        raise ValueError("子进程无输出，无法解析 JSON")
    lines = stdout.strip().splitlines()
    for line in reversed(lines):
        line = line.strip()
        if line.startswith("{"):
            return json.loads(line)
    raise ValueError(f"未在子进程输出中找到 JSON 行，原始输出前200字符: {stdout[:200]!r}")


@app.route("/api/ai/generate-text", methods=["POST"])
def generate_text():
    """占位接口：Qt 客户端目前未使用，保留兼容性。"""
    return jsonify({"status": "success", "data": {"text": "", "taskId": str(uuid.uuid4())}})


@app.route("/api/ai/generate-video", methods=["POST"])
def generate_video():
    data = request.get_json(force=True, silent=True) or {}
    text = data.get("text", "")
    voice_id = data.get("voiceId", "默认女声")
    voice_ref_audio = data.get("voiceRefAudio", "")
    avatar_id = data.get("avatarId", "默认形象")
    speed = float(data.get("speed", 1.0))

    if not text.strip():
        return jsonify({"status": "error", "message": "文案不能为空"}), 400

    task_id = str(uuid.uuid4())
    with lock:
        tasks[task_id] = {
            "status": "pending",
            "progress": 0,
            "videoUrl": "",
            "message": "排队中...",
        }

    def run_task():
        with lock:
            tasks[task_id]["status"] = "processing"
            tasks[task_id]["progress"] = 5
            tasks[task_id]["message"] = "正在生成音频..."

        try:
            # 1. 调用 Bert-VITS 生成音频
            print(f"[Task {task_id}] 开始生成音频: text={text[:30]} voice={voice_id} ref={voice_ref_audio}")
            audio_args = [BERT_VITS_PYTHON, BERT_VITS_API, text, voice_id, voice_ref_audio or "", str(speed)]
            audio_res = subprocess.run(
                audio_args,
                capture_output=True,
                text=True,
                encoding="utf-8",
                errors="replace",
                timeout=300,
            )
            print(f"[Task {task_id}] 音频生成返回码: {audio_res.returncode}")
            if audio_res.stdout:
                print(f"[Task {task_id}] 音频生成 stdout:\n{audio_res.stdout}")
            if audio_res.stderr:
                print(f"[Task {task_id}] 音频生成 stderr:\n{audio_res.stderr}")

            if audio_res.returncode != 0:
                raise RuntimeError(f"音频生成失败: {audio_res.stderr or audio_res.stdout}")

            audio_payload = _parse_json_output(audio_res.stdout)
            if audio_payload.get("status") != "success":
                raise RuntimeError(audio_payload.get("message", "音频生成失败"))

            audio_path = audio_payload["data"]["file_path"]
            print(f"[Task {task_id}] 音频生成成功: {audio_path}")

            with lock:
                tasks[task_id]["progress"] = 50
                tasks[task_id]["message"] = "正在合成数字人视频..."

            # 2. 调用 Wav2Lip 生成视频
            face_path = REF_VIDEO
            if avatar_id and os.path.exists(avatar_id):
                face_path = avatar_id

            print(f"[Task {task_id}] 开始生成视频: audio={audio_path} face={face_path}")
            video_res = subprocess.run(
                [WAV2LIP_PYTHON, WAV2LIP_API, audio_path, face_path],
                capture_output=True,
                text=True,
                encoding="utf-8",
                errors="replace",
                timeout=1800,
            )
            print(f"[Task {task_id}] 视频生成返回码: {video_res.returncode}")
            if video_res.stdout:
                print(f"[Task {task_id}] 视频生成 stdout:\n{video_res.stdout}")
            if video_res.stderr:
                print(f"[Task {task_id}] 视频生成 stderr:\n{video_res.stderr}")

            if video_res.returncode != 0:
                raise RuntimeError(f"视频生成失败: {video_res.stderr or video_res.stdout}")

            video_payload = _parse_json_output(video_res.stdout)
            if video_payload.get("status") != "success":
                raise RuntimeError(video_payload.get("message", "视频生成失败"))

            video_path = video_payload["data"]["file_path"]
            print(f"[Task {task_id}] 视频生成成功: {video_path}")

            with lock:
                tasks[task_id]["status"] = "completed"
                tasks[task_id]["progress"] = 100
                tasks[task_id]["videoUrl"] = _to_local_url(video_path)
                tasks[task_id]["message"] = "生成完成"

        except Exception as exc:
            print(f"[Task {task_id}] 生成失败: {exc}")
            with lock:
                tasks[task_id]["status"] = "failed"
                tasks[task_id]["message"] = f"生成失败: {exc}"

    threading.Thread(target=run_task, daemon=True).start()
    return jsonify({"status": "success", "data": {"taskId": task_id}})


@app.route("/api/ai/task/<task_id>", methods=["GET"])
def get_task(task_id):
    with lock:
        task = tasks.get(task_id)
    if not task:
        return jsonify({"status": "error", "message": "任务不存在"}), 404
    return jsonify({"status": "success", "data": task})


if __name__ == "__main__":
    print("启动后端 API 服务：http://127.0.0.1:8000")
    app.run(host="127.0.0.1", port=8000, debug=False, threaded=True)
