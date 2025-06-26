// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/impl/RowEntityServiceImpl.java
package excel_upload_service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.dto.RowEntityDto;
import excel_upload_service.model.RowEntity;
import excel_upload_service.model.SheetEntity;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.repository.SheetEntityRepository;
import excel_upload_service.service.ModificationHistoryService;
import excel_upload_service.service.RowEntityService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RowEntityServiceImpl implements RowEntityService {

    private final RowEntityRepository repository;
    private final SheetEntityRepository sheetRepository;
    private final ModificationHistoryService modificationHistoryService;
    private final ObjectMapper objectMapper;

    public RowEntityServiceImpl(RowEntityRepository repository,
                                SheetEntityRepository sheetRepository,
                                ModificationHistoryService modificationHistoryService,
                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.sheetRepository = sheetRepository;
        this.modificationHistoryService = modificationHistoryService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Page<RowEntityDto> searchBySheetId(Long sheetId, String keyword, Pageable pageable) {
        // CORRECTION 1 : On ne peut pas utiliser "Sort.and(Sort.Order)".
        // On doit collecter les "Order" dans des listes séparées.
        List<Sort.Order> dbOrders = new ArrayList<>();
        List<Sort.Order> jsonOrders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {
            if (order.getProperty().startsWith("data.")) {
                jsonOrders.add(order);
            } else {
                dbOrders.add(order);
            }
        }

        // On crée les objets Sort à partir des listes d'ordres.
        Sort dbSort = Sort.by(dbOrders);
        Sort jsonSort = Sort.by(jsonOrders);

        // 1. On interroge la base de données avec la pagination et le tri qu'elle peut gérer.
        Pageable dbPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), dbSort);
        Page<RowEntity> page = repository.searchBySheetIdAndKeyword(sheetId, keyword, dbPageable);
        
        // 2. On transforme les entités en DTOs.
        List<RowEntityDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        // 3. Si un tri sur un champ JSON a été demandé, on l'effectue en mémoire ici.
        if (jsonSort.isSorted()) {
            Comparator<RowEntityDto> finalComparator = buildJsonComparator(jsonSort);
            if (finalComparator != null) {
                dtos.sort(finalComparator);
            }
        }
        
        // 4. On retourne la page avec les données triées.
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    /**
     * Construit un comparateur composite pour trier une liste de RowEntityDto
     * en fonction des ordres de tri spécifiés pour les champs JSON.
     */
    private Comparator<RowEntityDto> buildJsonComparator(Sort jsonSort) {
        Comparator<RowEntityDto> finalComparator = null;

        for (Sort.Order order : jsonSort) {
            String jsonKey = order.getProperty().substring(5); // Enlève "data."

            // CORRECTION 2 : On crée un comparateur qui gère explicitement les types mixtes (nombres et textes).
            Comparator<RowEntityDto> currentComparator = (dto1, dto2) -> {
                Object value1 = dto1.getData().get(jsonKey);
                Object value2 = dto2.getData().get(jsonKey);

                // Gestion des nuls
                if (value1 == null && value2 == null) return 0;
                if (value1 == null) return 1; // Les nuls sont placés à la fin
                if (value2 == null) return -1;

                // Tentative de comparaison numérique
                try {
                    Double num1 = Double.parseDouble(value1.toString().replace(',', '.'));
                    Double num2 = Double.parseDouble(value2.toString().replace(',', '.'));
                    return num1.compareTo(num2);
                } catch (NumberFormatException e) {
                    // Si ce ne sont pas des nombres, on compare comme des chaînes de caractères
                    return value1.toString().compareTo(value2.toString());
                }
            };
            
            if (order.isDescending()) {
                currentComparator = currentComparator.reversed();
            }

            if (finalComparator == null) {
                finalComparator = currentComparator;
            } else {
                finalComparator = finalComparator.thenComparing(currentComparator);
            }
        }
        return finalComparator;
    }

    private RowEntityDto mapToDto(RowEntity entity) {
        try {
            RowEntityDto dto = new RowEntityDto();
            dto.setId(entity.getId());
            dto.setSheetIndex(entity.getSheet().getSheetIndex());
            dto.setData(objectMapper.readValue(entity.getDataJson(), new TypeReference<Map<String, Object>>() {}));
            return dto;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON deserialization error", e);
        }
    }
    
    // Le reste du fichier est inchangé
    @Override
    public RowEntityDto getById(Long id) {
        RowEntity entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + id));
        return mapToDto(entity);
    }
    
    @Override
    public RowEntityDto create(Long sheetId, RowEntityDto dto) {
        SheetEntity sheetEntity = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new EntityNotFoundException("Sheet not found with ID: " + sheetId));
        try {
            String json = objectMapper.writeValueAsString(dto.getData());
            RowEntity entity = new RowEntity();
            entity.setDataJson(json);
            entity.setSheet(sheetEntity);
            RowEntity saved = repository.save(entity);
            modificationHistoryService.saveHistory(saved.getId(), "CREATE", null, json);
            return mapToDto(saved);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during JSON serialization", e);
        }
    }

    @Override
    public RowEntityDto update(Long id, RowEntityDto dto) {
        try {
            RowEntity entity = repository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + id));
            String oldJson = entity.getDataJson();
            String newJson = objectMapper.writeValueAsString(dto.getData());
            entity.setDataJson(newJson);
            RowEntity updated = repository.save(entity);
            modificationHistoryService.saveHistory(id, "UPDATE", oldJson, newJson);
            return mapToDto(updated);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during JSON serialization", e);
        }
    }

    @Override
    public void delete(Long id) {
        RowEntity entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + id));
        modificationHistoryService.saveHistory(id, "DELETE", entity.getDataJson(), null);
        repository.deleteById(id);
    }
    
    @Override
    public Page<RowEntityDto> search(String fileName, String keyword, Pageable pageable) {
        return repository.searchWithFileAndKeyword(fileName, keyword, pageable).map(this::mapToDto);
    }
}