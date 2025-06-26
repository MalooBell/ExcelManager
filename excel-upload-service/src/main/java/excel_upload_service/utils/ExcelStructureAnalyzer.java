// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/utils/ExcelStructureAnalyzer.java
package excel_upload_service.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.enums.CellExtraTypeEnum; // Import direct pour plus de clarté
import com.alibaba.excel.metadata.CellExtra;
import com.alibaba.excel.read.listener.ReadListener;
import java.io.InputStream;
import excel_upload_service.dto.LayoutAnalysis;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExcelStructureAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ExcelStructureAnalyzer.class);
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");


    @Getter
    public static class ExcelStructure {
        private final List<Map<Integer, String>> headerRows = new ArrayList<>();
        private final Map<String, MergedRegion> mergedRegions = new HashMap<>();
        private int dataStartRow = -1;
    }

    @Getter
    public static class MergedRegion {
        private final int firstRow;
        private final int lastRow;
        private final int firstCol;
        private final int lastCol;

        public MergedRegion(int firstRow, int lastRow, int firstCol, int lastCol) {
            this.firstRow = firstRow;
            this.lastRow = lastRow;
            this.firstCol = firstCol;
            this.lastCol = lastCol;
        }
    }

    public LayoutAnalysis analyze(InputStream inputStream, int sheetIndex) throws IOException { // <--- MODIFIED SIGNATURE
    // --- END MODIFICATION FOR ANALYZE METHOD ---
        StructureListener listener = new StructureListener();
        try {
            EasyExcel.read(inputStream, listener).sheet(sheetIndex).headRowNumber(0).doRead();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("interrupt")) {
                logger.debug("Layout detection read interrupted as expected after hitting row limit.");
            } else {
                logger.error("Error during layout detection for sheet index {}: {}", sheetIndex, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
        
        List<Map<Integer, String>> headRows = listener.getStructure().getHeaderRows();
        int bestScore = -100;
        int bestHeaderRowIndex = -1; 

        for (int i = 0; i < headRows.size(); i++) {
            Map<Integer, String> row = headRows.get(i);
            if (row == null || row.values().stream().allMatch(v -> v == null || v.trim().isEmpty())) {
                continue; 
            }
            int score = 0;
            Collection<String> values = row.values().stream().filter(v -> v != null && !v.trim().isEmpty()).collect(Collectors.toList());
            long nonEmptyCells = values.size();
            
            if (nonEmptyCells < 2) {
                score -= 20;
            } else {
                long distinctValues = values.stream().distinct().count();
                if (distinctValues == nonEmptyCells) {
                    score += 10;
                }
                
                long numericCount = values.stream().filter(v -> v != null && NUMERIC_PATTERN.matcher(v).matches()).count();
                if (numericCount > nonEmptyCells / 2) { 
                    score -= 15;
                } else if (numericCount <= nonEmptyCells / 3) { 
                    score += 5;
                }

                if (i + 3 < headRows.size()) {
                    Set<String> nextThreeRowsValues = new HashSet<>();
                    for (int j = i + 1; j <= Math.min(i + 3, headRows.size() -1); j++) {
                        headRows.get(j).values().stream()
                            .filter(v -> v != null && !v.trim().isEmpty())
                            .forEach(nextThreeRowsValues::add);
                    }
                    if (Collections.disjoint(values, nextThreeRowsValues) && !nextThreeRowsValues.isEmpty()) {
                        score += 15;
                    }
                }
            }
            logger.debug("Layout detection for sheet index {}: Row {} score: {}", sheetIndex, (i + 1), score);
            if (score > bestScore) {
                bestScore = score;
                bestHeaderRowIndex = i + 1;
            }
        }
        if (bestScore <= 0 && bestHeaderRowIndex != -1) {
            return new LayoutAnalysis(-1, false);
        }
        return new LayoutAnalysis(bestHeaderRowIndex, bestHeaderRowIndex != -1);
    }

    private static class StructureListener implements ReadListener<Map<Integer, String>> {
        private final ExcelStructure structure = new ExcelStructure();
        private static final int MAX_HEADER_ROWS_TO_CHECK = 10; 

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            int currentRow = context.readRowHolder().getRowIndex();

            if (structure.dataStartRow == -1 && !isRowEmpty(data)) {
                structure.dataStartRow = currentRow;
            }
            
            if (structure.dataStartRow != -1 && (currentRow - structure.dataStartRow) < MAX_HEADER_ROWS_TO_CHECK) {
                structure.headerRows.add(new HashMap<>(data));
            }
        }
        
        @Override
        public void extra(CellExtra extra, AnalysisContext context) {
            // CORRECTION FINALE : Utiliser CellExtraTypeEnum.MERGE
            if (extra.getType() == CellExtraTypeEnum.MERGE) {
                String key = extra.getFirstRowIndex() + "-" + extra.getFirstColumnIndex();
                structure.mergedRegions.put(key,
                        new MergedRegion(extra.getFirstRowIndex(), extra.getLastRowIndex(),
                                extra.getFirstColumnIndex(), extra.getLastColumnIndex()));
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // Pas de changement
        }
        
        private boolean isRowEmpty(Map<Integer, String> data) {
            if (data == null || data.isEmpty()) return true;
            return data.values().stream().allMatch(cell -> cell == null || cell.trim().isEmpty());
        }

        public ExcelStructure getStructure() {
            return structure;
        }
    }
}