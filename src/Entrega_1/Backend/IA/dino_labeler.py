import os
import glob
import torch
from PIL import Image
from transformers import AutoProcessor, AutoModelForZeroShotObjectDetection

# Configurações do projeto
IMAGE_DIR = r"c:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_1\Backend\IA\imagens_reais"
MODEL_ID = "IDEA-Research/grounding-dino-tiny"

# Dicionário mapeando o ID da classe (YOLO) para o texto (prompt) em inglês para o DINO
# O GroundingDINO funciona muito melhor com prompts em inglês.
# Adicionamos "package" ou "bag" para ajudar o modelo a encontrar a embalagem inteira.
CLASS_PROMPTS = {
    0: "bag of rice",
    1: "bag of beans",
    2: "bag of sugar",
    3: "package of pasta",
    4: "package of cornmeal",
    5: "bottle of oil"
}

# Invertemos o dicionário para buscar o ID da classe a partir do texto detectado
PROMPT_TO_CLASS_ID = {v: k for k, v in CLASS_PROMPTS.items()}

# Parâmetros de confiança do GroundingDINO
BOX_THRESHOLD = 0.35
TEXT_THRESHOLD = 0.25

def main():
    print(f"Carregando o modelo {MODEL_ID} (pode demorar na primeira vez)...")
    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"Dispositivo utilizado: {device}")

    processor = AutoProcessor.from_pretrained(MODEL_ID)
    model = AutoModelForZeroShotObjectDetection.from_pretrained(MODEL_ID).to(device)

    # O GroundingDINO aceita uma única string com as classes separadas por ponto
    text_prompt = " . ".join(CLASS_PROMPTS.values()) + " ."

    # Busca todas as imagens na pasta (suporta jpg, jpeg e png)
    image_paths = []
    for ext in ["*.jpg", "*.jpeg", "*.png"]:
        image_paths.extend(glob.glob(os.path.join(IMAGE_DIR, ext)))
        image_paths.extend(glob.glob(os.path.join(IMAGE_DIR, ext.upper())))
    
    # Remove duplicatas por conta de case sensitivity no Windows
    image_paths = list(set(image_paths))

    if not image_paths:
        print(f"Nenhuma imagem encontrada na pasta: {IMAGE_DIR}")
        return

    print(f"Encontradas {len(image_paths)} imagens. Iniciando a anotação...")

    for img_path in image_paths:
        try:
            image = Image.open(img_path).convert("RGB")
        except Exception as e:
            print(f"Erro ao abrir {img_path}: {e}")
            continue
            
        img_width, img_height = image.size

        # Prepara a entrada para o modelo
        inputs = processor(images=image, text=text_prompt, return_tensors="pt").to(device)

        with torch.no_grad():
            outputs = model(**inputs)

        # Processa as saídas (sem passar os thresholds aqui por compatibilidade de versão)
        results = processor.post_process_grounded_object_detection(
            outputs,
            inputs.input_ids,
            target_sizes=[image.size[::-1]] # formato esperado: [height, width]
        )[0]

        # Monta as anotações no formato YOLO
        yolo_annotations = []
        
        boxes = results["boxes"]
        scores = results["scores"]
        labels = results["labels"]

        for box, score, label in zip(boxes, scores, labels):
            # Filtro manual de confiança da caixa (box_threshold)
            if score < BOX_THRESHOLD:
                continue

            # O GroundingDINO pode retornar a string não exatamente igual, então verificamos
            # qual dos nossos prompts está presente no label detectado.
            class_id = -1
            for prompt_text, c_id in PROMPT_TO_CLASS_ID.items():
                if prompt_text in label:
                    class_id = c_id
                    break
            
            if class_id == -1:
                # Se o modelo detectou algo que não mapeia para as classes, ignoramos
                continue

            # A caixa vem no formato [xmin, ymin, xmax, ymax] em pixels absolutos
            xmin, ymin, xmax, ymax = box.tolist()
            
            # Converte para [x_center, y_center, width, height] normalizado (0 a 1)
            x_center = ((xmin + xmax) / 2) / img_width
            y_center = ((ymin + ymax) / 2) / img_height
            box_w = (xmax - xmin) / img_width
            box_h = (ymax - ymin) / img_height
            
            # Limita entre 0 e 1 por segurança
            x_center = max(0.0, min(1.0, x_center))
            y_center = max(0.0, min(1.0, y_center))
            box_w = max(0.0, min(1.0, box_w))
            box_h = max(0.0, min(1.0, box_h))

            yolo_annotations.append(f"{class_id} {x_center:.6f} {y_center:.6f} {box_w:.6f} {box_h:.6f}")

        # Salva o arquivo .txt com o mesmo nome da imagem
        txt_path = os.path.splitext(img_path)[0] + ".txt"
        
        with open(txt_path, "w") as f:
            f.write("\n".join(yolo_annotations))
        
        print(f"[{img_path}] -> Geradas {len(yolo_annotations)} anotações em {txt_path}")

    print("Processo finalizado com sucesso!")

if __name__ == "__main__":
    main()
