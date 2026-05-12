from ultralytics import YOLO

def main():
    # Carrega o "cérebro" base — YOLOv8n (Nano)
    # Tem apenas 3.2M de parâmetros, treina muito mais rápido e usa menos memória,
    # mantendo uma precisão excelente.
    model = YOLO('yolov8n.pt')

    print("Iniciando o Treinamento SLIM (Rápido e Eficiente)...")
    
    # Inicia o fine-tuning com os seus dados
    resultados = model.train(
        # Atualize o caminho se necessário
        data=r'C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_1\Backend\IA\dataset_yolo\dataset.yaml',
        
        # Reduzido para 40: Um excelente meio-termo. Treina rápido e já dá para ver
        # como o modelo se comporta com as novas imagens.
        epochs=40,      
        
        # Reduzido para 512: O tamanho original da geração do Stable Diffusion.
        # Processa as imagens mais rápido que a versão 640.
        imgsz=512,      
        
        # Aumentado para 16: Como o modelo Nano é mais leve, sua placa de vídeo
        # deve aguentar lotes maiores, o que acelera o tempo de cada época.
        # (Se a placa der erro de "CUDA out of memory", altere para 8).
        batch=16,        
        
        # GPU principal
        device=0,       
        
        # === DATA AUGMENTATION ESSENCIAL ===
        # Versão simplificada para não pesar tanto no processador durante o treino,
        # mas mantendo o que é essencial para o celular (distância e posição).
        degrees=10.0,       # Rotação leve (10°)
        translate=0.2,      # IMPORTANTE: Produto fora do centro do frame
        scale=0.5,          # IMPORTANTE: Produto mais perto ou mais longe
        fliplr=0.5,         # 50% de chance de espelhar horizontalmente
        mosaic=1.0,         # O Mosaico é crucial para ajudar o modelo Nano a aprender contexto
        
        # Desligamos as augmentações pesadas (shear, mixup, copy_paste, etc)
        # para economizar tempo de CPU/GPU.
        
        # Salvar em uma pasta separada para não misturar com o treinamento "pesado"
        project='runs/detect',
        name='train_slim',
        
        # Gera gráficos automáticos
        plots=True      
    )
    
    print("\nTreinamento SLIM concluído!")
    print("Os melhores pesos foram salvos na pasta: runs/detect/train_slim/weights/best.pt")
    print("IMPORTANTE: Se for testar, atualize o caminho no teste.py para apontar para o best.pt desta nova pasta!")

if __name__ == '__main__':
    main()
