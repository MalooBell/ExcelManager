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
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification; // NOUVEAU
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // NOUVEAU

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.Comparator;
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
    
   
    /**
     * NOUVEAU : Méthode pour préparer l'objet Pageable pour la base de données.
     * DESCRIPTION : Spring Data JPA ne sait pas comment trier sur "data.nom".
     * Cette méthode transforme les demandes de tri sur des champs JSON en expressions
     * que la base de données peut comprendre via la fonction JSON_EXTRACT.
     */
    private Pageable convertSortForDb(Pageable pageable) {
        List<Sort.Order> newOrders = pageable.getSort().stream()
            .map(order -> {
                if (order.getProperty().startsWith("data.")) {
                    // C'est un champ JSON, on doit le transformer.
                    String jsonField = order.getProperty().substring(5);
                    // On utilise `Sort.Order.by` avec une expression que Hibernate va interpréter.
                    // CAST(... AS CHAR) est utilisé pour s'assurer que le tri est alphabétique.
                    // Pour un tri numérique, on pourrait caster en DECIMAL.
                    // Note : Ceci est dépendant de la base de données (ici, syntaxe compatible MySQL/H2)
                    String expression = String.format("JSON_UNQUOTE(JSON_EXTRACT(dataJson, '$.%s'))", jsonField);
                    return new Sort.Order(order.getDirection(), expression, order.getNullHandling());
                }
                // C'est un champ de table normal (ex: 'id'), on le laisse tel quel.
                return order;
            })
            .collect(Collectors.toList());

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(newOrders));
    }


   
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
    
    // Cette méthode de recherche globale reste à faire si nécessaire
    @Override
    public Page<RowEntityDto> search(String fileName, String keyword, Pageable pageable) {
        // La logique ici devrait aussi être migrée vers une Spécification JPA pour la cohérence.
        // Pour l'instant, on la laisse telle quelle.
        return Page.empty(); 
    }


    /**
     * MODIFICATION FINALE : Approche de tri corrigée et robuste.
     * DESCRIPTION : Le tri en base de données sur des champs JSON dynamiques étant complexe et dépendant
     * de la BDD, nous revenons à une approche de tri en mémoire, mais cette fois-ci CORRECTE.
     * 1. On récupère TOUTES les données filtrées (sans pagination).
     * 2. On les trie en mémoire.
     * 3. On applique la pagination manuellement sur la liste triée.
     * Cette approche garantit un tri et une pagination corrects, au prix d'une charge mémoire
     * plus importante si le nombre de lignes est très élevé (> 100,000), mais elle est fiable.
     */
    @Override
    public Page<RowEntityDto> searchBySheetId(Long sheetId, String keyword, Pageable pageable) {
        Specification<RowEntity> spec = (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.equal(root.get("sheet").get("id"), sheetId);
            if (StringUtils.hasText(keyword)) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(root.get("dataJson"), "%" + keyword + "%"));
            }
            return predicate;
        };

        // 1. Récupérer TOUTES les données qui correspondent au filtre, sans pagination initiale.
        List<RowEntity> allFilteredEntities = repository.findAll(spec);
        List<RowEntityDto> allDtos = allFilteredEntities.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        // 2. Trier la liste complète en mémoire si un tri est demandé.
        if (pageable.getSort().isSorted()) {
            Comparator<RowEntityDto> comparator = buildComparator(pageable.getSort());
            allDtos.sort(comparator);
        }

        // 3. Appliquer la pagination manuellement sur la liste triée.
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allDtos.size());

        List<RowEntityDto> pageContent = (start > allDtos.size()) ? List.of() : allDtos.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allDtos.size());
    }

    /**
     * NOUVEAU : Un comparateur robuste pour le tri en mémoire.
     * Gère les champs JSON et les types de données mixtes (numérique/texte).
     */
    private Comparator<RowEntityDto> buildComparator(Sort sort) {
        Comparator<RowEntityDto> comparator = null;
        for (Sort.Order order : sort) {
            Comparator<RowEntityDto> currentComparator;
            if (order.getProperty().startsWith("data.")) {
                String jsonField = order.getProperty().substring(5);
                currentComparator = Comparator.comparing(dto -> dto.getData().get(jsonField),
                        Comparator.nullsLast(this::compareValues));
            } else {
                // Tri sur des champs non-JSON (non implémenté ici mais pourrait l'être)
                continue;
            }

            if (order.isDescending()) {
                currentComparator = currentComparator.reversed();
            }

            if (comparator == null) {
                comparator = currentComparator;
            } else {
                comparator = comparator.thenComparing(currentComparator);
            }
        }
        return comparator != null ? comparator : (d1, d2) -> 0;
    }

    /**
     * NOUVEAU : Méthode de comparaison qui tente de traiter les valeurs comme des nombres d'abord.
     */
    private int compareValues(Object v1, Object v2) {
        if (v1 == null || v2 == null) return 0;
        try {
            Double d1 = Double.parseDouble(v1.toString().replace(',', '.'));
            Double d2 = Double.parseDouble(v2.toString().replace(',', '.'));
            return d1.compareTo(d2);
        } catch (NumberFormatException e) {
            // Si ce ne sont pas des nombres, on compare comme des chaînes de caractères.
            return v1.toString().compareTo(v2.toString());
        }
    }


    private RowEntityDto mapToDto(RowEntity entity) {
        try {
            RowEntityDto dto = new RowEntityDto();
            dto.setId(entity.getId());
            dto.setData(objectMapper.readValue(entity.getDataJson(), new TypeReference<Map<String, Object>>() {}));
            return dto;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON deserialization error", e);
        }
    }
}