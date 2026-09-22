import torch
print(f"torch {torch.__version__}", flush=True)
print(f"cuda {torch.cuda.is_available()}", flush=True)
print("done", flush=True)
