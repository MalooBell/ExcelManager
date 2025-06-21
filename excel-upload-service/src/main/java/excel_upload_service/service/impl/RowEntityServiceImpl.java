package excel_upload_service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.dto.RowEntityDto;
import excel_upload_service.model.FileEntity;
import excel_upload_service.model.RowEntity;
import excel_upload_service.repository.FileEntityRepository;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.service.ModificationHistoryService;
import excel_upload_service.service.RowEntityService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RowEntityServiceImpl implements RowEntityService {

    private final RowEntityRepository repository;
    private final FileEntityRepository fileRepository; // Inject File repository
    private final ModificationHistoryService modificationHistoryService;
    private final ObjectMapper objectMapper;

    public RowEntityServiceImpl(RowEntityRepository repository,
                                FileEntityRepository fileRepository,
                                ModificationHistoryService modificationHistoryService,
                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.fileRepository = fileRepository;
        this.modificationHistoryService = modificationHistoryService;
        this.objectMapper = objectMapper;
    }

    // This method now serves as a global search.
    @Override
    public Page<RowEntityDto> getAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Override
    public RowEntityDto getById(Long id) {
        RowEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found with ID: " + id));
        return mapToDto(entity);
    }

    @Override
    public RowEntityDto create(Long fileId, RowEntityDto dto) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));

        try {
            String json = objectMapper.writeValueAsString(dto.getData());

            RowEntity entity = new RowEntity();
            entity.setSheetIndex(dto.getSheetIndex());
            entity.setDataJson(json);
            entity.setFile(fileEntity); // Associate with the file

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
                    .orElseThrow(() -> new RuntimeException("Entity not found with ID: " + id));

            String oldJson = entity.getDataJson();
            String newJson = objectMapper.writeValueAsString(dto.getData());

            entity.setSheetIndex(dto.getSheetIndex());
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
                .orElseThrow(() -> new RuntimeException("Entity not found with ID: " + id));
        modificationHistoryService.saveHistory(id, "DELETE", entity.getDataJson(), null);
        repository.deleteById(id);
    }

    @Override
    public Page<RowEntityDto> search(String fileName, String keyword, Pageable pageable) {
        return repository.searchWithFileAndKeyword(fileName, keyword, pageable).map(this::mapToDto);
    }

    @Override
    public Page<RowEntityDto> searchByFileId(Long fileId, String keyword, Pageable pageable) {
        // Separate pageable for DB query and for in-memory sort
        Sort jsonSort = null;
        Sort dbSort = Sort.unsorted();

        for (Sort.Order order : pageable.getSort()) {
            if (order.getProperty().startsWith("data.")) {
                jsonSort = Sort.by(order);
            } else {
                dbSort = dbSort.and(Sort.by(order));
            }
        }

        Pageable dbPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), dbSort);

        Page<RowEntity> page = repository.searchByFileIdAndKeyword(fileId, keyword, dbPageable);
        List<RowEntityDto> dtos = page.getContent().stream().map(this::mapToDto).collect(Collectors.toList());

        // --- In-memory sorting for JSON fields ---
        if (jsonSort != null) {
            Sort.Order order = jsonSort.iterator().next();
            String jsonKey = order.getProperty().substring(5); // remove "data."

            Comparator<RowEntityDto> comparator = Comparator.comparing(
                    dto -> {
                        Object value = dto.getData().get(jsonKey);
                        if (value instanceof Comparable) {
                            return ((Comparable) value).toString();
                        }
                        return value != null ? value.toString() : "";
                    },
                    Comparator.nullsLast(Comparator.naturalOrder())
            );

            if (order.isDescending()) {
                comparator = comparator.reversed();
            }
            dtos.sort(comparator);
        }

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    private RowEntityDto mapToDto(RowEntity entity) {
        try {
            RowEntityDto dto = new RowEntityDto();
            dto.setId(entity.getId());
            dto.setSheetIndex(entity.getSheetIndex());
            dto.setData(objectMapper.readValue(entity.getDataJson(), new TypeReference<Map<String, Object>>() {}));
            // Optionally add file info to the DTO if needed by the frontend
            // dto.setFileId(entity.getFile().getId());
            // dto.setFileName(entity.getFile().getFileName());
            return dto;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON deserialization error", e);
        }
    }
}