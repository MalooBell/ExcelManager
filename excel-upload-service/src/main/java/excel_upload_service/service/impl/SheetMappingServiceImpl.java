// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/impl/SheetMappingServiceImpl.java
package excel_upload_service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.dto.SheetMappingDto;
import excel_upload_service.model.SheetEntity;
import excel_upload_service.model.SheetMapping;
import excel_upload_service.model.SheetMappingTemplate;
import excel_upload_service.repository.SheetEntityRepository;
import excel_upload_service.repository.SheetMappingRepository;
import excel_upload_service.repository.SheetMappingTemplateRepository;
import excel_upload_service.service.SheetMappingService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * NOUVEAU : Implémentation du service de mapping.
 */
@Service
public class SheetMappingServiceImpl implements SheetMappingService {

    private final SheetMappingRepository sheetMappingRepository;
    private final SheetEntityRepository sheetEntityRepository;
    private final ObjectMapper objectMapper;
    private final SheetMappingTemplateRepository templateRepository;

    public SheetMappingServiceImpl(SheetMappingRepository sheetMappingRepository,
                                   SheetEntityRepository sheetEntityRepository,
                                   SheetMappingTemplateRepository templateRepository,
                                   ObjectMapper objectMapper) {
        this.sheetMappingRepository = sheetMappingRepository;
        this.sheetEntityRepository = sheetEntityRepository;
        this.templateRepository = templateRepository; // NOUVEAU
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public SheetMappingDto applyTemplateToSheet(Long sheetId, Long templateId) {
        // 1. Valider que la feuille et le modèle existent.
        SheetEntity sheet = sheetEntityRepository.findById(sheetId)
                .orElseThrow(() -> new EntityNotFoundException("Sheet not found with ID: " + sheetId));
        SheetMappingTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("Template not found with ID: " + templateId));

        // 2. Récupérer le mapping existant pour la feuille ou en créer un nouveau.
        SheetMapping sheetMapping = sheetMappingRepository.findBySheetId(sheetId)
                .orElse(new SheetMapping());
        
        sheetMapping.setSheet(sheet);
        // 3. Copier la définition JSON du modèle vers le mapping de la feuille.
        sheetMapping.setMappingDefinitionJson(template.getMappingDefinitionJson());
        
        sheetMappingRepository.save(sheetMapping);
        
        return convertToDto(sheetMapping);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SheetMappingDto> getMappingBySheetId(Long sheetId) {
        return sheetMappingRepository.findBySheetId(sheetId)
                .map(this::convertToDto); // Utilise une référence de méthode pour convertir l'entité en DTO
    }

    @Override
    @Transactional
    public SheetMappingDto createOrUpdateMapping(Long sheetId, SheetMappingDto mappingDto) {
        // Trouve la feuille correspondante, sinon lance une exception.
        SheetEntity sheet = sheetEntityRepository.findById(sheetId)
                .orElseThrow(() -> new EntityNotFoundException("Sheet not found with ID: " + sheetId));

        // Cherche un mapping existant ou en crée un nouveau.
        SheetMapping sheetMapping = sheetMappingRepository.findBySheetId(sheetId)
                .orElse(new SheetMapping());

        sheetMapping.setSheet(sheet);
        try {
            // Sérialise le DTO en une chaîne de caractères JSON pour le stockage.
            String jsonDefinition = objectMapper.writeValueAsString(mappingDto);
            sheetMapping.setMappingDefinitionJson(jsonDefinition);
        } catch (JsonProcessingException e) {
            // En cas d'erreur de sérialisation, lance une exception non contrôlée.
            throw new RuntimeException("Error serializing mapping DTO to JSON", e);
        }

        // Sauvegarde l'entité en base de données.
        SheetMapping savedMapping = sheetMappingRepository.save(sheetMapping);
        return convertToDto(savedMapping);
    }

    /**
     * Méthode privée pour convertir une entité SheetMapping en son DTO.
     * @param entity L'entité à convertir.
     * @return Le DTO correspondant.
     */
    private SheetMappingDto convertToDto(SheetMapping entity) {
        try {
            // Désérialise la chaîne JSON de la base de données en un objet DTO.
            return objectMapper.readValue(entity.getMappingDefinitionJson(), SheetMappingDto.class);
        } catch (JsonProcessingException e) {
            // En cas d'erreur, lance une exception.
            throw new RuntimeException("Error deserializing mapping JSON to DTO", e);
        }
    }
}