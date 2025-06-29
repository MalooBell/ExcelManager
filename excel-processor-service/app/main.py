# app/main.py (version mise à jour)

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse # Import nécessaire
import traceback

from .models import ExcelProcessingResponse
from .excel_parser import process_excel_file

app = FastAPI(
    title="Excel Processor API",
    description="Un microservice pour traiter les fichiers Excel complexes.",
    version="1.0.0"
)

# ... (la route root "/" ne change pas) ...
@app.get("/", tags=["Root"])
async def read_root():
    return {"message": "Bienvenue sur l'API de traitement Excel. Le service est opérationnel."}


@app.post("/process-excel",
          response_model=ExcelProcessingResponse,
          tags=["Excel Processing"],
          summary="Traite un fichier Excel et retourne ses données en JSON structuré.")
async def process_excel(file: UploadFile = File(..., description="Le fichier Excel (.xlsx, .xls) à traiter.")):
    if not file.filename.endswith(('.xlsx', '.xls')):
        # On retourne une erreur structurée
        return JSONResponse(
            status_code=400,
            content={"error_code": "INVALID_FILE_FORMAT", "message": "Format de fichier invalide. Seuls les .xlsx et .xls sont acceptés."}
        )

    try:
        file_content = await file.read()
        processed_data = process_excel_file(file_content, file.filename)
        
        # --- NOUVELLE GESTION D'ERREUR ---
        # Si le traitement n'a produit aucune feuille, on considère que c'est une erreur métier.
        if not processed_data.get("sheets"):
             return JSONResponse(
                status_code=400,
                content={"error_code": "NO_DATA_PROCESSED", "message": "Le fichier a été lu mais aucune donnée traitable n'a été trouvée."}
            )
            
        return processed_data

    except ValueError as ve:
        # Erreur métier levée par notre parser (ex: fichier corrompu)
        return JSONResponse(
            status_code=400,
            content={"error_code": "FILE_CORRUPT", "message": str(ve)}
        )
    except Exception as e:
        # Erreur interne inattendue
        traceback.print_exc()
        return JSONResponse(
            status_code=500,
            content={"error_code": "INTERNAL_SERVER_ERROR", "message": "Une erreur interne est survenue dans le service de traitement."}
        )