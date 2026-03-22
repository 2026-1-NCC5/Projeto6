import pandas as pd

df = pd.read_csv("Matriz_Original.csv", sep=";")

coluna = df.columns[1]

df[coluna].iloc[1:] = df[coluna].iloc[1:][::-1].values

df.to_csv("Matriz_Inverter_horizontal.csv", index=False, sep=";")