# app/main.py

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
import traceback  # Importé pour un meilleur logging des erreurs

from .models import ExcelProcessingResponse
from .excel_parser import process_excel_file

# Crée une instance de l'application FastAPI
app = FastAPI(
    title="Excel Processor API",
    description="Un microservice pour traiter les fichiers Excel complexes.",
    version="1.0.0"
)

@app.get("/", tags=["Root"])
async def read_root():
    """
    Point d'entrée de base pour vérifier que l'API est en cours d'exécution.
    """
    return {"message": "Bienvenue sur l'API de traitement Excel. Le service est opérationnel."}


# Le décorateur @app.post indique que cette fonction gère les requêtes POST.
# response_model=ExcelProcessingResponse garantit que la réponse sera conforme
# à notre modèle Pydantic, et l'inclut dans la documentation.
@app.post("/process-excel",
          response_model=ExcelProcessingResponse,
          tags=["Excel Processing"],
          summary="Traite un fichier Excel et retourne ses données en JSON structuré.")
async def process_excel(file: UploadFile = File(..., description="Le fichier Excel (.xlsx, .xls) à traiter.")):
    """
    Ce point d'API réalise les actions suivantes :
    - **Reçoit un fichier Excel** téléversé par un client.
    - **Lit le contenu** du fichier en mémoire.
    - **Appelle la logique de traitement** pour gérer les en-têtes complexes et les cellules fusionnées.
    - **Retourne une structure JSON** standardisée contenant les données de chaque feuille.

    En cas d'erreur (fichier corrompu, format non supporté), une erreur HTTP 400 est retournée
    avec un message explicatif.
    """
    # Vérifie l'extension du fichier pour s'assurer qu'il s'agit d'un format Excel supporté.
    if not file.filename.endswith(('.xlsx', '.xls')):
        raise HTTPException(status_code=400, detail="Format de fichier invalide. Seuls les fichiers .xlsx et .xls sont acceptés.")

    try:
        # Lit le contenu complet du fichier téléversé en mémoire sous forme de bytes.
        file_content = await file.read()
        
        # Appelle notre fonction de traitement principale avec le contenu et le nom du fichier.
        processed_data = process_excel_file(file_content, file.filename)
        
        # Retourne les données traitées. FastAPI se chargera de la conversion en JSON.
        return processed_data

    except ValueError as ve:
        # Gère spécifiquement les erreurs fonctionnelles levées par notre parser (ex: fichier illisible).
        raise HTTPException(status_code=400, detail=str(ve))
    except Exception as e:
        # Gère toutes les autres erreurs inattendues pour éviter de faire planter le serveur.
        # Affiche la trace de l'erreur dans la console du serveur pour le débogage.
        print(f"Une erreur inattendue est survenue : {e}")
        traceback.print_exc()
        # Retourne une réponse d'erreur générique au client.
        raise HTTPException(status_code=500, detail="Une erreur interne est survenue lors du traitement du fichier.")