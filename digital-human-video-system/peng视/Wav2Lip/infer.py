"""
Wav2Lip 核心推理模块
用法：
    from Wav2Lip.infer import wav2lip_generate
    wav2lip_generate("face.mp4", "audio.wav", "output.mp4")
"""
import os, sys, subprocess, platform
import numpy as np
import cv2
import torch
from tqdm import tqdm

# 自动找到 imageio-ffmpeg 内置的 ffmpeg 二进制
_FFMPEG_PATH = "ffmpeg"
try:
    import imageio_ffmpeg
    _FFMPEG_PATH = imageio_ffmpeg.get_ffmpeg_exe()
    os.environ["PATH"] = os.path.dirname(_FFMPEG_PATH) + os.pathsep + os.environ.get("PATH", "")
except Exception:
    pass

_MODEL_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, _MODEL_DIR)
sys.path.insert(0, os.path.join(_MODEL_DIR, ".."))
import audio
import face_detection
from models import Wav2Lip

# ---------- 全局单例 ----------
_MODELS = {}
_device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
mel_step_size = 16


def _ensure_models(checkpoint_path: str = ""):
    global _MODELS
    if _MODELS:
        return _MODELS
    base = os.path.dirname(os.path.abspath(__file__))
    checkpoint_path = checkpoint_path or os.path.join(base, "checkpoints", "wav2lip_gan.pth")

    model = Wav2Lip()
    checkpoint = torch.load(checkpoint_path, map_location=_device, weights_only=False)
    s = checkpoint["state_dict"]
    new_s = {k.replace("module.", ""): v for k, v in s.items()}
    model.load_state_dict(new_s)
    model = model.to(_device).eval()

    _MODELS["model"] = model
    return _MODELS


def _get_smoothened_boxes(boxes, T):
    for i in range(len(boxes)):
        if i + T > len(boxes):
            window = boxes[len(boxes) - T:]
        else:
            window = boxes[i: i + T]
        boxes[i] = np.mean(window, axis=0)
    return boxes


def _face_detect(images, pads, face_det_batch_size, nosmooth):
    detector = face_detection.FaceAlignment(
        face_detection.LandmarksType._2D, flip_input=False, device=_device)

    batch_size = face_det_batch_size
    while True:
        predictions = []
        try:
            for i in tqdm(range(0, len(images), batch_size), desc="FaceDet"):
                predictions.extend(
                    detector.get_detections_for_batch(np.array(images[i:i + batch_size])))
        except RuntimeError:
            if batch_size == 1:
                raise
            batch_size //= 2
            print(f"[Wav2Lip] OOM, batch_size -> {batch_size}", file=sys.stderr)
            continue
        break

    results = []
    pady1, pady2, padx1, padx2 = pads
    for rect, image in zip(predictions, images):
        if rect is None:
            raise ValueError("[Wav2Lip] 未检测到人脸！")
        y1 = max(0, rect[1] - pady1)
        y2 = min(image.shape[0], rect[3] + pady2)
        x1 = max(0, rect[0] - padx1)
        x2 = min(image.shape[1], rect[2] + padx2)
        results.append([x1, y1, x2, y2])

    boxes = np.array(results)
    if not nosmooth:
        boxes = _get_smoothened_boxes(boxes, T=5)
    results = [[image[y1:y2, x1:x2], (y1, y2, x1, x2)]
               for image, (x1, y1, x2, y2) in zip(images, boxes)]
    del detector
    return results


