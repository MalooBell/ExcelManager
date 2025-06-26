# app/excel_parser.py

import pandas as pd
import io
from typing import List, Dict, Any
from .models import ColumnSchema, SheetData

def find_header_and_data(df: pd.DataFrame) -> (int, pd.DataFrame):
    """
    Analyse un DataFrame pour trouver la ligne d'en-tête la plus probable et nettoie le DataFrame.

    Cette fonction est cruciale pour les fichiers Excel "mal formatés" où les données
    ne commencent pas à la première ligne.

    Args:
        df (pd.DataFrame): Le DataFrame brut lu depuis Excel.

    Returns:
        tuple: Un tuple contenant l'index de la ligne d'en-tête et le DataFrame nettoyé.
    """
    # On cherche la première ligne qui a le plus de cellules non-vides.
    # C'est souvent un bon indicateur de la ligne d'en-tête.
    non_empty_counts = df.notna().sum(axis=1)
    
    # On ignore les lignes qui n'ont qu'une seule valeur (souvent des titres ou des notes)
    non_empty_counts = non_empty_counts[non_empty_counts > 1]

    if non_empty_counts.empty:
        # Si aucune ligne n'a plus d'une valeur, on suppose qu'il n'y a pas d'en-tête ou de données.
        return 0, pd.DataFrame()

    header_row_index = non_empty_counts.idxmax()

    # Les nouvelles colonnes sont définies par les valeurs de la ligne d'en-tête trouvée.
    new_columns = df.iloc[header_row_index].astype(str)
    
    # On prend les données à partir de la ligne *après* l'en-tête.
    data_df = df.iloc[header_row_index + 1:].copy()
    data_df.columns = new_columns
    
    # Réinitialise l'index pour avoir des index propres à partir de 0.
    data_df.reset_index(drop=True, inplace=True)
    
    return header_row_index, data_df

def process_excel_file(file_content: bytes, file_name: str) -> Dict[str, Any]:
    """
    Fonction principale pour traiter le contenu d'un fichier Excel.

    Args:
        file_content (bytes): Le contenu binaire du fichier Excel.
        file_name (str): Le nom du fichier original.

    Returns:
        Dict[str, Any]: Un dictionnaire structuré contenant les données du fichier,
                        conforme au modèle ExcelProcessingResponse.
    """
    try:
        # On utilise pd.ExcelFile pour pouvoir itérer sur les feuilles plus facilement.
        # 'openpyxl' est le moteur utilisé pour les fichiers .xlsx.
        xls = pd.ExcelFile(io.BytesIO(file_content), engine='openpyxl')
    except Exception as e:
        # Gère les cas où le fichier est corrompu ou n'est pas un format Excel valide.
        raise ValueError(f"Impossible de lire le fichier Excel. Est-il corrompu ? Erreur : {e}")

    processed_sheets = []

    # On boucle sur chaque nom de feuille dans le fichier.
    for sheet_name in xls.sheet_names:
        # On lit la feuille entière sans supposer d'en-tête pour le moment.
        # `header=None` est crucial ici.
        df_raw = pd.read_excel(xls, sheet_name=sheet_name, header=None)
        
        # On gère les cellules fusionnées en propageant la dernière valeur valide.
        # C'est la méthode standard pour "remplir" les cellules fusionnées.
        df_filled = df_raw.ffill()

        # On appelle notre fonction intelligente pour trouver l'en-tête et les vraies données.
        header_index, df_data = find_header_and_data(df_filled)
        
        # Si aucune donnée n'est trouvée dans la feuille, on passe à la suivante.
        if df_data.empty:
            continue

        # Convertit les types non supportés par JSON (comme les Timestamps) en chaînes de caractères.
        # NaT (Not a Time) est remplacé par None (qui deviendra null en JSON).
        for col in df_data.columns:
            if pd.api.types.is_datetime64_any_dtype(df_data[col]):
                df_data[col] = df_data[col].dt.strftime('%Y-%m-%d %H:%M:%S').replace({pd.NaT: None})

        # Remplace les NaN (Not a Number) de pandas par None pour une conversion JSON propre.
        df_data = df_data.where(pd.notna(df_data), None)

        # Création du schéma de colonnes.
        schema = [ColumnSchema(name=str(col)) for col in df_data.columns]
        
        # Conversion du DataFrame en une liste de dictionnaires.
        data_records = df_data.to_dict(orient='records')

        # Ajout des données traitées de la feuille à notre liste.
        processed_sheets.append(SheetData(
            sheet_name=sheet_name,
            schema=schema,
            data=data_records,
            total_rows=len(data_records)
        ))

    # Construit la réponse finale.
    response_data = {
        "file_name": file_name,
        "sheets": [sheet.dict() for sheet in processed_sheets]
    }

    return response_data