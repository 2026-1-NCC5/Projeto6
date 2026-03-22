import pandas as pd

df = pd.read_csv("Matriz_Original.csv", sep=";")

colunas = ["linha", "coluna"]

df[colunas] = df[colunas] / 2  

df.to_csv("Matriz_diminuir.csv", index=False, sep=";")