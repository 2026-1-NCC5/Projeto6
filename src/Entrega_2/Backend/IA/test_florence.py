import os
import torch
import unittest.mock
from transformers.dynamic_module_utils import get_imports

def fixed_get_imports(filename: str | os.PathLike) -> list[str]:
    if not str(filename).endswith("modeling_florence2.py"):
        return get_imports(filename)
    imports = get_imports(filename)
    if "flash_attn" in imports:
        imports.remove("flash_attn")
    return imports

with unittest.mock.patch("transformers.dynamic_module_utils.get_imports", fixed_get_imports):
    from transformers import AutoProcessor, AutoModelForCausalLM

    print(f"CUDA: {torch.cuda.is_available()}")
    try:
        processor = AutoProcessor.from_pretrained("microsoft/Florence-2-base", trust_remote_code=True)
        model = AutoModelForCausalLM.from_pretrained("microsoft/Florence-2-base", trust_remote_code=True)
        print("Florence-2 loaded successfully without flash_attn!")
    except Exception as e:
        print(f"Erro: {e}")
