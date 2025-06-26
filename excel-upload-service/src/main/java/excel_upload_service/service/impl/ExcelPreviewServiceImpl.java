package excel_upload_service.service.impl;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import excel_upload_service.dto.SheetPreviewDto;
import excel_upload_service.model.FileEntity;
import excel_upload_service.repository.FileEntityRepository;
import excel_upload_service.service.ExcelPreviewService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile; // Vous aurez besoin de trouver un moyen de récupérer le fichier. Pour l'instant, nous allons supposer qu'il est accessible.

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelPreviewServiceImpl implements ExcelPreviewService {

    // NOTE : Pour cette fonctionnalité, nous avons besoin d'accéder au fichier physique.
    // Nous allons supposer pour l'instant qu'ils sont stockés dans un répertoire connu.
    // Une solution plus robuste utiliserait un service de stockage (S3, etc.).
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    private final FileEntityRepository fileRepository;

    public ExcelPreviewServiceImpl(FileEntityRepository fileRepository) {
        this.fileRepository = fileRepository;
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }
    
    // Cette méthode est un placeholder, vous devrez l'adapter à votre logique de stockage de fichiers.
    private InputStream getFileInputStream(Long fileId) throws IOException {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File not found with id: " + fileId));
        Path filePath = this.fileStorageLocation.resolve(fileEntity.getFileName()).normalize();
        return new FileInputStream(filePath.toFile());
    }


    @Override
    public SheetPreviewDto getSheetPreview(Long fileId, int sheetIndex, int rowLimit) throws IOException {
        PreviewListener listener = new PreviewListener(rowLimit);
        try (InputStream inputStream = getFileInputStream(fileId)) {
             try {
                EasyExcel.read(inputStream, listener).sheet(sheetIndex).headRowNumber(0).doRead();
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("interrupt")) {
                    // C'est normal, le listener interrompt la lecture.
                } else {
                    throw e;
                }
            }
        }
        return new SheetPreviewDto(listener.getRows());
    }

    private static class PreviewListener implements ReadListener<Map<Integer, String>> {
        private final int rowLimit;
        private final List<List<String>> rows = new ArrayList<>();

        public PreviewListener(int rowLimit) {
            this.rowLimit = rowLimit;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            List<String> rowData = new ArrayList<>();
            // S'assurer qu'on a bien toutes les colonnes, même les vides, jusqu'à la dernière cellule non-nulle
            int maxCol = data.keySet().stream().max(Integer::compare).orElse(-1);
            for (int i = 0; i <= maxCol; i++) {
                rowData.add(data.getOrDefault(i, ""));
            }
            rows.add(rowData);
            if (rows.size() >= rowLimit) {
                context.interrupt();
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {}

        public List<List<String>> getRows() {
            return rows;
        }
    }

    
}
