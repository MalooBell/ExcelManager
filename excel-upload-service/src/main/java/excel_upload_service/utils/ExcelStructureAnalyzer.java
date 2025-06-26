// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/utils/ExcelStructureAnalyzer.java
package excel_upload_service.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.enums.CellExtraTypeEnum; // Import direct pour plus de clarté
import com.alibaba.excel.metadata.CellExtra;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelStructureAnalyzer {

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

    public static ExcelStructure analyze(MultipartFile file, int sheetIndex) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            StructureListener listener = new StructureListener();
            EasyExcel.read(inputStream, listener)
                    .sheet(sheetIndex)
                    .headRowNumber(0) 
                    .doRead();
            return listener.getStructure();
        }
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