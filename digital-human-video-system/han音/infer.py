"""
版本管理、兼容推理及模型加载实现。
版本说明：
    1. 版本号与github的release版本号对应，使用哪个release版本训练的模型即对应其版本号
    2. 请在模型的config.json中显示声明版本号，添加一个字段"version" : "你的版本号"
特殊版本说明：
    1.1.1-fix： 1.1.1版本训练的模型，但是在推理时使用dev的日语修复
    2.3：当前版本
"""

import torch
import commons
from text import cleaned_text_to_sequence, get_bert

# from clap_wrapper import get_clap_audio_feature, get_clap_text_feature
from typing import Union
from text.cleaner import clean_text
import utils

from models import SynthesizerTrn
from text.symbols import symbols


# V101 旧版日语音素（38个），用于 checkpoint embedding 重映射
_V101_OLD_JA_SYMBOLS = [
    "I", "N", "U", "V", "a", "b", "by", "ch", "cl", "d", "dy", "e", "f",
    "g", "gy", "h", "hy", "i", "j", "k", "ky", "m", "my", "n", "ny", "o",
    "p", "py", "r", "ry", "s", "sh", "t", "ts", "u", "w", "y", "z",
]

_V101_PUNCT = ["!", "?", "\u2026", ",", ".", "'", "-", "SP", "UNK"]


def _build_v101_symbols():
    """构建 V101 版本的完整音素列表（108个）。"""
    from text.symbols import zh_symbols, en_symbols
    old_ja_set = set(_V101_OLD_JA_SYMBOLS)
    old_normal = sorted(set(zh_symbols) | old_ja_set | set(en_symbols))
    return ["_"] + old_normal + _V101_PUNCT


def _build_current_symbols():
    """构建当前版本完整音素列表（112个）。"""
    from text.symbols import zh_symbols, ja_symbols, en_symbols
    normal = sorted(set(zh_symbols) | set(ja_symbols) | set(en_symbols))
    return ["_"] + normal + _V101_PUNCT


def _remap_v101_embedding(net_g, model_path, device):
    """将 V101 训练（108 音素）的 embedding 权重重映射到当前（112 音素）位置。"""
    try:
        ckpt = torch.load(model_path, map_location=device, weights_only=False)
        if "model" not in ckpt:
            return
        ckpt_state = ckpt["model"]
        emb_key = "enc_p.emb.weight"
        if emb_key not in ckpt_state:
            return

        old_weight = ckpt_state[emb_key]
        new_weight = net_g.enc_p.emb.weight

        # 检查是否需要重映射
        if old_weight.shape[0] == new_weight.shape[0]:
            return  # 形状一致，无需重映射

        old_sym = _build_v101_symbols()
        new_sym = _build_current_symbols()

        utils.logger.info(
            f"Remapping embedding from V101 ({len(old_sym)} symbols) "
            f"to current ({len(new_sym)} symbols)"
        )

        with torch.no_grad():
            # 为每个旧音素找到新位置，拷贝权重
            # 对于不在旧音素列表中的新音素，保留随机初始化
            weight = net_g.enc_p.emb.weight
            weight.zero_()  # 清零，确保未映射的位置为 0（而非随机）
            mapped = 0
            for old_idx, sym in enumerate(old_sym):
                sym_stripped = sym.strip()
                if sym_stripped in new_sym:
                    new_idx = new_sym.index(sym_stripped)
                    if new_idx < weight.shape[0] and old_idx < old_weight.shape[0]:
                        weight[new_idx] = old_weight[old_idx]
                        mapped += 1

            utils.logger.info(
                f"Embedding remap complete: {mapped}/{len(old_sym)} symbols mapped"
            )

    except Exception as e:
        utils.logger.warning(f"Embedding remap failed (non-critical): {e}")

