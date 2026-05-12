import os
from pathlib import Path

def verificar_labels_faltantes(dir_imagens, dir_labels):
    caminho_imagens = Path(dir_imagens)
    caminho_labels = Path(dir_labels)

    if not caminho_imagens.exists():
        print(f"Erro: A pasta de imagens não existe: {caminho_imagens}")
        return
    
    if not caminho_labels.exists():
        print(f"Erro: A pasta de labels não existe: {caminho_labels}")
        return

    # Extensões de imagem consideradas (pode adicionar mais se precisar)
    extensoes_validas = {'.jpg', '.jpeg', '.png', '.bmp', '.webp'}
    
    imagens_sem_label = []

    for arquivo_imagem in caminho_imagens.iterdir():
        if arquivo_imagem.is_file() and arquivo_imagem.suffix.lower() in extensoes_validas:
            # O YOLO espera um arquivo .txt com o mesmo nome da imagem
            arquivo_label = caminho_labels / f"{arquivo_imagem.stem}.txt"
            
            if not arquivo_label.exists():
                imagens_sem_label.append(arquivo_imagem.name)

    if imagens_sem_label:
        print(f"Total de {len(imagens_sem_label)} imagens sem o arquivo .txt correspondente:")
        for nome_img in imagens_sem_label:
            print(f" -> {nome_img}")
    else:
        print("Tudo certo! Todas as imagens possuem seus respectivos arquivos de label (.txt).")

if __name__ == "__main__":
    # Caminhos com base no que você forneceu
    PASTA_IMAGENS = r"c:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_1\Backend\IA\dataset\images"
    PASTA_LABELS = r"c:\Projetos\Faculdade\AlimempatIA\AlimempatIA\Projeto6\src\Entrega_1\Backend\IA\dataset\labels"
    
    print("Verificando imagens sem labels...")
    verificar_labels_faltantes(PASTA_IMAGENS, PASTA_LABELS)
