import pandas as pd

df = pd.read_csv("Matriz_Original.csv", sep=";")

linha = df.columns[0]

df[linha].iloc[1:] = df[linha].iloc[1:][::-1].values

df.to_csv("Matriz_Inverter_Verticalmente.csv", index=False, sep=";")