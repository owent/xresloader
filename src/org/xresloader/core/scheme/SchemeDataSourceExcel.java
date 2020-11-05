package org.xresloader.core.scheme;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.err.InitializeException;
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.engine.IdentifyDescriptor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;

/**
 * Created by owentou on 2014/9/30.
 */
public final class SchemeDataSourceExcel extends SchemeDataSourceBase {

    private Workbook currentWorkbook = null;

    public int load() throws InitializeException {

        String file_path = ProgramOptions.getInstance().dataSourceFile;
        currentWorkbook = ExcelEngine.openWorkbook(file_path);
        if (null == currentWorkbook) {
            throw new InitializeException(
                    String.format("open file \"%s\" failed", ProgramOptions.getInstance().dataSourceFile));
        }

        return 0;
    }

    public boolean load_scheme(String sheet_name) throws InitializeException {
        Workbook wb = currentWorkbook;
        Sheet table = wb.getSheet(sheet_name);
        if (null == table) {
            throw new InitializeException(String.format("excel sheet \"%s\" not found", sheet_name));
        }

        // 数据及header
        int row_num = table.getLastRowNum() + 1;
        int data_row = 0;
        int key_col = 0; // 字段列
        int data_col[] = { 2, 3, 4 }; // 数据列

        // 查找“字段”列
        for (; data_row < row_num; ++data_row) {
            ExcelEngine.DataRowWrapper rowWrapper = new ExcelEngine.DataRowWrapper(table.getRow(data_row));
            int col_num = rowWrapper.getLastCellNum() + 1;
            for (key_col = 0; key_col < col_num; ++key_col) {
                String val = cell2str(rowWrapper, key_col);
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
                String val = cell2str(rowWrapper, i);
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
                    String.format("scheme \"%s\" has no configure, or it's not a scheme sheet?", sheet_name));
        }

        // 数据项必须在这之后
        for (++data_row; data_row < row_num; ++data_row) {
            ExcelEngine.DataRowWrapper rowWrapper = new ExcelEngine.DataRowWrapper(table.getRow(data_row));
            if (null == rowWrapper) {
                continue;
            }

            String key = cell2str(rowWrapper, key_col);
            ArrayList<String> datas = new ArrayList<String>();
            datas.add(cell2str(rowWrapper, data_col[0]));
            datas.add(cell2str(rowWrapper, data_col[1]));
            datas.add(cell2str(rowWrapper, data_col[2]));

            set_scheme(key, datas);
        }

        return true;
    }

    private String cell2str(ExcelEngine.DataRowWrapper rowWrapper, int col) {
        IdentifyDescriptor ident = new IdentifyDescriptor();
        ident.index = col;

        DataContainer<String> cache = DataSrcImpl.getStringCache("");
        ExcelEngine.cell2s(cache, rowWrapper, ident);
        return cache.get();
    }

    private int cell2int(ExcelEngine.DataRowWrapper rowWrapper, int col) throws ConvException {
        IdentifyDescriptor ident = new IdentifyDescriptor();
        ident.index = col;

        DataContainer<Long> cache = DataSrcImpl.getLongCache(0L);

        ExcelEngine.cell2i(cache, rowWrapper, ident);
        return cache.get().intValue();
    }
}
