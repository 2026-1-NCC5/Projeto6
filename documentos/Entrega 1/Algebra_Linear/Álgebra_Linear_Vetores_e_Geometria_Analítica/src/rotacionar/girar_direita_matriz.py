import pandas as pd

df = pd.read_csv("Matriz_Original.csv", sep=";")

df[["linha", "coluna"]] = df[["coluna", "linha"]]

df.to_csv("Matriz_GirarDireita.csv", index=False, sep=";")