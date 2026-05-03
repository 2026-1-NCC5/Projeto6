import cv2
from ultralytics import YOLO

def main():
    # 1. Carregando a SUA Inteligência Artificial!
    # Passamos o caminho exato do seu arquivo best.pt
    caminho_modelo = r'C:\Projetos\Faculdade\AlimempatIA\runs\detect\train5\weights\best.pt'
    print("Carregando o modelo personalizado...")
    model = YOLO(caminho_modelo)

    # 2. Conectando na câmera do celular (DroidCam)
    print("Conectando à câmera...")
    cap = cv2.VideoCapture('http://192.168.15.9:4747/video')
    
    # Tenta forçar o OpenCV a não guardar fila de imagens antigas (evita o delay)
    cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)

    if not cap.isOpened():
        print("Erro: Não foi possível acessar a câmera.")
        return

    print("Tudo pronto! Aponte a câmera para o objeto. Pressione 'q' para sair.")

    contador_frames = 0
    frame_anotado = None

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        contador_frames += 1

        # Analisa 1 a cada 3 frames para o vídeo não travar
        if contador_frames % 3 == 0:
            # conf=0.5 significa que a IA só vai desenhar o quadrado se tiver mais de 50% de certeza
            resultados = model(frame, stream=True, verbose=False, conf=0.015)
            
            for resultado in resultados:
                frame_anotado = resultado.plot()
        
        if frame_anotado is None:
            frame_anotado = frame

        # Mostra o resultado na tela
        cv2.imshow('AlimempatIA', frame_anotado)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()