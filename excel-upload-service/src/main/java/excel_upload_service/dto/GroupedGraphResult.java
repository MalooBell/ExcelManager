package excel_upload_service.dto;

import java.math.BigDecimal;

public interface GroupedGraphResult {
    String getPrimaryCategory();
    String getSecondaryCategory();
    BigDecimal getValue();
}