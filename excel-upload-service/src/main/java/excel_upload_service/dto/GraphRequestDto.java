package excel_upload_service.dto;

import java.util.List;

public class GraphRequestDto {
    private String chartType; // "pie", "bar"
    private String categoryColumn; // X-axis for bar, labels for pie
    private List<String> valueColumns; // Y-axis for bar
    private String aggregationType; // "COUNT", "SUM"
    // Add other options like aggregation type (count, sum, avg) if needed

    // Getters and Setters
    public String getChartType() { return chartType; }
    public void setChartType(String chartType) { this.chartType = chartType; }
    public String getCategoryColumn() { return categoryColumn; }
    public void setCategoryColumn(String categoryColumn) { this.categoryColumn = categoryColumn; }
    public List<String> getValueColumns() { return valueColumns; }
    public void setValueColumns(List<String> valueColumns) { this.valueColumns = valueColumns; }
    public String getAggregationType() { return aggregationType; }
    public void setAggregationType(String aggregationType) { this.aggregationType = aggregationType; }
}