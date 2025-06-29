import pika
import time
import json
import os
import pandas as pd
import requests # Bibliothèque pour faire des appels HTTP

# ... (les fonctions de parsing process_excel_file et find_header_and_data ne changent pas) ...
def find_header_and_data(df: pd.DataFrame):
    non_empty_counts = df.notna().sum(axis=1)
    non_empty_counts = non_empty_counts[non_empty_counts > 1]
    if non_empty_counts.empty:
        return 0, pd.DataFrame()
    header_row_index = non_empty_counts.idxmax()
    new_columns = df.iloc[header_row_index].astype(str)
    data_df = df.iloc[header_row_index + 1:].copy()
    data_df.columns = new_columns
    data_df.reset_index(drop=True, inplace=True)
    return header_row_index, data_df

# NOUVELLE FONCTION pour deviner le type de données d'une colonne
def infer_data_type(series: pd.Series) -> str:
    """
    Analyse une série Pandas et retourne le type de données le plus probable.
    """
    # Supprime les valeurs nulles pour ne pas fausser l'analyse
    series = series.dropna()
    if series.empty:
        return "VARCHAR(255)" # Par défaut, si la colonne est vide

    # Essai de conversion en numérique (entier puis flottant)
    try:
        if (series.astype(int) == series).all():
            return "INTEGER"
    except (ValueError, TypeError):
        try:
            series.astype(float)
            return "DECIMAL(18, 4)" # Un type décimal pour la précision
        except (ValueError, TypeError):
            pass # Continue si ce n'est pas un nombre

    # Essai de conversion en datetime
    try:
        pd.to_datetime(series, errors='raise')
        return "DATETIME"
    except (ValueError, TypeError):
        pass

    # Si tout le reste échoue, c'est probably du texte.
    # On peut ajuster la taille maximale si nécessaire.
    return "VARCHAR(255)"


# --- METTRE À JOUR LA FONCTION process_excel_file ---
def process_excel_file(file_path: str, file_name: str):
    try:
        xls = pd.ExcelFile(file_path, engine='openpyxl')
    except Exception as e:
        raise ValueError(f"Impossible de lire le fichier: {e}")

    processed_sheets = []
    for sheet_name in xls.sheet_names:
        df_raw = pd.read_excel(xls, sheet_name=sheet_name, header=None)
        df_filled = df_raw.ffill()
        header_index, df_data = find_header_and_data(df_filled)
        
        if df_data.empty:
            continue

        # --- DÉBUT DE LA MODIFICATION ---
        
        # Création du schéma enrichi avec les types de données
        schema = []
        for col_name in df_data.columns:
            inferred_type = infer_data_type(df_data[col_name])
            schema.append({"name": str(col_name), "type": inferred_type})
        
        # --- FIN DE LA MODIFICATION ---

        df_data = df_data.where(pd.notna(df_data), None)
        data_records = df_data.to_dict(orient='records')
        
        processed_sheets.append({
            "sheet_name": sheet_name,
            "schema": schema, # On envoie le nouveau schéma enrichi
            "data": data_records,
            "total_rows": len(data_records)
        })

    return { "file_name": file_name, "sheets": processed_sheets }


# --- Logique du Worker RabbitMQ MISE À JOUR ---

# URL de l'API Java (à configurer proprement dans un fichier de config en production)
JAVA_API_BASE_URL = "http://localhost:8080" 

def submit_data_to_java(file_id: int, data: dict):
    """
    Soumet les données traitées à l'API Java.
    """
    url = f"{JAVA_API_BASE_URL}/api/internal/files/{file_id}/processed-data"
    try:
        # NOTE: Il faudrait passer le token JWT ici pour sécuriser l'API interne
        # headers = {'Authorization': 'Bearer <token_service_interne>'}
        response = requests.post(url, json=data)
        response.raise_for_status()  # Lève une exception si le statut est 4xx ou 5xx
        print(f" [ok] Données pour le fichier {file_id} soumises avec succès à Java.")
        return True
    except requests.exceptions.RequestException as e:
        print(f" [!] Échec de la soumission des données à Java pour le fichier {file_id}: {e}")
        return False

def main():
    # Définir les identifiants de connexion
    credentials = pika.PlainCredentials('user', 'password')
    connection = None
    
    # Boucle de tentative de connexion à RabbitMQ
    while not connection:
        try:
            # Utiliser les identifiants et le nom d'hôte correct ('rabbitmq' est le nom du service dans docker-compose)
            connection = pika.BlockingConnection(
                pika.ConnectionParameters(host='localhost', credentials=credentials)
            )
            print("Connecté à RabbitMQ avec succès.")
        except pika.exceptions.AMQPConnectionError:
            print("Connexion à RabbitMQ échouée. Nouvelle tentative dans 5 secondes...")
            time.sleep(5)

    channel = connection.channel()
    queue_name = 'excel-processing-queue'
    channel.queue_declare(queue=queue_name, durable=True)

    def callback(ch, method, properties, body):
        message_data = json.loads(body)
        file_id = message_data.get("fileId")
        stored_filename = message_data.get("storedFilename")
        print(f" [x] Traitement du fichier ID: {file_id}, Nom: {stored_filename}")

        try:
            # Le chemin d'accès au fichier est relatif au conteneur du worker
            file_path = os.path.join("..", "excel-upload-service", "file-storage", stored_filename)
            processed_data = process_excel_file(file_path, message_data.get("originalFilename"))
            
            if submit_data_to_java(file_id, processed_data):
                ch.basic_ack(delivery_tag=method.delivery_tag)
                # On peut supprimer le fichier temporaire après succès
                try:
                    os.remove(file_path)
                    print(f" [ok] Fichier temporaire {stored_filename} supprimé.")
                except OSError as e:
                    print(f" [!] Erreur lors de la suppression du fichier temporaire: {e}")
            else:
                # Si la soumission échoue, on ne retire pas le message de la file pour qu'il soit retraité plus tard
                ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
                
        except Exception as e:
            print(f" [!] Erreur critique lors du traitement du fichier {file_id}: {e}")
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

    channel.basic_consume(queue=queue_name, on_message_callback=callback)
    print(' [*] Worker Python démarré. En attente de tâches de traitement...')
    channel.start_consuming()

if __name__ == '__main__':
    main()