# from oldVersion.V220.models import SynthesizerTrn as V220SynthesizerTrn
# from oldVersion.V220.text import symbols as V220symbols
# from oldVersion.V210.models import SynthesizerTrn as V210SynthesizerTrn
# from oldVersion.V210.text import symbols as V210symbols
# from oldVersion.V200.models import SynthesizerTrn as V200SynthesizerTrn
# from oldVersion.V200.text import symbols as V200symbols
# from oldVersion.V111.models import SynthesizerTrn as V111SynthesizerTrn
# from oldVersion.V111.text import symbols as V111symbols
# from oldVersion.V110.models import SynthesizerTrn as V110SynthesizerTrn
# from oldVersion.V110.text import symbols as V110symbols
# from oldVersion.V101.models import SynthesizerTrn as V101SynthesizerTrn
# from oldVersion.V101.text import symbols as V101symbols

# from oldVersion import V111, V110, V101, V200, V210, V220

# 当前版本信息
latest_version = "2.3"

# 版本兼容
SynthesizerTrnMap = {
    "2.3": SynthesizerTrn,
}

# symbolsMap = {
#     "2.2": V220symbols,
#     "2.1": V210symbols,
#     "2.0.2-fix": V200symbols,
#     "2.0.1": V200symbols,
#     "2.0": V200symbols,
#     "1.1.1-fix": V111symbols,
#     "1.1.1": V111symbols,
#     "1.1": V110symbols,
#     "1.1.0": V110symbols,
#     "1.0.1": V101symbols,
#     "1.0": V101symbols,
#     "1.0.0": V101symbols,
# }


# def get_emo_(reference_audio, emotion, sid):
#     emo = (
#         torch.from_numpy(get_emo(reference_audio))
#         if reference_audio and emotion == -1
#         else torch.FloatTensor(
#             np.load(f"emo_clustering/{sid}/cluster_center_{emotion}.npy")
#         )
#     )
#     return emo


def get_net_g(model_path: str, version: str, device: str, hps):
    if version != latest_version:
        net_g = SynthesizerTrnMap[version](
            len(symbolsMap[version]),
            hps.data.filter_length // 2 + 1,
            hps.train.segment_size // hps.data.hop_length,
            n_speakers=hps.data.n_speakers,
            **hps.model,
        ).to(device)
    else:
        # 当前版本模型 net_g
        net_g = SynthesizerTrn(
            len(symbols),
            hps.data.filter_length // 2 + 1,
            hps.train.segment_size // hps.data.hop_length,
            n_speakers=hps.data.n_speakers,
            **hps.model,
        ).to(device)
    _ = net_g.eval()
    _ = utils.load_checkpoint(model_path, net_g, None, skip_optimizer=True)

    # 检查是否需要重映射 embedding（兼容 V101 旧版 checkpoint，108 音素→112 音素）
    _remap_v101_embedding(net_g, model_path, device)

    return net_g


def get_text(text, language_str, hps, device, style_text=None, style_weight=0.7):
    style_text = None if style_text == "" else style_text
    # 在此处实现当前版本的get_text
    norm_text, phone, tone, word2ph = clean_text(text, language_str)
    phone, tone, language = cleaned_text_to_sequence(phone, tone, language_str)

    if hps.data.add_blank:
        phone = commons.intersperse(phone, 0)
        tone = commons.intersperse(tone, 0)
        language = commons.intersperse(language, 0)
        for i in range(len(word2ph)):
            word2ph[i] = word2ph[i] * 2
        word2ph[0] += 1
    bert_ori = get_bert(
        norm_text, word2ph, language_str, device, style_text, style_weight
    )
    del word2ph
    assert bert_ori.shape[-1] == len(phone), phone

    if language_str == "ZH":
        bert = bert_ori
        ja_bert = torch.zeros(1024, len(phone))
        en_bert = torch.zeros(1024, len(phone))
    else:
        raise ValueError("仅支持中文 ZH")

    assert bert.shape[-1] == len(
        phone
    ), f"Bert seq len {bert.shape[-1]} != {len(phone)}"

    phone = torch.LongTensor(phone)
    tone = torch.LongTensor(tone)
    language = torch.LongTensor(language)
    return bert, ja_bert, en_bert, phone, tone, language


