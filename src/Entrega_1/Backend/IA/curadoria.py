import os
import shutil
from pathlib import Path
from PIL import Image
from transformers import pipeline

# 1. CONFIGURAÇÕES
PASTA_DATASET = Path("C:/Projetos/Faculdade/AlimempatIA/AlimempatIA/Projeto6/src/Entrega_1/Backend/IA/dataset_yolo/images/train")
PASTA_LABELS = Path("C:/Projetos/Faculdade/AlimempatIA/AlimempatIA/Projeto6/src/Entrega_1/Backend/IA/dataset_yolo/labels/train")
PASTA_QUARENTENA = Path("./dataset_quarentena")

LIMIAR_MINIMO = 0.35

# O Catálogo Oficial
CATALOGO_BUSCA = {
    "Arroz": "a plastic bag of white rice",
    "Feijao": "a plastic bag of raw beans",
    "Acucar": "a plastic bag of refined sugar",
    "Macarrao": "a package of spaghetti pasta",
    "Fuba": "a plastic bag of yellow cornmeal",
    "Oleo": "a plastic bottle of cooking oil"
}

# As "Pegadinhas" (Distratores) para testar a qualidade da imagem
DISTRATORES = [
    "a generic blank plastic bag", 
    "an unidentifiable metallic package",
    "a blurry shapeless object",
    "an empty box"
]

# Cria a lista completa de múltipla escolha (Todos os produtos + Distratores)
TODAS_AS_LABELS = list(CATALOGO_BUSCA.values()) + DISTRATORES

def preparar_quarentena():
    (PASTA_QUARENTENA / "images").mkdir(parents=True, exist_ok=True)
    (PASTA_QUARENTENA / "labels").mkdir(parents=True, exist_ok=True)

def main():
    print("Carregando o Inspetor DINO (Modo Classificação Contrastiva)...")
    inspetor = pipeline(model="IDEA-Research/grounding-dino-tiny", task="zero-shot-object-detection", device=0)
    
    preparar_quarentena()
    imagens = list(PASTA_DATASET.glob("*.jpg"))
    removidas = 0
    
    print(f"Iniciando faxina competitiva em {len(imagens)} imagens de treino...")
    
    for caminho_img in imagens:
        nome_arquivo = caminho_img.name
        classe_produto = nome_arquivo.split('_')[1] 
        
        if classe_produto not in CATALOGO_BUSCA:
            continue
            
        texto_esperado = CATALOGO_BUSCA[classe_produto]
        imagem_pil = Image.open(caminho_img).convert("RGB")
        
        # Pede pro DINO achar o que quiser dentro de TODAS as opções possíveis
        resultados = inspetor(
            imagem_pil,
            candidate_labels=TODAS_AS_LABELS
        )
        
        imagem_valida = False
        motivo_rejeicao = ""
        
        if len(resultados) > 0:
            # Pega o resultado com a MAIOR CERTEZA ABSOLUTA na imagem
            melhor_resultado = max(resultados, key=lambda x: x['score'])
            label_vencedora = melhor_resultado['label']
            certeza = melhor_resultado['score']
            
            # A imagem só passa se o DINO escolher O PRODUTO CERTO e com uma nota aceitável
            if label_vencedora == texto_esperado and certeza >= LIMIAR_MINIMO:
                imagem_valida = True
            else:
                motivo_rejeicao = f"DINO achou que parecia mais com '{label_vencedora}' ({certeza:.2f})"
        else:
            motivo_rejeicao = "DINO não encontrou absolutamente nada."
                
        if not imagem_valida:
            print(f"[REJEITADA] {nome_arquivo} | Motivo: {motivo_rejeicao}")
            
            shutil.move(caminho_img, PASTA_QUARENTENA / "images" / nome_arquivo)
            
            caminho_txt = PASTA_LABELS / caminho_img.with_suffix(".txt").name
            if caminho_txt.exists():
                shutil.move(caminho_txt, PASTA_QUARENTENA / "labels" / caminho_txt.name)
                
            removidas += 1
            
    print("-" * 30)
    print(f"Faxina Concluída! {removidas} imagens com qualidade duvidosa foram removidas.")

if __name__ == "__main__":
    main()