def _datagen(frames, mels, img_size, wav2lip_batch_size, static, pads,
             face_det_batch_size, nosmooth, box):
    img_batch, mel_batch, frame_batch, coords_batch = [], [], [], []

    if box[0] == -1:
        if not static:
            face_det_results = _face_detect(frames, pads, face_det_batch_size, nosmooth)
        else:
            face_det_results = _face_detect([frames[0]], pads, face_det_batch_size, nosmooth)
    else:
        y1, y2, x1, x2 = box
        face_det_results = [[f[y1:y2, x1:x2], (y1, y2, x1, x2)] for f in frames]

    for i, m in enumerate(mels):
        idx = 0 if static else i % len(frames)
        frame_to_save = frames[idx].copy()
        face, coords = face_det_results[idx].copy()
        face = cv2.resize(face, (img_size, img_size))
        img_batch.append(face)
        mel_batch.append(m)
        frame_batch.append(frame_to_save)
        coords_batch.append(coords)

        if len(img_batch) >= wav2lip_batch_size:
            img_batch = np.asarray(img_batch)
            mel_batch = np.asarray(mel_batch)
            img_masked = img_batch.copy()
            img_masked[:, img_size // 2:] = 0
            img_batch = np.concatenate((img_masked, img_batch), axis=3) / 255.
            mel_batch = mel_batch.reshape(len(mel_batch), mel_batch.shape[1],
                                          mel_batch.shape[2], 1)
            yield img_batch, mel_batch, frame_batch, coords_batch
            img_batch, mel_batch, frame_batch, coords_batch = [], [], [], []

    if len(img_batch) > 0:
        img_batch = np.asarray(img_batch)
        mel_batch = np.asarray(mel_batch)
        img_masked = img_batch.copy()
        img_masked[:, img_size // 2:] = 0
        img_batch = np.concatenate((img_masked, img_batch), axis=3) / 255.
        mel_batch = mel_batch.reshape(len(mel_batch), mel_batch.shape[1],
                                      mel_batch.shape[2], 1)
        yield img_batch, mel_batch, frame_batch, coords_batch


def wav2lip_generate(
    face_path: str,
    audio_path: str,
    out_video_path: str,
    checkpoint_path: str = "",
    pads: tuple = (0, 10, 0, 0),
    face_det_batch_size: int = 16,
    wav2lip_batch_size: int = 128,
    resize_factor: int = 1,
    crop: tuple = (0, -1, 0, -1),
    box: tuple = (-1, -1, -1, -1),
    rotate: bool = False,
    nosmooth: bool = False,
    fps: float = 25.0,
    **kwargs,
) -> str:
    """
    使用 Wav2Lip 生成唇形同步视频。

    Args:
        face_path:          输入人脸视频/图片路径
        audio_path:         驱动音频路径
        out_video_path:     输出视频路径
        checkpoint_path:    模型权重路径
        pads:               padding (top, bottom, left, right)
        face_det_batch_size: 人脸检测 batch size
        wav2lip_batch_size:  Wav2Lip batch size
        resize_factor:      降采样系数
        crop:               裁剪区域 (top, bottom, left, right)
        box:                固定 bbox
        rotate:             是否旋转 90 度
        nosmooth:           不进行平滑
        fps:                静态图片时的帧率

    Returns:
        输出视频路径
    """
    m = _ensure_models(checkpoint_path)
    model = m["model"]
    img_size = 96
    static = False

    os.makedirs(os.path.dirname(out_video_path) or ".", exist_ok=True)

    # ---------- 1. 读取输入 ----------
    if os.path.isfile(face_path) and face_path.split(".")[-1] in ("jpg", "png", "jpeg"):
        static = True
        full_frames = [cv2.imread(face_path)]
        fps = fps
    else:
        video_stream = cv2.VideoCapture(face_path)
        fps = video_stream.get(cv2.CAP_PROP_FPS)
        full_frames = []
        while True:
            still_reading, frame = video_stream.read()
            if not still_reading:
                video_stream.release()
                break
            if resize_factor > 1:
                frame = cv2.resize(frame,
                                   (frame.shape[1] // resize_factor, frame.shape[0] // resize_factor))
            if rotate:
                frame = cv2.rotate(frame, cv2.ROTATE_90_CLOCKWISE)
            y1, y2, x1, x2 = crop
            if x2 == -1:
                x2 = frame.shape[1]
            if y2 == -1:
                y2 = frame.shape[0]
            frame = frame[y1:y2, x1:x2]
            full_frames.append(frame)

    # ---------- 2. 音频处理 ----------
    if not audio_path.endswith(".wav"):
        temp_wav = os.path.join(os.path.dirname(out_video_path), ".wav2lip_temp.wav")
        subprocess.call(f'"{_FFMPEG_PATH}" -y -v warning -i "{audio_path}" -strict -2 "{temp_wav}"',
                        shell=True)
        audio_path = temp_wav

    wav = audio.load_wav(audio_path, 16000)
    mel = audio.melspectrogram(wav)
    if np.isnan(mel.reshape(-1)).sum() > 0:
        raise ValueError("[Wav2Lip] Mel 包含 NaN！")

    mel_chunks = []
    mel_idx_multiplier = 80. / fps
    i = 0
    while True:
        start_idx = int(i * mel_idx_multiplier)
        if start_idx + mel_step_size > len(mel[0]):
            mel_chunks.append(mel[:, len(mel[0]) - mel_step_size:])
            break
        mel_chunks.append(mel[:, start_idx: start_idx + mel_step_size])
        i += 1

    full_frames = full_frames[:len(mel_chunks)]

    # ---------- 3. 推理 ----------
    gen = _datagen(full_frames.copy(), mel_chunks, img_size, wav2lip_batch_size,
                   static, pads, face_det_batch_size, nosmooth, box)

    frame_h, frame_w = full_frames[0].shape[:-1]
    temp_avi = os.path.join(os.path.dirname(out_video_path), ".wav2lip_temp.avi")
    out = cv2.VideoWriter(temp_avi, cv2.VideoWriter_fourcc(*"DIVX"), fps, (frame_w, frame_h))

    for i, (img_batch, mel_batch, frames, coords) in enumerate(tqdm(
            gen, total=int(np.ceil(float(len(mel_chunks)) / wav2lip_batch_size)),
            desc="Wav2Lip")):
        img_batch = torch.FloatTensor(np.transpose(img_batch, (0, 3, 1, 2))).to(_device)
        mel_batch = torch.FloatTensor(np.transpose(mel_batch, (0, 3, 1, 2))).to(_device)
        with torch.no_grad():
            pred = model(mel_batch, img_batch)
        pred = pred.cpu().numpy().transpose(0, 2, 3, 1) * 255.
        for p, f, c in zip(pred, frames, coords):
            y1, y2, x1, x2 = c
            p = cv2.resize(p.astype(np.uint8), (x2 - x1, y2 - y1))
            f[y1:y2, x1:x2] = p
            out.write(f)
    out.release()

    # ---------- 4. 合成最终视频 ----------
    cmd = f'"{_FFMPEG_PATH}" -y -v warning -i "{audio_path}" -i "{temp_avi}" -strict -2 -q:v 1 "{out_video_path}"'
    subprocess.call(cmd, shell=True)

    # 清理
    if os.path.exists(temp_avi):
        os.remove(temp_avi)
    if not audio_path.endswith(".wav") and os.path.exists(audio_path):
        try:
            os.remove(audio_path)
        except:
            pass

    print(f"[Wav2Lip] 完成: {out_video_path}", file=sys.stderr)
    return out_video_path


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--face", required=True)
    parser.add_argument("--audio", required=True)
    parser.add_argument("--out", default="output_wav2lip.mp4")
    args = parser.parse_args()
    wav2lip_generate(args.face, args.audio, args.out)
