import cv2

#IA liberada que pode dectar utilização de imagens
from ultralytics import YOLO
#Fazer contagens
from collections import Counter

modelo = YOLO('yolov8n.pt')
#Usar 1 ou 0
camera = cv2.VideoCapture(0)

print("Iniciando a câmera...Pressione q para sair.")

item_alvo = "bottle"

while True:
    sucesso, frame = camera.read()
    if not sucesso:
        print("Erro ao acessar a câmera!")
        break
        #Vai capturar os frames
    resultados = modelo(frame, stream=True)
    itens_frame = []
    frame_anotado = frame
        #Vai identificar o item da foto
    for resultado in resultados:
        frame_anotado = resultado.plot()
        #Vai identificar os itens e dar nomes para as caixas
        classes_ids = resultado.boxes.cls.tolist()
        nomes = resultado.names
        for cls_id in classes_ids:
            itens_frame.append(nomes[int(cls_id)])
    contagem = Counter(itens_frame)
    #Vai colocar o nome acima da caixa para deixar separado
    y_pos = 40
    cv2.rectangle(frame_anotado, (10,10),(350,150),(0,0,0),-1)

    for item, quantidade in contagem.items():
        texto_contagem = f"{item}:{quantidade} unidades"
        cv2.putText(frame_anotado, texto_contagem, (20, y_pos), cv2.FONT_HERSHEY_COMPLEX, 0.8, (255,255,255), 2)
        y_pos += 30

    cv2.imshow("Contador de itens", frame_anotado)
    if(cv2.waitKey(1) & 0xFF == ord('q')):
        break
camera.release()
cv2.destroyAllWindows()