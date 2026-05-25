import os
import shutil

DATASET_ANTIGO = r"C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_2\Backend\IA\dataset"
DATASET_NOVO = r"C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_2\Backend\IA\dataset_yolo"

def copiar_pasta(origem, destino):
    if not os.path.exists(origem):
        print(f"Aviso: Pasta origem não existe: {origem}")
        return
        
    os.makedirs(destino, exist_ok=True)
    
    arquivos = os.listdir(origem)
    count = 0
    for arquivo in arquivos:
        src = os.path.join(origem, arquivo)
        dst = os.path.join(destino, arquivo)
        
        if os.path.isfile(src):
            # Copiar apenas se não existir para evitar problemas, embora nomes sejam diferentes
            if not os.path.exists(dst):
                shutil.copy2(src, dst)
                count += 1
                
    print(f"Copiados {count} arquivos de {origem} para {destino}")

print("Iniciando mesclagem...")

# Imagens
copiar_pasta(os.path.join(DATASET_ANTIGO, "images", "train"), os.path.join(DATASET_NOVO, "images", "train"))
copiar_pasta(os.path.join(DATASET_ANTIGO, "images", "val"), os.path.join(DATASET_NOVO, "images", "val"))

# Labels
copiar_pasta(os.path.join(DATASET_ANTIGO, "labels", "train"), os.path.join(DATASET_NOVO, "labels", "train"))
copiar_pasta(os.path.join(DATASET_ANTIGO, "labels", "val"), os.path.join(DATASET_NOVO, "labels", "val"))

print("Mesclagem concluída com sucesso!")

# Contagem final
print("\nContagem Final no Dataset YOLO (Pronto para treinar):")
for tipo in ["train", "val"]:
    img_dir = os.path.join(DATASET_NOVO, "images", tipo)
    if os.path.exists(img_dir):
        print(f"Images {tipo}: {len(os.listdir(img_dir))}")
