import os
import torch
import transformers.dynamic_module_utils

# Monkey patch para ignorar a validação de dependências remotas (que cobra o flash_attn)
transformers.dynamic_module_utils.check_imports = lambda filename: []

from transformers import AutoProcessor, AutoModelForCausalLM
from PIL import Image

PASTA_IMAGENS_CRUAS = r"C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_2\Backend\IA\Brutas" 
PASTA_DATASET_VAL = os.path.join(PASTA_IMAGENS_CRUAS, "Dataset-Val")
PASTA_TESTE = os.path.join(PASTA_IMAGENS_CRUAS, "teste")
PASTA_SAIDA_YOLO = r"C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_2\Backend\IA\dataset_yolo_florence"
os.makedirs(PASTA_SAIDA_YOLO, exist_ok=True)

# Mapeamento Ontológico: Texto descritivo para Classe e ID
ONTOLOGIA = {
    "rice packaging": {"id": 0, "class": "Arroz"},
    "beans packaging": {"id": 1, "class": "Feijao"},
    "sugar packaging": {"id": 2, "class": "Acucar"},
    "pasta packaging": {"id": 3, "class": "Macarrao"},
    "cornmeal flour packaging": {"id": 4, "class": "Fuba"},
    "cooking oil bottle": {"id": 5, "class": "Oleo"}
}

print("Carregando Florence-2 da Microsoft (Zero-Shot Object Detection)...")
device = "cuda" if torch.cuda.is_available() else "cpu"
model_id = "microsoft/Florence-2-base"

# Florence-2 aceita detecção baseada em texto através da task <CAPTION_TO_PHRASE_GROUNDING>
processor = AutoProcessor.from_pretrained(model_id, trust_remote_code=True)
model = AutoModelForCausalLM.from_pretrained(model_id, trust_remote_code=True).to(device)

def processar_imagem(caminho_imagem, pasta_saida):
    image = Image.open(caminho_imagem).convert("RGB")
    largura, altura = image.size
    
    anotacoes_yolo = []
    
    # Florence-2 Caption to Phrase Grounding
    for descricao, info in ONTOLOGIA.items():
        prompt = f"<CAPTION_TO_PHRASE_GROUNDING> {descricao}"
        inputs = processor(text=prompt, images=image, return_tensors="pt").to(device)
        
        with torch.no_grad():
            generated_ids = model.generate(
                input_ids=inputs["input_ids"],
                pixel_values=inputs["pixel_values"],
                max_new_tokens=1024,
                num_beams=3
            )
        
        generated_text = processor.batch_decode(generated_ids, skip_special_tokens=False)[0]
        parsed_answer = processor.post_process_generation(generated_text, task="<CAPTION_TO_PHRASE_GROUNDING>", image_size=(largura, altura))
        
        bboxes = parsed_answer.get("<CAPTION_TO_PHRASE_GROUNDING>", {}).get("bboxes", [])
        
        # Florence-2 retorna [x1, y1, x2, y2]
        for bbox in bboxes:
            x1, y1, x2, y2 = bbox
            # Convertendo para YOLO format: x_center, y_center, width, height (normalizados)
            x_center = ((x1 + x2) / 2) / largura
            y_center = ((y1 + y2) / 2) / altura
            box_width = (x2 - x1) / largura
            box_height = (y2 - y1) / altura
            
            anotacoes_yolo.append(f"{info['id']} {x_center:.6f} {y_center:.6f} {box_width:.6f} {box_height:.6f}")
    
    # Salvar arquivo txt se encontrou algo (ou arquivo vazio se preferir)
    nome_arquivo = os.path.basename(caminho_imagem)
    nome_txt = os.path.splitext(nome_arquivo)[0] + ".txt"
    caminho_txt = os.path.join(pasta_saida, nome_txt)
    
    with open(caminho_txt, "w") as f:
        f.write("\n".join(anotacoes_yolo))

def processar_pasta(pasta_input):
    if not os.path.exists(pasta_input):
        return
    print(f"\nProcessando imagens em: {pasta_input}")
    for root, _, files in os.walk(pasta_input):
        for file in files:
            if file.lower().endswith((".jpg", ".jpeg", ".png")):
                caminho = os.path.join(root, file)
                processar_imagem(caminho, PASTA_SAIDA_YOLO)

if __name__ == "__main__":
    processar_pasta(PASTA_DATASET_VAL)
    processar_pasta(PASTA_TESTE)
    print(f"\nProcesso concluído! Arquivos gerados em: {PASTA_SAIDA_YOLO}")
