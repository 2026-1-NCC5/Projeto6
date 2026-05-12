import os
from pathlib import Path

def main():
    # Caminhos baseados no diretório do script
    base_dir = Path(__file__).parent
    csv_path = base_dir / "remover.CSV"
    labels_dir = base_dir / "dataset" / "labels"
    
    if not csv_path.exists():
        print(f"Erro: Arquivo CSV não encontrado em {csv_path}")
        return
        
    if not labels_dir.exists():
        print(f"Erro: Diretório de labels não encontrado em {labels_dir}")
        return

    # Lendo os nomes do arquivo CSV
    names_to_remove = set()
    with open(csv_path, "r", encoding="utf-8") as f:
        for line in f:
            name = line.strip()
            # Ignorar linhas vazias
            if name:
                names_to_remove.add(name)

    print(f"Total de itens para remover listados no CSV: {len(names_to_remove)}")

    # Contadores
    removed_count = 0
    
    # Busca e remove os arquivos de label correspondentes (.txt)
    for name in names_to_remove:
        # Usando rglob para procurar em todas as subpastas (como train, val, etc.)
        for txt_file in labels_dir.rglob(f"{name}.txt"):
            if txt_file.is_file():
                try:
                    txt_file.unlink()
                    print(f"Removido: {txt_file.relative_to(base_dir)}")
                    removed_count += 1
                except Exception as e:
                    print(f"Erro ao remover {txt_file}: {e}")

    print(f"\nOperação concluída. Total de labels removidos: {removed_count}")

if __name__ == "__main__":
    main()
