import os
import torch
import random
import time
from diffusers import StableDiffusionXLPipeline, AutoencoderKL
from transformers import AutoProcessor, AutoModelForZeroShotObjectDetection


# ---------------------------------------------------------
# 1. CARREGAMENTO DOS MODELOS (GPU)
# ---------------------------------------------------------
print("Carregando VAE em 32-bits (Proteção contra tela preta)...")
vae_seguro = AutoencoderKL.from_pretrained(
    "stabilityai/stable-diffusion-xl-base-1.0", 
    subfolder="vae",
    torch_dtype=torch.float32 
)

print("Carregando SDXL (Gerador de Imagens)...")
gerador = StableDiffusionXLPipeline.from_pretrained(
    "stabilityai/stable-diffusion-xl-base-1.0", 
    vae=vae_seguro, 
    torch_dtype=torch.float16, 
    variant="fp16", 
    use_safetensors=True
)

# ATENÇÃO: A linha gerador.to("cuda") FOI REMOVIDA DAQUI! 
# Deixe o gerenciamento de memória exclusivamente para o Offload.

# Ordem correta de otimização de memória extrema:
gerador.enable_model_cpu_offload()
gerador.enable_vae_slicing()
gerador.enable_attention_slicing() # <-- NOVO: Fatiamento matemático para GPUs pequenas

processor = AutoProcessor.from_pretrained("IDEA-Research/grounding-dino-base")
detector = AutoModelForZeroShotObjectDetection.from_pretrained("IDEA-Research/grounding-dino-base").to("cuda")
# ---------------------------------------------------------
# 1 e 3. CONFIGURAÇÃO DE DIRETÓRIOS E MATRIZ DE PRODUTOS
# ---------------------------------------------------------
PASTA_IMAGENS = "dataset/images/train"
PASTA_LABELS = "dataset/labels/train"

os.makedirs(PASTA_IMAGENS, exist_ok=True)
os.makedirs(PASTA_LABELS, exist_ok=True)

CONFIANCA_MINIMA = 0.35

# CATÁLOGO DE PRODUTOS — Prompts realistas estilo supermercado brasileiro
# ID YOLO | O que o SDXL vai desenhar | O que o DINO vai procurar
# DICA: Os prompts enfatizam as características visuais ÚNICAS de cada embalagem
#       para que o modelo aprenda a diferenciá-las no mundo real.
CATALOGO_PRODUTOS = {
    "Arroz": {
        "id_yolo": 0,
        # Arroz brasileiro: saco de plástico transparente/translúcido onde se vêem os grãos brancos,
        # geralmente 5kg, marcas Camil/Tio João com rótulo azul, vermelho ou verde.
        "prompt_visual": "a 5 kg transparent plastic bag of Brazilian white rice, the white rice grains are clearly visible through the clear plastic packaging, colorful printed label wrapped around the bag",
        "busca_dino": "a plastic bag of rice. a package of rice."
    },
    "Feijao": {
        "id_yolo": 1,
        # Feijão brasileiro (Carioca): saco de plástico transparente com grãos menores e mais claros.
        "prompt_visual": "a 1 kg transparent plastic bag of Brazilian carioca beans, the beans are very small and light tan or beige in color, small light brown grains clearly visible through the clear plastic, printed label on the front with warm brown and red colors",
        "busca_dino": "a plastic bag of beans. a package of beans."
    },
    "Acucar": {
        "id_yolo": 2,
        # Açúcar brasileiro: saco de PAPEL OPACO branco (não transparente!), não dá para ver o conteúdo,
        # geralmente 1kg ou 5kg, marcas União/Caravelas com letras grandes.
        "prompt_visual": "a 1 kg sealed opaque white plastic bag of refined crystal sugar, the bag is NOT transparent, thick white plastic packaging with large bold colorful text and logo printed on it, sealed shut",
        "busca_dino": "a paper bag of sugar. a package of sugar."
    },
    "Macarrao": {
        "id_yolo": 3,
        # Macarrão brasileiro: pacote plástico transparente fino e retangular/comprido,
        # espaguete amarelo visível dentro, marcas Renata/Barilla/Adria.
        "prompt_visual": "a 500g long rectangular transparent plastic package of dry spaghetti pasta, the long yellow pasta noodles are clearly visible through the cellophane wrapping, thin flat package shape, small printed label",
        "busca_dino": "a package of pasta. a package of spaghetti."
    },
    "Fuba": {
        "id_yolo": 4,
        # Fubá brasileiro: saco plástico opaco AMARELO brilhante (cor muito característica),
        # geralmente 500g ou 1kg, marcas Yoki/Sinhá.
        "prompt_visual": "a 500g sealed bright yellow opaque plastic bag of cornmeal flour, the bag itself is distinctly yellow colored, printed label with the word FUBA or cornmeal, sealed plastic package",
        "busca_dino": "a plastic bag of cornmeal. a yellow bag of flour."
    },
    "Oleo": {
        "id_yolo": 5,
        # Óleo brasileiro: garrafa PET transparente com líquido dourado/amarelo dentro,
        # formato cilíndrico com tampa, marcas Soya/Liza/Sadia.
        "prompt_visual": "a 900ml clear transparent PET plastic bottle of golden yellow soybean cooking oil, cylindrical bottle shape with a screw cap, golden liquid clearly visible inside, printed label wrapped around the bottle",
        "busca_dino": "a plastic bottle of cooking oil. a bottle of oil."
    }
}

