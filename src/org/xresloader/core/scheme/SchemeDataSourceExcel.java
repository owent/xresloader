package org.xresloader.core.scheme;

import java.io.File;
import java.util.ArrayList;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.err.InitializeException;
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcExcel;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.engine.IdentifyDescriptor;

/**
 * Created by owentou on 2014/9/30.
 */
public final class SchemeDataSourceExcel extends SchemeDataSourceBase {

    private File currentWorkbookFile = null;

    @Override
    public int load() throws InitializeException {

        String filePath = ProgramOptions.getInstance().dataSourceFile;
        currentWorkbookFile = new File(filePath);
        if (!currentWorkbookFile.exists() || !currentWorkbookFile.isFile()) {
            throw new InitializeException(
                    String.format("open file \"%s\" failed", ProgramOptions.getInstance().dataSourceFile));
        }

        return 0;
    }

    @Override
    @Deprecated
    public boolean load_scheme(String sheetName) throws InitializeException {
        // 数据及header
        int dataRow = 0;
        int keyCol = 0; // 字段列
        int dataCol[] = { 2, 3, 4 }; // 数据列

        SchemeConf.DataInfo dataInfo = new SchemeConf.DataInfo();
        dataInfo.filePath = currentWorkbookFile.getPath();
        dataInfo.tableName = sheetName;

        ExcelEngine.DataSheetWrapper sheetWrapper = DataSrcExcel.createDataSheetWrapper(dataInfo,
                false);

        int rowNum = sheetWrapper.getLastRowIndex() + 1;

        // 查找“字段”列
        for (; dataRow < rowNum; ++dataRow) {
            ExcelEngine.DataItemGridWrapper itemGrid = DataSrcExcel.createDataExcelGridWrapper(sheetWrapper, false,
                    dataRow);
            int colNum = itemGrid.getLastDataFieldIndex() + 1;
            for (keyCol = 0; keyCol < colNum; ++keyCol) {
                String val = cell2str(itemGrid, keyCol);
                val = val.trim();
                if (val.equals("字段") || val.equalsIgnoreCase("header")) {
                    break;
                }
            }

            if (keyCol >= colNum) {
                continue;
            }

            // 数据列判定
            for (int i = 0; i < colNum; ++i) {
                String val = cell2str(itemGrid, i);
                val = val.trim();
                if (val.equals("主配置") || val.equalsIgnoreCase("major")) {
                    dataCol[0] = i;
                } else if (val.equals("次配置") || val.equalsIgnoreCase("minor")) {
                    dataCol[1] = i;
                } else if (val.equals("补充配置") || val.equalsIgnoreCase("addition")) {
                    dataCol[2] = i;
                }
            }
            break;
        }

        if (dataRow >= rowNum) {
            throw new InitializeException(
                    String.format("scheme \"%s\" has no configure, or it's not a scheme sheet?", sheetName));
        }

        // 数据项必须在这之后
        for (++dataRow; dataRow < rowNum; ++dataRow) {
            ExcelEngine.DataItemGridWrapper itemGrid = DataSrcExcel.createDataExcelGridWrapper(sheetWrapper, false,
                    dataRow);

            String key = cell2str(itemGrid, keyCol);
            ArrayList<String> datas = new ArrayList<>();
            datas.add(cell2str(itemGrid, dataCol[0]));
            datas.add(cell2str(itemGrid, dataCol[1]));
            datas.add(cell2str(itemGrid, dataCol[2]));

            setScheme(key, datas);
        }

        return true;
    }

    private String cell2str(ExcelEngine.DataItemGridWrapper itemGrid, int column) throws InitializeException {
        IdentifyDescriptor ident = new IdentifyDescriptor(column);

        DataContainer<String> cache = DataSrcImpl.getStringCache("");
        try {
            ExcelEngine.cell2s(cache, itemGrid, ident, null);
        } catch (ConvException ex) {
            throw new InitializeException("convert excel cell to string failed", ex);
        }
        return cache.get();
    }
}
