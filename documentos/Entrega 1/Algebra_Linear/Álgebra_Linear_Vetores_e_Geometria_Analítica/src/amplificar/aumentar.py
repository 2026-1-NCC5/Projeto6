import pandas as pd

# carregar matriz
df = pd.read_csv("matriz.csv", sep=";")

# escolher as colunas de posição
colunas = ["linha", "coluna"]

# aplicar escala 2x (aumentar imagem)
df[colunas] = df[colunas] * 2  # multiplica todos os valores por 2

# salvar novo arquivo
df.to_csv("aumenta.csv", index=False, sep=";")