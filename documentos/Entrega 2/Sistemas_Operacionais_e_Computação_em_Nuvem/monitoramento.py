import psutil
import pandas as pd
import time
import matplotlib.pyplot as plt
import numpy as np

from sklearn.linear_model import LinearRegression
from sklearn.neighbors import KNeighborsClassifier
from sklearn.cluster import KMeans
# -------------------------
# PASSO 2 - Coleta de dados
# -------------------------
dados =[]

print("Coletando dados...")

for i in range(20):
    cpu = psutil.cpu_percent()
    memoria = psutil.virtual_memory().percent

    dados.append([cpu, memoria])

    time.sleep(1)

df = pd.DataFrame(dados, columns=["cpu", "memoria"])

print(df)

# -------------------------
# PASSO 3 - IA
# -------------------------
#  Regressão Linear

x = np.array(range(len(df))).reshape(-1, 1)
y = df["cpu"]

modelo = LinearRegression()
modelo.fit(x,y)

previsao = modelo.predict(x)

#  Classificação (KNN)

df["status"] = df ["cpu"].apply(lambda x:1 if x > 50 else 0)

x_class = df[["cpu", "memoria"]]
y_class = df["status"]

knn = KNeighborsClassifier(n_neighbors = 3)
knn.fit(x_class, y_class)

pred = knn.predict(x_class)

# Clustering (KMeans)

kmeans = KMeans(n_clusters = 2)
df["grupo"] = kmeans.fit_predict(df[["cpu", "memoria"]])

# -------------------------
# PASSO 4 - Gráficos
# -------------------------

# CPU real vs previsão

plt.plot(df["cpu"], label="Real")
plt.plot(previsao, label="Previsão")
plt.legend()
plt.title("CPU Real vs Previsão")
plt.show()

# Clusters

plt.scatter(df["cpf"], df["memoria"], c =df["grupo"])
plt.xlabel("CPU")
plt.ylabel("Memória")
plt.title("Clusters de uso")
plt.show()
