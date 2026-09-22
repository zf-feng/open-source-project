"""
模型权重下载脚本。
运行方式：python download_weights.py

自动下载：
  - Chinese BERT（HuggingFace 公开模型）
手动放置（找队友拿）：
  - Data/models/G_78000.pth
  - Data/models/DUR_78000.pth
"""

import os
from huggingface_hub import snapshot_download

BASE_DIR = os.path.dirname(os.path.abspath(__file__))


def download_bert():
    """下载中文 BERT 模型（HuggingFace 公开模型）"""
    target = os.path.join(BASE_DIR, "bert", "chinese-roberta-wwm-ext-large")
    if os.path.exists(os.path.join(target, "pytorch_model.bin")):
        print("[OK] Chinese BERT 已存在")
        return True

    print("[下载] Chinese BERT（~1.2GB，首次需要较长时间）...")
    os.makedirs(target, exist_ok=True)
    snapshot_download(
        repo_id="hfl/chinese-roberta-wwm-ext-large",
        local_dir=target,
        local_dir_use_symlinks=False,
    )
    print("[完成] Chinese BERT 下载完成")
    return True


def check_models():
    """检查推理必需的模型文件"""
    required = [
        ("Data/models/G_78000.pth", "语音生成主模型（593MB）"),
        ("Data/models/DUR_78000.pth", "时长预测器（7MB）"),
    ]

    missing = []
    for path, desc in required:
        full = os.path.join(BASE_DIR, path)
        if os.path.exists(full):
            size = os.path.getsize(full) / 1024 / 1024
            print(f"[OK] {path} ({size:.0f}MB)")
        else:
            missing.append((path, desc))

    if missing:
        print("\n⚠ 以下模型文件缺失，需要找队友拷贝放入对应目录：")
        for path, desc in missing:
            print(f"    {path}  ← {desc}")
        print("\n   例：将 G_78000.pth 放到 Data/models/ 目录下")
    else:
        print("\n[OK] 所有模型文件已就绪！")


if __name__ == "__main__":
    print("=" * 50)
    print("Bert-VITS2 模型权重准备工具")
    print("=" * 50)

    download_bert()
    check_models()

    print("\n全部检查完成！运行 python webui.py 启动程序。")
