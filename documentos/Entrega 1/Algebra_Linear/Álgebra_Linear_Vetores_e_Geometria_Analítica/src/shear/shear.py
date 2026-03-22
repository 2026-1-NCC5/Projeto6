import pandas as pd
import numpy as np
from PIL import Image

df = pd.read_csv("Matriz_Original.csv", sep=";")

kx = 0.5  
ky = 0.3 

df["linha_nova"] = df["linha"] + kx * df["coluna"]
df["coluna_nova"] = df["coluna"] + ky * df["linha"]

df["linha_nova"] = df["linha_nova"].round().astype(int)
df["coluna_nova"] = df["coluna_nova"].round().astype(int)

min_linha = df["linha_nova"].min()
min_coluna = df["coluna_nova"].min()

df["linha_nova"] -= min_linha
df["coluna_nova"] -= min_coluna

altura = df["linha_nova"].max() + 1
largura = df["coluna_nova"].max() + 1

matriz = np.zeros((altura, largura, 3), dtype=np.uint8)

for _, row in df.iterrows():
    i = row["linha_nova"]
    j = row["coluna_nova"]
    matriz[i, j] = [int(row["R"]), int(row["G"]), int(row["B"])]

df.to_csv("Matriz_Shear.csv", index=False, sep=";")