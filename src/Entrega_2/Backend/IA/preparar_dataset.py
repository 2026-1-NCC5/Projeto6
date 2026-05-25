import os
import shutil
import random

PASTA_ORIGEM = r"C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_2\Backend\IA\Brutas\Anotadas_Manual"
PASTA_DATASET = r"C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_2\Backend\IA\dataset_yolo"

IMAGES_TRAIN = os.path.join(PASTA_DATASET, "images", "train")
IMAGES_VAL = os.path.join(PASTA_DATASET, "images", "val")
LABELS_TRAIN = os.path.join(PASTA_DATASET, "labels", "train")
LABELS_VAL = os.path.join(PASTA_DATASET, "labels", "val")

# Limpa o dataset antigo e cria as pastas
for p in [IMAGES_TRAIN, IMAGES_VAL, LABELS_TRAIN, LABELS_VAL]:
    if os.path.exists(p):
        shutil.rmtree(p)
    os.makedirs(p, exist_ok=True)

# Listar todas as imagens que têm um txt correspondente
arquivos_jpg = [f for f in os.listdir(PASTA_ORIGEM) if f.endswith(".jpg")]
pares_validos = []

for jpg in arquivos_jpg:
    txt = jpg.replace(".jpg", ".txt")
    if os.path.exists(os.path.join(PASTA_ORIGEM, txt)):
        pares_validos.append((jpg, txt))

# Embaralhar para evitar viés (split 80/20)
random.seed(42)
random.shuffle(pares_validos)

split_idx = int(len(pares_validos) * 0.8)
train_files = pares_validos[:split_idx]
val_files = pares_validos[split_idx:]

def copiar_arquivos(lista_arquivos, pasta_images, pasta_labels):
    for jpg, txt in lista_arquivos:
        shutil.copy(os.path.join(PASTA_ORIGEM, jpg), os.path.join(pasta_images, jpg))
        shutil.copy(os.path.join(PASTA_ORIGEM, txt), os.path.join(pasta_labels, txt))

copiar_arquivos(train_files, IMAGES_TRAIN, LABELS_TRAIN)
copiar_arquivos(val_files, IMAGES_VAL, LABELS_VAL)

print(f"Dataset criado com sucesso!")
print(f"Treino: {len(train_files)} imagens")
print(f"Validação: {len(val_files)} imagens")
