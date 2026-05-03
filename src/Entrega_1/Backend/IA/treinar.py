from ultralytics import YOLO

def main():
    # Carrega o "cérebro" base leve e rápido
    model = YOLO('yolov8n.pt')

    print("Iniciando o Treinamento...")
    
    # Inicia o fine-tuning com os seus dados
    resultados = model.train(
        data='C:/Projetos/Faculdade/AlimempatIA/meu_dataset/dataset.yaml',
        epochs=30,      # Quantas vezes ele vai ler todas as fotos
        imgsz=640,      # Resolução padrão do YOLO
        batch=8,        # Lê 8 fotos por vez (evita travar o PC)
        device='cpu'    # Força o uso do processador
    )
    
    print("Treinamento concluído!")

if __name__ == '__main__':
    main()