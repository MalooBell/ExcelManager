# app/models.py

from pydantic import BaseModel, Field
from typing import List, Dict, Any

class ColumnSchema(BaseModel):
    """
    Décrit la structure d'une colonne détectée dans le fichier Excel.
    """
    name: str = Field(..., description="Nom de la colonne (l'en-tête).")
    # Le type est une chaîne pour l'instant, mais pourrait être enrichi plus tard
    # pour inclure des types de données détectés (ex: 'nombre', 'texte', 'date').
    type: str = Field(default="string", description="Type de données potentiel de la colonne.")

class SheetData(BaseModel):
    """
    Représente les données et le schéma d'une seule feuille Excel.
    """
    sheet_name: str = Field(..., description="Nom de la feuille de calcul.")
    schema: List[ColumnSchema] = Field(..., description="Le schéma des colonnes pour cette feuille.")
    data: List[Dict[str, Any]] = Field(..., description="Les données de la feuille, où chaque élément est un dictionnaire représentant une ligne.")
    total_rows: int = Field(..., description="Nombre total de lignes de données extraites.")

class ExcelProcessingResponse(BaseModel):
    """
    La réponse JSON finale contenant les données de toutes les feuilles traitées.
    """
    file_name: str = Field(..., description="Nom du fichier original.")
    sheets: List[SheetData] = Field(..., description="Liste des feuilles traitées du fichier.")