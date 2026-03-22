from PIL import Image
import numpy as np
import pandas as pd

img = Image.open("imagem_shear.png")
matriz = np.array(img)

# transformar em tabela
altura, largura, _ = matriz.shape

dados = []

for i in range(altura):
    for j in range(largura):
        r, g, b = matriz[i][j]
        dados.append([i, j, r, g, b])

df = pd.DataFrame(dados, columns=["linha", "coluna", "R", "G", "B"])

df.to_csv("Imagem_Shear.csv", index=False, sep=";")