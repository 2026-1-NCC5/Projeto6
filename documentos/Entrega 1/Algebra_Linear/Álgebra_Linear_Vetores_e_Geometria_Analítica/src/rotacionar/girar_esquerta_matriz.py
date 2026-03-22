import pandas as pd

df = pd.read_csv("Matriz_Original.csv", sep=";")

coluna = df.columns[1]
df.loc[1:, coluna] = df.loc[1:, coluna].iloc[::-1].values

df[["linha", "coluna"]] = df[["coluna", "linha"]]

df.to_csv("Matriz_GirarEsquerta.csv", index=False, sep=";")