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

        String file_path = ProgramOptions.getInstance().dataSourceFile;
        currentWorkbookFile = new File(file_path);
        if (!currentWorkbookFile.exists() || !currentWorkbookFile.isFile()) {
            throw new InitializeException(
                    String.format("open file \"%s\" failed", ProgramOptions.getInstance().dataSourceFile));
        }

        return 0;
    }

    @Override
    public boolean load_scheme(String sheetName) throws InitializeException {
        // 数据及header
        int data_row = 0;
        int key_col = 0; // 字段列
        int data_col[] = { 2, 3, 4 }; // 数据列

        SchemeConf.DataInfo dataInfo = new SchemeConf.DataInfo();
        dataInfo.filePath = currentWorkbookFile.getPath();
        dataInfo.tableName = sheetName;

        ExcelEngine.DataSheetWrapper sheetWrapper = DataSrcExcel.createDataSheetWrapper(dataInfo,
                false);

        int row_num = sheetWrapper.getLastRowIndex() + 1;

        // 查找“字段”列
        for (; data_row < row_num; ++data_row) {
            ExcelEngine.DataItemGridWrapper itemGrid = DataSrcExcel.createDataExcelGridWrapper(sheetWrapper, false,
                    data_row);
            int col_num = itemGrid.getLastDataFieldIndex() + 1;
            for (key_col = 0; key_col < col_num; ++key_col) {
                String val = cell2str(itemGrid, key_col);
                val = val.trim();
                if (val.equals("字段") || val.equalsIgnoreCase("header")) {
                    break;
                }
            }

            if (key_col >= col_num) {
                continue;
            }

            // 数据列判定
            for (int i = 0; i < col_num; ++i) {
                String val = cell2str(itemGrid, i);
                val = val.trim();
                if (val.equals("主配置") || val.equalsIgnoreCase("major")) {
                    data_col[0] = i;
                } else if (val.equals("次配置") || val.equalsIgnoreCase("minor")) {
                    data_col[1] = i;
                } else if (val.equals("补充配置") || val.equalsIgnoreCase("addition")) {
                    data_col[2] = i;
                }
            }
            break;
        }

        if (data_row >= row_num) {
            throw new InitializeException(
                    String.format("scheme \"%s\" has no configure, or it's not a scheme sheet?", sheetName));
        }

        // 数据项必须在这之后
        for (++data_row; data_row < row_num; ++data_row) {
            ExcelEngine.DataItemGridWrapper itemGrid = DataSrcExcel.createDataExcelGridWrapper(sheetWrapper, false,
                    data_row);

            String key = cell2str(itemGrid, key_col);
            ArrayList<String> datas = new ArrayList<>();
            datas.add(cell2str(itemGrid, data_col[0]));
            datas.add(cell2str(itemGrid, data_col[1]));
            datas.add(cell2str(itemGrid, data_col[2]));

            set_scheme(key, datas);
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
