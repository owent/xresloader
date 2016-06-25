package com.owent.xresloader.scheme;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.err.ConvException;
import com.owent.xresloader.engine.ExcelEngine;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;


/**
 * Created by owentou on 2014/9/30.
 */
public final class SchemeDataSourceExcel extends SchemeDataSourceBase {

    private Workbook currentWorkbook = null;

    public int load() {

        String file_path = ProgramOptions.getInstance().dataSourceFile;
        currentWorkbook = ExcelEngine.openWorkbook(file_path);
        if (null == currentWorkbook) {
            ProgramOptions.getLoger().error("open file \"" + ProgramOptions.getInstance().dataSourceFile + "\" failed");
            return -21;
        }

        return 0;
    }

    public boolean load_scheme(String sheet_name) {
        Workbook wb = currentWorkbook;
        Sheet table = wb.getSheet(sheet_name);
        if (null == table) {
            ProgramOptions.getLoger().error("excel sheet \"" + sheet_name + "\" not found");
            return false;
        }

        // 数据及header
        int row_num = table.getLastRowNum() + 1;
        int data_row = 0;
        int key_col = 0; // 字段列
        int data_col[] = {2, 3, 4}; // 数据列

        // 查找“字段”列
        for (; data_row < row_num; ++data_row) {
            Row row = table.getRow(data_row);
            int col_num = row.getLastCellNum() + 1;
            for (key_col = 0; key_col < col_num; ++key_col) {
                String val = cell2str(row, key_col);
                val = val.trim();
                if (val.equals("字段") || val.equalsIgnoreCase("header")) {
                    break;
                }
            }

            if (key_col >= col_num)
                continue;

            // 数据列判定
            for (int i = 0; i < col_num; ++i) {
                String val = cell2str(row, i);
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
            ProgramOptions.getLoger().error("scheme \"" + sheet_name + "\" has no valid header");
            return false;
        }

        // 数据项必须在这之后
        for (++data_row; data_row < row_num; ++data_row) {
            Row row = table.getRow(data_row);
            if (null == row)
                continue;

            String key = cell2str(row, key_col);
            ArrayList<String> datas = new ArrayList<String>();
            datas.add(cell2str(row, data_col[0]));
            datas.add(cell2str(row, data_col[1]));
            datas.add(cell2str(row, data_col[2]));

            set_scheme(key, datas);
        }

        return true;
    }

    private String cell2str(Row row, int col) {
        return ExcelEngine.cell2s(row, col).get();
    }

    private int cell2int(Row row, int col) throws ConvException {
        return ExcelEngine.cell2i(row, col).get().intValue();
    }
}