def infer(
    text,
    emotion: Union[int, str],
    sdp_ratio,
    noise_scale,
    noise_scale_w,
    length_scale,
    sid,
    language,
    hps,
    net_g,
    device,
    reference_audio=None,
    skip_start=False,
    skip_end=False,
    style_text=None,
    style_weight=0.7,
):
# 2.2版本参数位置变了
# inferMap_V4 = {
#     "2.2": V220.infer,
# }
# 2.1 参数新增 emotion reference_audio skip_start skip_end
# inferMap_V3 = {
#     "2.1": V210.infer,
# }
# 支持中日英三语版本
# inferMap_V2 = {
#     "2.0.2-fix": V200.infer,
#     "2.0.1": V200.infer,
#     "2.0": V200.infer,
#     "1.1.1-fix": V111.infer_fix,
#     "1.1.1": V111.infer,
#     "1.1": V110.infer,
#     "1.1.0": V110.infer,
# }
# 仅支持中文版本
# 在测试中，并未发现两个版本的模型不能互相通用
# inferMap_V1 = {
#     "1.0.1": V101.infer,
#     "1.0": V101.infer,
#     "1.0.0": V101.infer,
# }
    version = hps.version if hasattr(hps, "version") else latest_version
    # 非当前版本，根据版本号选择合适的infer
    if version != latest_version:
        if version in inferMap_V4.keys():
            return inferMap_V4[version](
                text,
                emotion,
                sdp_ratio,
                noise_scale,
                noise_scale_w,
                length_scale,
                sid,
                language,
                hps,
                net_g,
                device,
                reference_audio,
                skip_start,
                skip_end,
                style_text,
                style_weight,
            )
        if version in inferMap_V3.keys():
            return inferMap_V3[version](
                text,
                sdp_ratio,
                noise_scale,
                noise_scale_w,
                length_scale,
                sid,
                language,
                hps,
                net_g,
                device,
                reference_audio,
                emotion,
                skip_start,
                skip_end,
                style_text,
                style_weight,
            )
        if version in inferMap_V2.keys():
            return inferMap_V2[version](
                text,
                sdp_ratio,
                noise_scale,
                noise_scale_w,
                length_scale,
                sid,
                language,
                hps,
                net_g,
                device,
            )
        if version in inferMap_V1.keys():
            return inferMap_V1[version](
                text,
                sdp_ratio,
                noise_scale,
                noise_scale_w,
                length_scale,
                sid,
                hps,
                net_g,
                device,
            )
    # 在此处实现当前版本的推理
    # emo = get_emo_(reference_audio, emotion, sid)
    # if isinstance(reference_audio, np.ndarray):
    #     emo = get_clap_audio_feature(reference_audio, device)
    # else:
    #     emo = get_clap_text_feature(emotion, device)
    # emo = torch.squeeze(emo, dim=1)

    bert, ja_bert, en_bert, phones, tones, lang_ids = get_text(
        text,
        language,
        hps,
        device,
        style_text=style_text,
        style_weight=style_weight,
    )
    if skip_start:
        phones = phones[3:]
        tones = tones[3:]
        lang_ids = lang_ids[3:]
        bert = bert[:, 3:]
        ja_bert = ja_bert[:, 3:]
        en_bert = en_bert[:, 3:]
    if skip_end:
        phones = phones[:-2]
        tones = tones[:-2]
        lang_ids = lang_ids[:-2]
        bert = bert[:, :-2]
        ja_bert = ja_bert[:, :-2]
        en_bert = en_bert[:, :-2]
    with torch.no_grad():
        x_tst = phones.to(device).unsqueeze(0)
        tones = tones.to(device).unsqueeze(0)
        lang_ids = lang_ids.to(device).unsqueeze(0)
        bert = bert.to(device).unsqueeze(0)
        ja_bert = ja_bert.to(device).unsqueeze(0)
        en_bert = en_bert.to(device).unsqueeze(0)
        x_tst_lengths = torch.LongTensor([phones.size(0)]).to(device)
        # emo = emo.to(device).unsqueeze(0)
        del phones
        speakers = torch.LongTensor([hps.data.spk2id[sid]]).to(device)
        audio = (
            net_g.infer(
                x_tst,
                x_tst_lengths,
                speakers,
                tones,
                lang_ids,
                bert,
                ja_bert,
                en_bert,
                sdp_ratio=sdp_ratio,
                noise_scale=noise_scale,
                noise_scale_w=noise_scale_w,
                length_scale=length_scale,
            )[0][0, 0]
            .data.cpu()
            .float()
            .numpy()
        )
        del (
            x_tst,
            tones,
            lang_ids,
            bert,
            x_tst_lengths,
            speakers,
            ja_bert,
            en_bert,
        )  # , emo
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
        return audio


