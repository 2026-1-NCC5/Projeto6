import os
from ultralytics import YOLO

# Configurações de Diretórios
PASTA_IMAGENS_CRUAS = r"C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_2\Backend\IA\Brutas"
PASTA_SAIDA_YOLO = r"C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_2\Backend\IA\dataset_yolo_auto"

# O modelo treinado que o usuário vai gerar rodando o treinar.py
# (Geralmente salvo em runs/detect/train/weights/best.pt)
CAMINHO_MODELO = r"C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_2\Backend\IA\runs\detect\train-2\weights\best.pt"

os.makedirs(PASTA_SAIDA_YOLO, exist_ok=True)

def processar_pasta(pasta_input, modelo):
    if not os.path.exists(pasta_input):
        return
        
    print(f"\nIniciando Auto-Labeling na pasta: {pasta_input}...")
    
    for root, _, files in os.walk(pasta_input):
        for file in files:
            if file.lower().endswith((".jpg", ".jpeg", ".png")):
                caminho_imagem = os.path.join(root, file)
                nome_txt = os.path.splitext(file)[0] + ".txt"
                caminho_txt = os.path.join(PASTA_SAIDA_YOLO, nome_txt)
                
                # Fazer a predição
                results = modelo(caminho_imagem, conf=0.60, verbose=False)
                
                # Salvar no formato YOLO
                with open(caminho_txt, "w") as f:
                    for result in results:
                        for box in result.boxes:
                            # Classe da predição
                            cls = int(box.cls[0].item())
                            # Coordenadas YOLO normalizadas [x_center, y_center, width, height]
                            x, y, w, h = box.xywhn[0].tolist()
                            
                            f.write(f"{cls} {x:.6f} {y:.6f} {w:.6f} {h:.6f}\n")

if __name__ == "__main__":
    if not os.path.exists(CAMINHO_MODELO):
        print(f"Erro: Modelo não encontrado em {CAMINHO_MODELO}.")
        print("Você precisa treinar a amostra de 40 imagens com o treinar.py primeiro!")
    else:
        print("Carregando o seu modelo customizado YOLOv8...")
        modelo = YOLO(CAMINHO_MODELO)
        
        # Rotular automaticamente o Dataset-Val e o teste
        processar_pasta(os.path.join(PASTA_IMAGENS_CRUAS, "Dataset-Val"), modelo)
        processar_pasta(os.path.join(PASTA_IMAGENS_CRUAS, "teste"), modelo)
        
        print(f"\nProcesso finalizado! Arquivos .txt (labels) gerados em: {PASTA_SAIDA_YOLO}")
