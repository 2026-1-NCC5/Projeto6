import torch
import random
from diffusers import StableDiffusionGLIGENPipeline

# 1. CARREGA O MODELO
print("Carregando o modelo na placa de vídeo...")
pipe = StableDiffusionGLIGENPipeline.from_pretrained(
    "masterful/gligen-1-4-generation-text-box", 
    safety_checker=None,           
    requires_safety_checker=False
)
pipe = pipe.to("cuda")

# 2. LISTAS DE VARIABILIDADE PARA O DATASET
cores_embalagem = [
    "blue and white", "red and transparent", "green and yellow", 
    "orange", "metallic silver with bright logos", "minimalist white and red",
    "transparent with colorful branding", "yellow and black"
]

ambientes = [
    "on a clean wooden kitchen counter", "on a metal supermarket shelf", 
    "inside a grocery shopping cart", "on a dark granite countertop", 
    "on a messy pantry shelf", "isolated on a studio background"
]

detalhes_comerciais = [
    "bold brand logo, nutritional information text", 
    "colorful graphic design, barcode, price tag",
    "commercial packaging design, text elements",
    "supermarket product, bright labels, text"
]

# Sorteia as características para a imagem atual
cor_escolhida = random.choice(cores_embalagem)
ambiente_escolhido = random.choice(ambientes)
detalhe_escolhido = random.choice(detalhes_comerciais)

# 3. CONFIGURAÇÃO DINÂMICA DOS PROMPTS
prompt_geral = (
    f"A detailed photorealistic close-up photograph of a 1 kg commercial plastic bag of white rice. "
    f"The packaging is {cor_escolhida}, featuring {detalhe_escolhido}. "
    f"The bag is sitting {ambiente_escolhido}. "
    f"Soft natural daylight, 8k resolution, highly detailed, supermarket product photography."
)

print(f"Prompt gerado para esta iteração:\n{prompt_geral}\n")

objeto_para_marcar = ["a commercial 1 kg plastic bag of white rice"]

# Atualização crucial: Removemos a restrição de texto e logos do prompt negativo!
# Mantemos apenas restrições de qualidade visual.
prompt_negativo = "blurry, distorted shape, bad anatomy, ugly, low quality, worst quality, out of focus, deformed packaging"

# 4. DEFINE A BOUNDING BOX
caixa_gligen = [[0.25, 0.15, 0.75, 0.85]]

# 5. GERA A IMAGEM
print("Gerando a imagem do saco de arroz...")
resultado = pipe(
    prompt=prompt_geral,
    negative_prompt=prompt_negativo,
    gligen_phrases=objeto_para_marcar,
    gligen_boxes=caixa_gligen,
    height=512, 
    width=512,  
    gligen_scheduled_sampling_beta=1.0,
    output_type="pil",
    num_inference_steps=50, 
    guidance_scale=8.5, 
)

imagem_gerada = resultado.images[0]

# --- SALVAMENTO E GERAÇÃO DO ARQUIVO YOLO ---
# Em um loop real, você usaria um contador (ex: f"saco_arroz_{i:04d}")
nome_arquivo = "saco_arroz_treino_001"

imagem_gerada.save(f"{nome_arquivo}.jpg")
print(f"Imagem {nome_arquivo}.jpg salva com sucesso!")

x_min, y_min, x_max, y_max = caixa_gligen[0]
x_centro = (x_min + x_max) / 2
y_centro = (y_min + y_max) / 2
largura = x_max - x_min
altura = y_max - y_min

id_classe = 0 
linha_yolo = f"{id_classe} {x_centro:.6f} {y_centro:.6f} {largura:.6f} {altura:.6f}"

with open(f"{nome_arquivo}.txt", "w") as f:
    f.write(linha_yolo)

print(f"Arquivo de anotação {nome_arquivo}.txt gerado com sucesso!")