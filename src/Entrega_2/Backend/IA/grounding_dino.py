import os
from autodistill.detection import CaptionOntology
from autodistill_grounding_dino import GroundingDINO

# 1. Configuração de Diretórios
# Aponte para a pasta onde estão as suas 868 imagens sem anotação
PASTA_IMAGENS_CRUAS = r"C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_2\Backend\IA\Brutas\Dataset-Val" 

# O diretório base do seu dataset, conforme o seu arquivo YAML
PASTA_SAIDA_YOLO = r"C:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_2\Backend\IA\dataset_yolo"

# 2. Definindo a Ontologia (Prompt em Inglês -> Nome exato da sua classe no YAML)
# Foram utilizados descritores visuais comuns para embalagens de supermercado
# Removemos o foco excessivo no "plastic bag" e adicionamos cores e texturas
ontologia = CaptionOntology({
    "white rice grains packaging": "Arroz",       # Foca nos grãos brancos
    "brown raw beans packaging": "Feijao",        # Foca nos grãos marrons (ou mude para "black" se for feijão preto)
    "white refined sugar package": "Acucar",      # Destaca o branco e refinado
    "yellow dry long pasta packaging": "Macarrao", # Foca na cor amarela e no formato longo/seco
    "yellow cornmeal flour package": "Fuba",      # Mantemos o foco na farinha de milho amarela
    "clear plastic bottle of yellow cooking oil": "Oleo" # Formato garrafa + líquido amarelo
})

print("Carregando o modelo Grounding DINO com filtros rigorosos...")
# Subimos os thresholds para forçar a IA a ter muita certeza
modelo_base = GroundingDINO(
    ontology=ontologia,
    box_threshold=0.90,  # Subimos 10 pontos: agora exige 65% de precisão na caixa
    text_threshold=0.90  # Subimos para 60% a exigência de que o objeto bata com a descrição
)

print(f"Iniciando a varredura e detecção de objetos nas imagens...")
# 4. Executando a rotulação em lote
modelo_base.label(
    input_folder=PASTA_IMAGENS_CRUAS,
    extension=".jpg", # Certifique-se de que a extensão corresponde às suas imagens
    output_folder=PASTA_SAIDA_YOLO
)

print(f"Processo finalizado! O dataset foi gerado em: {PASTA_SAIDA_YOLO}")