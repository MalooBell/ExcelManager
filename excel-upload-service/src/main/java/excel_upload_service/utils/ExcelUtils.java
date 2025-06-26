package excel_upload_service.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.data.ReadCellData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap; // Use LinkedHashMap to preserve order
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ExcelUtils {

    private static final Logger logger = LoggerFactory.getLogger(ExcelUtils.class);

    public static List<String> extractHeaders(InputStream inputStream, int sheetIndex) {
        final List<String> headers = new ArrayList<>(); // Use List directly, not array

        EasyExcel.read(inputStream, new ReadListener<Map<Integer, String>>() {
            @Override
            public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
                // This method is invoked for the header row.
                if (headMap != null && !headMap.isEmpty()) {
                    // Sort keys to get headers in column order
                    List<Integer> sortedKeys = new ArrayList<>(headMap.keySet());
                    sortedKeys.sort(Integer::compareTo); // Ensure natural order of columns

                    int maxColumnIndex = Collections.max(sortedKeys);
                    String[] tempHeaders = new String[maxColumnIndex + 1]; // Use array to store by index

                    for (Map.Entry<Integer, ReadCellData<?>> entry : headMap.entrySet()) {
                        Integer colIndex = entry.getKey();
                        ReadCellData<?> cell = entry.getValue();
                        String cellValue = (cell != null) ? cell.getStringValue() : null;
                        if (colIndex <= maxColumnIndex) {
                            tempHeaders[colIndex] = (cellValue != null && !cellValue.trim().isEmpty()) ? cellValue.trim() : null;
                        }
                    }

                    // Convert array to list, trimming nulls if they are trailing
                    for (String header : tempHeaders) {
                        headers.add(header);
                    }
                    
                    // Remove trailing nulls if any (e.g., if columns beyond actual data are detected)
                    while (!headers.isEmpty() && headers.get(headers.size() - 1) == null) {
                        headers.remove(headers.size() - 1);
                    }
                }
                context.interrupt(); // Stop reading after header row
            }

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                // This method is for data rows, not relevant for header extraction after headRowNumber(0)
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {}
        }).sheet(sheetIndex).headRowNumber(0).doRead(); // Always read from row 0 for header extraction

        return headers;
    }

    // NEW METHOD: For reading actual data rows
    public static List<Map<String, String>> readExcelSheet(InputStream inputStream, int sheetIndex, int headerRowIndex) {
        List<Map<String, String>> dataList = new ArrayList<>();
        final List<String>[] headersHolder = new List[1]; // Use an array to hold headers for final/effectively final requirement

        // First, extract headers from the specified headerRowIndex
        try {
            // Important: Get a fresh InputStream for header extraction
            // The inputStream passed into readExcelSheet might be consumed by other operations.
            // If this utility is called outside a loop where streams are constantly re-opened,
            // the caller must ensure the stream is resettable or a new one is provided.
            // For now, assuming the caller (ExcelUploadServiceImpl) handles stream reopening.
            try (InputStream headerStream = inputStream) { // Use the provided stream if it's new
                headersHolder[0] = extractHeadersFromSpecificRow(headerStream, sheetIndex, headerRowIndex);
            }
        } catch (Exception e) {
            logger.error("Error extracting headers from sheet {}: {}", sheetIndex, e.getMessage(), e);
            return Collections.emptyList(); // Return empty list if headers cannot be extracted
        }

        // If no headers are found, we cannot map data correctly
        if (headersHolder[0] == null || headersHolder[0].isEmpty()) {
            logger.warn("No headers found for sheet {} at row index {}. Cannot read data.", sheetIndex, headerRowIndex);
            return Collections.emptyList();
        }

        final List<String> headers = headersHolder[0];

        EasyExcel.read(inputStream, new ReadListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> rowDataMap, AnalysisContext context) {
                int currentRowIndex = context.readRowHolder().getRowIndex() + 1; // EasyExcel is 0-indexed, convert to 1-indexed
                if (currentRowIndex <= headerRowIndex) {
                    // Skip rows before or at the header index
                    return;
                }

                Map<String, String> rowData = new LinkedHashMap<>(); // Preserve column order
                boolean isRowEmpty = true;
                for (int i = 0; i < headers.size(); i++) {
                    String header = headers.get(i);
                    String value = rowDataMap.get(i); // Data is mapped by column index

                    if (header != null && !header.trim().isEmpty()) { // Only include if header is valid
                        if (value != null && !value.trim().isEmpty()) {
                            rowData.put(header, value.trim());
                            isRowEmpty = false;
                        } else {
                            rowData.put(header, null); // Keep null for empty cells under valid headers
                        }
                    }
                }
                // Only add if the row is not entirely empty after processing
                if (!isRowEmpty) {
                    dataList.add(rowData);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                logger.debug("Finished reading sheet {} with {} data rows.", sheetIndex, dataList.size());
            }
        }).sheet(sheetIndex).headRowNumber(0).doRead(); // Always start reading from row 0, filter in invoke

        return dataList;
    }

    // Helper method to extract headers from a specific row, used by readExcelSheet
    public static List<String> extractHeadersFromSpecificRow(InputStream inputStream, int sheetIndex, int specificHeaderRowIndex) {
        final List<String> headers = new ArrayList<>();
        EasyExcel.read(inputStream, new ReadListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                // This listener is for reading regular data rows.
                // We are only interested in the row that matches specificHeaderRowIndex
                int currentRowIndex = context.readRowHolder().getRowIndex() + 1; // 1-based index
                if (currentRowIndex == specificHeaderRowIndex) {
                    // Sort keys to ensure headers are in column order
                    List<Integer> sortedKeys = new ArrayList<>(data.keySet());
                    sortedKeys.sort(Integer::compareTo);

                    int maxColumnIndex = Collections.max(sortedKeys);
                    String[] tempHeaders = new String[maxColumnIndex + 1];

                    for (Map.Entry<Integer, String> entry : data.entrySet()) {
                        Integer colIndex = entry.getKey();
                        String cellValue = entry.getValue();
                        if (colIndex <= maxColumnIndex) {
                            tempHeaders[colIndex] = (cellValue != null && !cellValue.trim().isEmpty()) ? cellValue.trim() : null;
                        }
                    }
                    
                    for (String header : tempHeaders) {
                        headers.add(header);
                    }
                    
                    // Remove trailing nulls
                    while (!headers.isEmpty() && headers.get(headers.size() - 1) == null) {
                        headers.remove(headers.size() - 1);
                    }
                    context.interrupt(); // Stop reading after the header row is found
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {}
        }).sheet(sheetIndex).headRowNumber(0).doRead(); // Start reading from row 0 to find the specific header row

        return headers;
    }
}