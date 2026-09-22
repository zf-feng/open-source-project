import numpy as np
import torch
import torchaudio
import torchaudio.transforms as T
import soundfile as sf
from scipy import signal
from scipy.io import wavfile
from hparams import hparams as hp

def load_wav(path, sr):
    """读取音频并 resample 到目标采样率。"""
    wav, orig_sr = sf.read(path, dtype="float32")
    if wav.ndim > 1:
        wav = wav.mean(axis=1)
    if orig_sr != sr:
        wav = torchaudio.functional.resample(
            torch.from_numpy(wav), orig_freq=orig_sr, new_freq=sr
        ).numpy()
    return wav

def save_wav(wav, path, sr):
    wav *= 32767 / max(0.01, np.max(np.abs(wav)))
    wavfile.write(path, sr, wav.astype(np.int16))

def save_wavenet_wav(wav, path, sr):
    sf.write(path, wav, sr)

def preemphasis(wav, k, preemphasize=True):
    if preemphasize:
        return signal.lfilter([1, -k], [1], wav)
    return wav

def inv_preemphasis(wav, k, inv_preemphasize=True):
    if inv_preemphasize:
        return signal.lfilter([1], [1, -k], wav)
    return wav

def melspectrogram(wav):
    """使用 torchaudio 计算 mel spectrogram，替代 librosa。"""
    wav = preemphasis(wav, hp.preemphasis, hp.preemphasize)
    wav_tensor = torch.from_numpy(wav).float()

    mel_transform = T.MelSpectrogram(
        sample_rate=hp.sample_rate,
        n_fft=hp.n_fft,
        win_length=hp.win_size,
        hop_length=hp.hop_size,
        n_mels=hp.num_mels,
        f_min=hp.fmin,
        f_max=hp.fmax,
        power=1.0,
        normalized=False,
    )

    mel = mel_transform(wav_tensor)
    mel = mel.numpy()

    # 转 dB
    mel = 20 * np.log10(np.maximum(1e-5, mel)) - hp.ref_level_db

    if hp.signal_normalization:
        mel = _normalize(mel)

    return mel

##########################################################
#Those are only correct when using lws!!! (This was messing with Wavenet quality for a long time!)
def num_frames(length, fsize, fshift):
    """Compute number of time frames of spectrogram
    """
    pad = (fsize - fshift)
    if length % fshift == 0:
        M = (length + pad * 2 - fsize) // fshift + 1
    else:
        M = (length + pad * 2 - fsize) // fshift + 2
    return M


def pad_lr(x, fsize, fshift):
    """Compute left and right padding
    """
    M = num_frames(len(x), fsize, fshift)
    pad = (fsize - fshift)
    T = len(x) + 2 * pad
    r = (M - 1) * fshift + fsize - T
    return pad, pad + r
##########################################################
#Librosa correct padding
def librosa_pad_lr(x, fsize, fshift):
    return 0, (x.shape[0] // fshift + 1) * fshift - x.shape[0]

# Conversions
_mel_basis = None

def _linear_to_mel(spectogram):
    global _mel_basis
    if _mel_basis is None:
        _mel_basis = _build_mel_basis()
    return np.dot(_mel_basis, spectogram)

def _build_mel_basis():
    # 保留旧函数但不再使用，避免其他代码引用时报错
    return np.zeros((hp.num_mels, hp.n_fft // 2 + 1))

def _amp_to_db(x):
    min_level = np.exp(hp.min_level_db / 20 * np.log(10))
    return 20 * np.log10(np.maximum(min_level, x))

def _db_to_amp(x):
    return np.power(10.0, (x) * 0.05)

def _normalize(S):
    if hp.allow_clipping_in_normalization:
        if hp.symmetric_mels:
            return np.clip((2 * hp.max_abs_value) * ((S - hp.min_level_db) / (-hp.min_level_db)) - hp.max_abs_value,
                           -hp.max_abs_value, hp.max_abs_value)
        else:
            return np.clip(hp.max_abs_value * ((S - hp.min_level_db) / (-hp.min_level_db)), 0, hp.max_abs_value)

    assert S.max() <= 0 and S.min() - hp.min_level_db >= 0
    if hp.symmetric_mels:
        return (2 * hp.max_abs_value) * ((S - hp.min_level_db) / (-hp.min_level_db)) - hp.max_abs_value
    else:
        return hp.max_abs_value * ((S - hp.min_level_db) / (-hp.min_level_db))

def _denormalize(D):
    if hp.allow_clipping_in_normalization:
        if hp.symmetric_mels:
            return (((np.clip(D, -hp.max_abs_value,
                              hp.max_abs_value) + hp.max_abs_value) * -hp.min_level_db / (2 * hp.max_abs_value))
                    + hp.min_level_db)
        else:
            return ((np.clip(D, 0, hp.max_abs_value) * -hp.min_level_db / hp.max_abs_value) + hp.min_level_db)

    if hp.symmetric_mels:
        return (((D + hp.max_abs_value) * -hp.min_level_db / (2 * hp.max_abs_value)) + hp.min_level_db)
    return ((D * -hp.min_level_db / hp.max_abs_value) + hp.min_level_db)
