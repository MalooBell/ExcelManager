package excel_upload_service.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.context.AnalysisContext;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ExcelUtils {

    public static List<String> extractHeaders(InputStream inputStream, int sheetIndex) {
        final List<String>[] headers = new List[1];

        EasyExcel.read(inputStream, new ReadListener<Map<Integer, String>>() {
            boolean firstRow = true;

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                if (firstRow) {
                    headers[0] = data.values().stream().toList();
                    firstRow = false;
                    // Interrompre la lecture après la première ligne
                    context.interrupt();
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {}
        }).sheet(sheetIndex).headRowNumber(0).doRead();

        return headers[0];
    }
}