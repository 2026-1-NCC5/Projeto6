import os
import random
import shutil
from pathlib import Path

# --- CONFIGURAÇÕES ---
# Pasta onde estão todas as 912 imagens e txt gerados atualmente
PASTA_ORIGEM = Path("./dataset_gerado") 

# Pasta de saída onde o YOLO vai ler os dados
PASTA_DESTINO = Path("./dataset_yolo")

CLASSES = ["Arroz", "Feijao", "Acucar", "Macarrao", "Fuba", "Oleo"]
CORES = [
    "blue and white", "red and yellow", "green and white", "orange and white",
    "brown and beige", "yellow and red", "white and blue", "red and white"
]
PROPORCAO_VAL = 0.2  # 20% das imagens para validação

def criar_pastas_yolo(base_path):
    """Cria a estrutura de pastas exigida pelo YOLO."""
    pastas = [
        base_path / "images" / "train",
        base_path / "images" / "val",
        base_path / "labels" / "train",
        base_path / "labels" / "val"
    ]
    for pasta in pastas:
        pasta.mkdir(parents=True, exist_ok=True)

def copiar_arquivo(arquivo_jpg, pasta_img_destino, pasta_lbl_destino):
    """Copia a imagem e seu respectivo txt para o destino."""
    arquivo_txt = arquivo_jpg.with_suffix(".txt")
    
    # Copia a imagem
    if arquivo_jpg.exists():
        shutil.copy(arquivo_jpg, pasta_img_destino / arquivo_jpg.name)
        
    # Copia o label (se existir)
    if arquivo_txt.exists():
        shutil.copy(arquivo_txt, pasta_lbl_destino / arquivo_txt.name)

def organizar_dataset():
    print("Iniciando a divisão do dataset...")
    criar_pastas_yolo(PASTA_DESTINO)
    
    # Define os caminhos de destino
    val_imgs = PASTA_DESTINO / "images" / "val"
    val_lbls = PASTA_DESTINO / "labels" / "val"
    train_imgs = PASTA_DESTINO / "images" / "train"
    train_lbls = PASTA_DESTINO / "labels" / "train"

    for classe in CLASSES:
        print(f"\nProcessando classe: {classe}")
        
        imagens_classe = list(PASTA_ORIGEM.glob(f"imagem_{classe}_*.jpg"))
        if not imagens_classe:
            print(f"Nenhuma imagem encontrada para {classe}. Pulando...")
            continue

        imagens_val_selecionadas = []
        
        # Seleciona as imagens de validação por cor, mantendo a proporção
        for cor in CORES:
            # Encontra todas as imagens desta classe e desta cor específica
            imagens_cor = [img for img in imagens_classe if f"_{cor}_" in img.name]
            
            # Embaralha para pegar imagens aleatórias
            random.shuffle(imagens_cor)
            
            # Pega a quantidade proporcional
            cota = int(len(imagens_cor) * PROPORCAO_VAL)
            selecionadas = imagens_cor[:cota]
            imagens_val_selecionadas.extend(selecionadas)

        # Agora faz a cópia física dos arquivos
        count_val = 0
        count_train = 0
        
        for img_jpg in imagens_classe:
            if img_jpg in imagens_val_selecionadas:
                copiar_arquivo(img_jpg, val_imgs, val_lbls)
                count_val += 1
            else:
                copiar_arquivo(img_jpg, train_imgs, train_lbls)
                count_train += 1
                
        print(f" -> {count_train} arquivos enviados para TRAIN.")
        print(f" -> {count_val} arquivos enviados para VAL.")

    print("\nDivisão concluída com sucesso! Seu dataset está pronto para o YOLO.")

if __name__ == "__main__":
    organizar_dataset()