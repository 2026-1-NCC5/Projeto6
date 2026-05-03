import torch
from diffusers import StableDiffusionGLIGENPipeline

# 1. CARREGA O MODELO (Em 32-bits, à prova de falhas matemáticas)
print("Carregando o modelo na placa de vídeo...")
pipe = StableDiffusionGLIGENPipeline.from_pretrained(
    "masterful/gligen-1-4-generation-text-box", 
    safety_checker=None,           
    requires_safety_checker=False
)
pipe = pipe.to("cuda") # Manda o processamento para a GPU

# 2. CONFIGURAÇÃO AVANÇADA DOS PROMPTS

# Descrevemos o saco de arroz de 1KG em um ambiente realista (bancada de cozinha)
prompt_geral = "A detailed photorealistic close-up photograph of a 1 kg clear plastic bag filled with raw white rice, sitting on a clean wooden kitchen counter. The transparent packaging reveals the individual white rice grains inside. Soft natural daylight, supermarket product photography, 8k resolution, highly detailed, sharp focus."

# O objeto exato que o GLIGEN vai desenhar dentro da caixa
objeto_para_marcar = ["a 1 kg plastic bag of white rice"]

# Mantemos o prompt negativo forte para evitar textos borrados ou marcas da embalagem distorcidas
prompt_negativo = "watermark, text, logo, words, letters, signature, brand name, stock photo text, blurry, distorted shape, bad anatomy, ugly, low quality, worst quality"

# 3. DEFINE A BOUNDING BOX
# Mudamos a proporção: agora é um retângulo vertical para simular um saco de arroz em pé.
# [x_min, y_min, x_max, y_max] -> Começa em 25% da esquerda, 15% do topo; vai até 75% da direita, 85% do fundo.
caixa_gligen = [[0.25, 0.15, 0.75, 0.85]]

# 4. GERA A IMAGEM
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

# Atualizamos o nome do arquivo para o seu dataset
nome_arquivo = "saco_arroz_1kg_001"

# Salva a imagem
imagem_gerada.save(f"{nome_arquivo}.jpg")
print(f"Imagem {nome_arquivo}.jpg salva com sucesso!")

# --- GERAÇÃO DO ARQUIVO YOLO ---
x_min, y_min, x_max, y_max = caixa_gligen[0]

# Converte para o padrão YOLO
x_centro = (x_min + x_max) / 2
y_centro = (y_min + y_max) / 2
largura = x_max - x_min
altura = y_max - y_min

# Se o arroz for a sua classe 0 no arquivo classes.txt do YOLO, mantenha 0.
id_classe = 0 

linha_yolo = f"{id_classe} {x_centro:.6f} {y_centro:.6f} {largura:.6f} {altura:.6f}"

with open(f"{nome_arquivo}.txt", "w") as f:
    f.write(linha_yolo)

print(f"Arquivo de anotação {nome_arquivo}.txt gerado com sucesso!")