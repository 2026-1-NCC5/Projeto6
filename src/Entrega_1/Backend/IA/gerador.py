import os
import torch
import random
import time
from diffusers import StableDiffusionXLPipeline, AutoencoderKL
from transformers import AutoProcessor, AutoModelForZeroShotObjectDetection

# ---------------------------------------------------------
# 1. CONFIGURAÇÃO DE DIRETÓRIOS E YOLO
# ---------------------------------------------------------
PASTA_IMAGENS = "dataset/images/train"
PASTA_LABELS = "dataset/labels/train"

os.makedirs(PASTA_IMAGENS, exist_ok=True)
os.makedirs(PASTA_LABELS, exist_ok=True)

ID_CLASSE_YOLO = 0
CONFIANCA_MINIMA = 0.35 # Só salva se a IA tiver 35%+ de certeza que achou o saco

# ---------------------------------------------------------
# 2. CARREGAMENTO DOS MODELOS (GPU)
# ---------------------------------------------------------
print("Carregando VAE em 32-bits (Proteção contra tela preta)...")
vae_seguro = AutoencoderKL.from_pretrained(
    "stabilityai/stable-diffusion-xl-base-1.0", 
    subfolder="vae",
    torch_dtype=torch.float32 # <-- Forçamos o VAE inteiro em 32-bits desde o início
)

print("Carregando SDXL (Gerador de Imagens)...")
gerador = StableDiffusionXLPipeline.from_pretrained(
    "stabilityai/stable-diffusion-xl-base-1.0", 
    vae=vae_seguro, # Injetamos o VAE seguro aqui
    torch_dtype=torch.float16, # O resto do modelo continua leve em 16-bits
    variant="fp16", 
    use_safetensors=True
)
gerador.to("cuda")
# ATENÇÃO: Remova ou comente aquela linha gerador.vae.to(torch.float32) !

gerador.enable_vae_slicing()

print("Carregando GroundingDINO (Anotador Zero-Shot)...")
processor = AutoProcessor.from_pretrained("IDEA-Research/grounding-dino-base")
detector = AutoModelForZeroShotObjectDetection.from_pretrained("IDEA-Research/grounding-dino-base").to("cuda")

# ---------------------------------------------------------
# 3. LISTAS DE VARIABILIDADE
# ---------------------------------------------------------
cores = ["blue and white", "red and transparent", "green and yellow", "orange", "minimalist white", "yellow and black"]
ambientes = ["on a clean wooden kitchen counter", "on a metal supermarket shelf", "inside a grocery cart", "isolated on a studio background"]
detalhes = ["bold brand logo, nutritional information text", "colorful graphic design, barcode", "commercial packaging design"]

# ---------------------------------------------------------
# 4. LOOP INFINITO DE GERAÇÃO
# ---------------------------------------------------------
contador = 1
print("\nIniciando mineração de dados. Pressione Ctrl+C para parar.\n")

