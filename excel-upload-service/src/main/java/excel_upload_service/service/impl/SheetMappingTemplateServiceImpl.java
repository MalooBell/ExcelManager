// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/impl/SheetMappingTemplateServiceImpl.java
package excel_upload_service.service.impl;

import excel_upload_service.dto.SheetMappingTemplateDto;
import excel_upload_service.model.SheetMappingTemplate;
import excel_upload_service.repository.SheetMappingTemplateRepository;
import excel_upload_service.service.SheetMappingTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * NOUVEAU : Implémentation du service pour les modèles de mapping.
 */
@Service
public class SheetMappingTemplateServiceImpl implements SheetMappingTemplateService {

    private final SheetMappingTemplateRepository templateRepository;

    public SheetMappingTemplateServiceImpl(SheetMappingTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SheetMappingTemplateDto> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SheetMappingTemplateDto> getTemplateById(Long id) {
        return templateRepository.findById(id).map(this::convertToDto);
    }

    @Override
    @Transactional
    public SheetMappingTemplateDto createTemplate(SheetMappingTemplateDto templateDto) {
        SheetMappingTemplate template = new SheetMappingTemplate();
        template.setName(templateDto.getName());
        template.setDescription(templateDto.getDescription());
        template.setMappingDefinitionJson(templateDto.getMappingDefinitionJson());
        
        SheetMappingTemplate saved = templateRepository.save(template);
        return convertToDto(saved);
    }

    @Override
    @Transactional
    public Optional<SheetMappingTemplateDto> updateTemplate(Long id, SheetMappingTemplateDto templateDto) {
        return templateRepository.findById(id)
                .map(existingTemplate -> {
                    existingTemplate.setName(templateDto.getName());
                    existingTemplate.setDescription(templateDto.getDescription());
                    existingTemplate.setMappingDefinitionJson(templateDto.getMappingDefinitionJson());
                    SheetMappingTemplate updated = templateRepository.save(existingTemplate);
                    return convertToDto(updated);
                });
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        if (templateRepository.existsById(id)) {
            templateRepository.deleteById(id);
        }
    }

    /**
     * Méthode de conversion d'une entité en DTO.
     */
    private SheetMappingTemplateDto convertToDto(SheetMappingTemplate template) {
        SheetMappingTemplateDto dto = new SheetMappingTemplateDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        // Pas besoin de re-parser le JSON, on le passe tel quel.
        dto.setMappingDefinitionJson(template.getMappingDefinitionJson());
        return dto;
    }
}