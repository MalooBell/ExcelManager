package excel_upload_service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.dto.RowEntityDto;
import excel_upload_service.model.RowEntity;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.service.RowEntityService;
import excel_upload_service.service.ModificationHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RowEntityServiceImpl implements RowEntityService {

    private final RowEntityRepository repository;
    private final ModificationHistoryService modificationHistoryService;
    private final ObjectMapper objectMapper;

    public RowEntityServiceImpl(RowEntityRepository repository,
                                ModificationHistoryService modificationHistoryService,
                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.modificationHistoryService = modificationHistoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Page<RowEntityDto> getAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Override
    public RowEntityDto getById(Long id) {
        RowEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entité non trouvée avec l'ID: " + id));
        return mapToDto(entity);
    }

    @Override
    public RowEntityDto create(RowEntityDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto.getData());

            RowEntity entity = new RowEntity();
            entity.setSheetIndex(dto.getSheetIndex());
            entity.setDataJson(json);

            RowEntity saved = repository.save(entity);

            modificationHistoryService.saveHistory(
                    saved.getId(), "CREATE", null, json);

            return mapToDto(saved);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur lors de la sérialisation JSON", e);
        }
    }


    @Override
    public RowEntityDto update(Long id, RowEntityDto dto) {
        try {
            RowEntity entity = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Entité non trouvée avec l'ID: " + id));

            String oldJson = entity.getDataJson();
            String newJson = objectMapper.writeValueAsString(dto.getData());

            entity.setSheetIndex(dto.getSheetIndex());
            entity.setDataJson(newJson);

            RowEntity updated = repository.save(entity);

            modificationHistoryService.saveHistory(
                    id, "UPDATE", oldJson, newJson);

            return mapToDto(updated);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur lors de la sérialisation JSON", e);
        }
    }

    @Override
    public void delete(Long id) {
        RowEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entité non trouvée avec l'ID: " + id));

        modificationHistoryService.saveHistory(
                id, "DELETE", entity.getDataJson(), null);

        repository.deleteById(id);
    }

    @Override
    public Page<RowEntityDto> search(String fileName, String keyword, Pageable pageable) {
        return repository.search(fileName, keyword, pageable)
                .map(this::mapToDto);
    }

    private RowEntityDto mapToDto(RowEntity entity) {
        try {
            RowEntityDto dto = new RowEntityDto();
            dto.setId(entity.getId());
            dto.setSheetIndex(entity.getSheetIndex());
            dto.setData(objectMapper.readValue(entity.getDataJson(), Map.class));
            return dto;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur de désérialisation JSON", e);
        }
    }
}
