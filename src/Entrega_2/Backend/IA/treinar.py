from ultralytics import YOLO

def main():
    # Carrega o "cérebro" base — YOLOv8s (Small) tem 11.2M parâmetros,
    # 3x mais que o Nano, ideal para distinguir embalagens visualmente parecidas.
    model = YOLO('yolov8s.pt')

    print("Iniciando o Treinamento...")
    
    # Inicia o fine-tuning com os seus dados
    resultados = model.train(
        # Atualize o caminho se necessário (conforme o local onde você salvou o arquivo)
        data=r'C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_1\Backend\IA\dataset_yolo\dataset.yaml',
        
        # Aumentado para 80: Com 7 classes e o modelo maior (Small), mais épocas
        # permitem que a IA fixe melhor as diferenças sutis entre embalagens.
        epochs=80,      
        
        # Aumentado para 640: Mais resolução = mais detalhe das embalagens.
        # A câmera do celular captura em resolução alta, então treinamos maior.
        imgsz=640,      
        
        # Mantido em 8: Bom equilíbrio entre velocidade e uso de VRAM.
        # Se estourar a memória com o modelo Small, reduza para 4.
        batch=8,        
        
        # GPU principal
        device=0,       
        
        # === DATA AUGMENTATION AGRESSIVO ===
        # Essas configurações simulam condições reais da câmera do celular,
        # onde o produto pode estar em qualquer posição, ângulo e escala.
        
        degrees=15.0,       # Rotação aleatória de até 15° (produto inclinado na mesa)
        translate=0.2,      # Translação de 20% (produto fora do centro do frame!)
        scale=0.7,          # Variação de escala de 30% (produto perto ou longe)
        shear=5.0,          # Distorção de perspectiva (ângulo da câmera)
        flipud=0.1,         # 10% de chance de virar de cabeça pra baixo
        fliplr=0.5,         # 50% de chance de espelhar horizontalmente
        mosaic=1.0,         # Mosaico: mistura 4 imagens (força robustez)
        mixup=0.15,         # 15% de chance de misturar 2 imagens (regularização)
        copy_paste=0.1,     # 10% de chance de copiar objetos entre imagens
        erasing=0.3,        # Apaga 30% aleatório da imagem (simula oclusão parcial)
        
        # Gera gráficos automáticos na pasta 'runs' (ótimos para o trabalho!)
        plots=True      
    )
    
    print("Treinamento concluído!")
    print("Os melhores pesos foram salvos na pasta: runs/detect/train/weights/best.pt")
    print("IMPORTANTE: Atualize o caminho no teste.py para apontar para o novo best.pt!")

if __name__ == '__main__':
    main()