def infer_multilang(
    text,
    sdp_ratio,
    noise_scale,
    noise_scale_w,
    length_scale,
    sid,
    language,
    hps,
    net_g,
    device,
    reference_audio=None,
    emotion=None,
    skip_start=False,
    skip_end=False,
):
    bert, ja_bert, en_bert, phones, tones, lang_ids = [], [], [], [], [], []
    # emo = get_emo_(reference_audio, emotion, sid)
    # if isinstance(reference_audio, np.ndarray):
    #     emo = get_clap_audio_feature(reference_audio, device)
    # else:
    #     emo = get_clap_text_feature(emotion, device)
    # emo = torch.squeeze(emo, dim=1)
    for idx, (txt, lang) in enumerate(zip(text, language)):
        _skip_start = (idx != 0) or (skip_start and idx == 0)
        _skip_end = (idx != len(language) - 1) or skip_end
        (
            temp_bert,
            temp_ja_bert,
            temp_en_bert,
            temp_phones,
            temp_tones,
            temp_lang_ids,
        ) = get_text(txt, lang, hps, device)
        if _skip_start:
            temp_bert = temp_bert[:, 3:]
            temp_ja_bert = temp_ja_bert[:, 3:]
            temp_en_bert = temp_en_bert[:, 3:]
            temp_phones = temp_phones[3:]
            temp_tones = temp_tones[3:]
            temp_lang_ids = temp_lang_ids[3:]
        if _skip_end:
            temp_bert = temp_bert[:, :-2]
            temp_ja_bert = temp_ja_bert[:, :-2]
            temp_en_bert = temp_en_bert[:, :-2]
            temp_phones = temp_phones[:-2]
            temp_tones = temp_tones[:-2]
            temp_lang_ids = temp_lang_ids[:-2]
        bert.append(temp_bert)
        ja_bert.append(temp_ja_bert)
        en_bert.append(temp_en_bert)
        phones.append(temp_phones)
        tones.append(temp_tones)
        lang_ids.append(temp_lang_ids)
    bert = torch.concatenate(bert, dim=1)
    ja_bert = torch.concatenate(ja_bert, dim=1)
    en_bert = torch.concatenate(en_bert, dim=1)
    phones = torch.concatenate(phones, dim=0)
    tones = torch.concatenate(tones, dim=0)
    lang_ids = torch.concatenate(lang_ids, dim=0)
    with torch.no_grad():
        x_tst = phones.to(device).unsqueeze(0)
        tones = tones.to(device).unsqueeze(0)
        lang_ids = lang_ids.to(device).unsqueeze(0)
        bert = bert.to(device).unsqueeze(0)
        ja_bert = ja_bert.to(device).unsqueeze(0)
        en_bert = en_bert.to(device).unsqueeze(0)
        # emo = emo.to(device).unsqueeze(0)
        x_tst_lengths = torch.LongTensor([phones.size(0)]).to(device)
        del phones
        speakers = torch.LongTensor([hps.data.spk2id[sid]]).to(device)
        audio = (
            net_g.infer(
                x_tst,
                x_tst_lengths,
                speakers,
                tones,
                lang_ids,
                bert,
                ja_bert,
                en_bert,
                sdp_ratio=sdp_ratio,
                noise_scale=noise_scale,
                noise_scale_w=noise_scale_w,
                length_scale=length_scale,
            )[0][0, 0]
            .data.cpu()
            .float()
            .numpy()
        )
        del (
            x_tst,
            tones,
            lang_ids,
            bert,
            x_tst_lengths,
            speakers,
            ja_bert,
            en_bert,
        )  # , emo
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
        return audio
