package excel_upload_service.dto;

import java.util.Map;

public class RowEntityDto {
    private Long id;
    private int sheetIndex;
    private Map<String, Object> data;

    public RowEntityDto() {
    }

    public RowEntityDto(Long id, int sheetIndex, Map<String, Object> data) {
        this.id = id;
        this.sheetIndex = sheetIndex;
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getSheetIndex() {
        return sheetIndex;
    }

    public void setSheetIndex(int sheetIndex) {
        this.sheetIndex = sheetIndex;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "RowEntityDto{" +
                "id=" + id +
                ", sheetIndex=" + sheetIndex +
                ", data=" + data +
                '}';
    }
}
