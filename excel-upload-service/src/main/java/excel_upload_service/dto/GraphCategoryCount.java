// Créez une nouvelle classe DTO, par exemple, dans le package dto
package excel_upload_service.dto;

public class GraphCategoryCount {
    private String category;
    private Long count;

    public GraphCategoryCount(String category, Long count) {
        this.category = category;
        this.count = count;
    }

    // Getters
    public String getCategory() { return category; }
    public Long getCount() { return count; }
}