while True:
    try:
        # Sorteia as características
        cor = random.choice(cores)
        ambiente = random.choice(ambientes)
        detalhe = random.choice(detalhes)

        prompt_geral = (
            f"A highly detailed photograph of a 1 kg commercial plastic bag of white rice. "
            f"The packaging is {cor}, featuring {detalhe}. "
            f"The bag is sitting {ambiente}. "
            f"Supermarket product photography, 8k resolution, sharp focus."
        )

        print(f"[{contador}] Gerando imagem no MODO TESTE...")
        
        # 1. GERAÇÃO (Vírgulas corrigidas, resolução segura e passos mínimos viáveis)
        resultado = gerador(
            prompt=prompt_geral, 
            height=768, 
            width=768, 
            num_inference_steps=25, 
            guidance_scale=7.5, 
            output_type="latent" 
        )
        latentes = resultado.images
        
        # --- DETECTOR DE TELA PRETA (Diagnóstico Físico da GPU) ---
        # Se a sua placa sofrer o apagão do 16-bits, a matriz enche de "NaN"
        if torch.isnan(latentes).any():
            print("⚠️ ERRO DE HARDWARE: A GPU falhou no cálculo (Gerou NaN no UNet).")
            print("A imagem sairia irremediavelmente preta. Descartando ciclo...\n")
            continue
        # -----------------------------------------------------------
        
        # 2. CONVERSÃO MANUAL
        latentes = latentes.to(dtype=torch.float32)
        latentes = latentes / gerador.vae.config.scaling_factor
        
        # 3. DECODIFICAÇÃO MANUAL
        with torch.no_grad():
            imagem_tensor = gerador.vae.decode(latentes, return_dict=False)[0]
            
        # 4. FINALIZAÇÃO
        imagem = gerador.image_processor.postprocess(imagem_tensor, output_type="pil")[0]

        # DEBUG VISUAL: Salva a imagem temporariamente antes do DINO avaliar.
        # Assim você pode abrir o arquivo e ver se parece um saco de arroz ou apenas borrões.
        imagem.save(f"debug_visual_{contador}.jpg")

        # ---------------------------------------------------------
        # 5. DETECÇÃO ZERO-SHOT (GROUNDING DINO)
        # ---------------------------------------------------------
        # O DINO entende texto, então pedimos para ele procurar isso na imagem:
        texto_busca = "a plastic bag of rice."
        
        inputs = processor(images=imagem, text=texto_busca, return_tensors="pt").to("cuda")
        
        with torch.no_grad():
            outputs = detector(**inputs)

        # Processa o resultado do detector
        target_sizes = torch.tensor([imagem.size[::-1]])
        resultados_dino = processor.image_processor.post_process_object_detection(
            outputs, target_sizes=target_sizes, threshold=CONFIANCA_MINIMA
        )[0]

        # Verifica se encontrou algo
        if len(resultados_dino["scores"]) > 0:
            # Pega a caixa com maior confiança
            indice_maior_confianca = torch.argmax(resultados_dino["scores"]).item()
            caixa = resultados_dino["boxes"][indice_maior_confianca].tolist() # [x_min, y_min, x_max, y_max] absolutos
            
            # Converte as coordenadas absolutas para o formato normalizado do YOLO
            largura_img, altura_img = imagem.size
            
            x_min, y_min, x_max, y_max = caixa
            
            # Normalização (0 a 1)
            x_centro = ((x_min + x_max) / 2) / largura_img
            y_centro = ((y_min + y_max) / 2) / altura_img
            largura_box = (x_max - x_min) / largura_img
            altura_box = (y_max - y_min) / altura_img

            # ---------------------------------------------------------
            # 6. SALVAMENTO DOS ARQUIVOS
            # ---------------------------------------------------------
            nome_base = f"saco_arroz_{contador:05d}"
            
            # Salva Imagem
            caminho_imagem = os.path.join(PASTA_IMAGENS, f"{nome_base}.jpg")
            imagem.save(caminho_imagem)
            
            # Salva Label YOLO
            linha_yolo = f"{ID_CLASSE_YOLO} {x_centro:.6f} {y_centro:.6f} {largura_box:.6f} {altura_box:.6f}"
            caminho_label = os.path.join(PASTA_LABELS, f"{nome_base}.txt")
            with open(caminho_label, "w") as f:
                f.write(linha_yolo)
                
            print(f"Sucesso! Salvo como {nome_base} (Confiança DINO: {resultados_dino['scores'][indice_maior_confianca]:.2f})\n")
            contador += 1
            print("Pausando por 10 segundos para resfriamento da VRAM e GPU...")
            time.sleep(10)
        else:
            print("IA gerou a imagem, mas o detector não encontrou o pacote com clareza. Descartando imagem e tentando de novo.\n")

        # Limpa o cache da GPU para evitar memory leak no loop
        torch.cuda.empty_cache()

    except KeyboardInterrupt:
        print("\nMineração interrompida pelo usuário. Finalizando com segurança.")
        break
    except Exception as e:
        print(f"\nOcorreu um erro no ciclo atual: {e}. Tentando o próximo ciclo...\n")
        time.sleep(2) # Pausa rápida para não fritar o loop de erros
        continue