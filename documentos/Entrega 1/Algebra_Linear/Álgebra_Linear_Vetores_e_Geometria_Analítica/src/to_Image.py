import pandas as pd
import numpy as np
from PIL import Image

# carregar os dados
df = pd.read_csv("matriz_transformada.csv", sep=";")

# definir fator de escala (por exemplo 100)
fator = 100
df['linha_scaled'] = (df['linha'] / fator).round().astype(int)
df['coluna_scaled'] = (df['coluna'] / fator).round().astype(int)

# descobrir tamanho da imagem
altura = df['linha_scaled'].max() + 1
largura = df['coluna_scaled'].max() + 1

# criar matriz vazia
matriz = np.zeros((altura, largura, 3), dtype=np.uint8)

# preencher a matriz
for _, row in df.iterrows():
    i = row['linha_scaled']
    j = row['coluna_scaled']
    matriz[i, j] = [int(row['R']), int(row['G']), int(row['B'])]

# criar imagem
img = Image.fromarray(matriz)
img.show()
img.save("matriz_transformada_redimensionada.png")