# Cores de embalagem mais realistas para produtos brasileiros
cores = [
    "blue and white", "red and yellow", "green and white", "orange and white",
    "brown and beige", "yellow and red", "white and blue", "red and white"
]

# Ambientes que simulam fotos reais tiradas com celular no dia a dia
ambientes = [
    "on a simple wooden kitchen counter, slightly messy background",
    "on a metal supermarket shelf next to other products",
    "inside a plastic grocery bag on a table",
    "being held by a hand in front of a kitchen background",
    "on a white kitchen countertop with natural window light",
    "on a tiled kitchen floor, cellphone photo angle from above",
    "on a simple table with a plain wall behind",
    "slightly tilted on a cluttered kitchen counter"
]

# Detalhes que imitam fotos amadoras de celular (reduz o domain gap)
detalhes = [
    "bold brand logo, nutritional information text, printed in Portuguese",
    "colorful graphic design, barcode, price sticker",
    "commercial packaging design, slightly wrinkled packaging",
    "realistic cellphone photo quality, slight motion blur",
    "natural indoor lighting, mild shadows, no studio setup"
]
# ---------------------------------------------------------
# 4. LOOP INFINITO DE GERAÇÃO
# ---------------------------------------------------------
contador = 1
print("\nIniciando mineração de dados. Pressione Ctrl+C para parar.\n")

while True:
    try:
        # 15% de chance de gerar imagem de Fundo (Background / Negative Sample)
        gerar_fundo = random.random() < 0.15

        if gerar_fundo:
            nome_produto = "Fundo"
            cor = "none" # Fundos não têm cor de embalagem
            prompt_fundo = random.choice([
                "an empty clean wooden kitchen counter, natural lighting",
                "a blank white A4 paper sheet on a table",
                "a messy supermarket shelf without any products",
                "a simple empty kitchen table, indoor lighting",
                "a close up of an empty hand against a wall background",
                "some random cups and plates on a dining table",
                "a very cluttered kitchen table full of random unrelated objects",
                "a blurry person walking in the background",
                "a close up of a person's leg and foot on the floor",
                "a messy living room with a couch and tv",
                "a bedroom interior with a bed and wardrobe",
                "a refrigerator door with magnets, seen from up close",
                "people shopping in a supermarket aisle, out of focus",
                "a person's hand holding a generic blank plastic container"
            ])
            prompt_geral = (
                f"A realistic cellphone photograph of {prompt_fundo}. "
                f"Realistic amateur photo, slightly imperfect focus as if taken with a smartphone camera."
            )
        else:
            nome_produto = random.choice(list(CATALOGO_PRODUTOS.keys()))
            dados_produto = CATALOGO_PRODUTOS[nome_produto]
            
            cor = random.choice(cores)
            ambiente = random.choice(ambientes)
            detalhe = random.choice(detalhes)

            prompt_geral = (
                f"A realistic cellphone photograph of {dados_produto['prompt_visual']}. "
                f"The packaging colors are {cor}, featuring {detalhe}. "
                f"The product is {ambiente}. "
                f"Realistic amateur photo, natural lighting, Brazilian grocery product, "
                f"slightly imperfect focus as if taken with a smartphone camera."
            )

        print(f"\n[{contador}] Fabricando: {nome_produto.upper()}...")
        
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
        # 5. DETECÇÃO ZERO-SHOT (GROUNDING DINO) OU BACKGROUND
        # ---------------------------------------------------------
        if gerar_fundo:
            # Imagens de fundo NÃO passam pelo DINO e não ganham arquivo .txt (YOLO entende como background)
            nome_base = f"imagem_Fundo_{contador:05d}"
            caminho_imagem = os.path.join(PASTA_IMAGENS, f"{nome_base}.jpg")
            imagem.save(caminho_imagem)
            
            print(f"Sucesso! Salvo como BACKGROUND: {nome_base}.jpg (Sem arquivo .txt)\n")
            contador += 1
            print("Pausando por 10 segundos para resfriamento da VRAM e GPU...")
            time.sleep(10)
        else:
            # O DINO entende texto, então pedimos para ele procurar isso na imagem:
            texto_busca = dados_produto['busca_dino']
            
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
                nome_base = f"imagem_{nome_produto}_{cor}_{contador:05d}"
                
                # Salva Imagem
                caminho_imagem = os.path.join(PASTA_IMAGENS, f"{nome_base}.jpg")
                imagem.save(caminho_imagem)
                
                # Puxa o ID da classe dinamicamente (0 a 6)
                id_da_classe_atual = dados_produto['id_yolo']
                
                # Salva Label YOLO
                linha_yolo = f"{id_da_classe_atual} {x_centro:.6f} {y_centro:.6f} {largura_box:.6f} {altura_box:.6f}"
                caminho_label = os.path.join(PASTA_LABELS, f"{nome_base}.txt") # Adicionando o nome no arquivo ajuda a debugar!

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