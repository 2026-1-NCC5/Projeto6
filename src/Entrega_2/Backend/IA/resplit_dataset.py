import os
import shutil
import random

DATASET = r"C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_2\Backend\IA\dataset_yolo"

IMAGES_TRAIN = os.path.join(DATASET, "images", "train")
IMAGES_VAL = os.path.join(DATASET, "images", "val")
LABELS_TRAIN = os.path.join(DATASET, "labels", "train")
LABELS_VAL = os.path.join(DATASET, "labels", "val")

# Pega todos os arquivos de imagem e label de todas as pastas
todos_arquivos = []

for pasta_img, pasta_lbl in [(IMAGES_TRAIN, LABELS_TRAIN), (IMAGES_VAL, LABELS_VAL)]:
    if os.path.exists(pasta_img):
        for f in os.listdir(pasta_img):
            if f.endswith(".jpg"):
                nome_base = f.replace(".jpg", "")
                caminho_img = os.path.join(pasta_img, f)
                caminho_lbl = os.path.join(pasta_lbl, nome_base + ".txt")
                
                if os.path.exists(caminho_lbl):
                    todos_arquivos.append((caminho_img, caminho_lbl))

print(f"Total de pares encontrados: {len(todos_arquivos)}")

# Embaralhar para garantir que todas as classes caiam em ambas as pastas
random.seed(42)
random.shuffle(todos_arquivos)

# Split 80/20
split_idx = int(len(todos_arquivos) * 0.8)
train_files = todos_arquivos[:split_idx]
val_files = todos_arquivos[split_idx:]

print(f"Novo Treino: {len(train_files)}")
print(f"Nova Validação: {len(val_files)}")

# Mover para pastas temporárias para evitar conflitos
TEMP_IMG_TRAIN = os.path.join(DATASET, "temp_img_train")
TEMP_LBL_TRAIN = os.path.join(DATASET, "temp_lbl_train")
TEMP_IMG_VAL = os.path.join(DATASET, "temp_img_val")
TEMP_LBL_VAL = os.path.join(DATASET, "temp_lbl_val")

for p in [TEMP_IMG_TRAIN, TEMP_LBL_TRAIN, TEMP_IMG_VAL, TEMP_LBL_VAL]:
    os.makedirs(p, exist_ok=True)

for img, lbl in train_files:
    shutil.move(img, os.path.join(TEMP_IMG_TRAIN, os.path.basename(img)))
    shutil.move(lbl, os.path.join(TEMP_LBL_TRAIN, os.path.basename(lbl)))

for img, lbl in val_files:
    shutil.move(img, os.path.join(TEMP_IMG_VAL, os.path.basename(img)))
    shutil.move(lbl, os.path.join(TEMP_LBL_VAL, os.path.basename(lbl)))

# Limpar antigas
for p in [IMAGES_TRAIN, IMAGES_VAL, LABELS_TRAIN, LABELS_VAL]:
    shutil.rmtree(p)

# Renomear temp para oficial
os.rename(TEMP_IMG_TRAIN, IMAGES_TRAIN)
os.rename(TEMP_LBL_TRAIN, LABELS_TRAIN)
os.rename(TEMP_IMG_VAL, IMAGES_VAL)
os.rename(TEMP_LBL_VAL, LABELS_VAL)

print("Re-split realizado com sucesso!")
