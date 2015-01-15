package com.owent.xresloader.scheme;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.engine.ExcelEngine;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;


/**
 * Created by owentou on 2014/9/30.
 */
public final class SchemeDataSourceExcel implements SchemeDataSourceImpl {

    private Workbook currentWorkbook = null;

    public int load() {

        String file_path = ProgramOptions.getInstance().dataSourceFile;
        currentWorkbook = ExcelEngine.openWorkbook(file_path);
        if (null == currentWorkbook) {
            System.err.println("[ERROR] open file \"" + ProgramOptions.getInstance().dataSourceFile + "\" failed");
            return -21;
        }

        return 0;
    }

    public boolean load_scheme(String sheet_name) {
        Workbook wb = currentWorkbook;
        Sheet table = wb.getSheet(sheet_name);
        if (null == table) {
            System.err.println("[ERROR] excel sheet \"" + sheet_name + "\" not found");
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
                if (val.equals("字段")) {
                    break;
                }
            }

            if (key_col >= col_num)
                continue;

            // 数据列判定
            for (int i = 0; i < col_num; ++i) {
                String val = cell2str(row, i);
                val = val.trim();
                if (val.equals("主配置")) {
                    data_col[0] = i;
                } else if (val.equals("次配置")) {
                    data_col[1] = i;
                } else if (val.equals("补充配置")) {
                    data_col[2] = i;
                }
            }
            break;
        }

        if (data_row >= row_num) {
            System.err.println("[ERROR] scheme \"" + sheet_name + "\" has no valid header");
            return false;
        }

        // 数据项必须在这之后
        for (++data_row; data_row < row_num; ++data_row) {
            Row row = table.getRow(data_row);
            if (null == row)
                continue;

            String key = cell2str(row, key_col);
            // 基础配置
            if (key.equalsIgnoreCase("DataSource")) {
                SchemeConf.getInstance().addDataSource(
                    cell2str(row, data_col[0]),
                    cell2str(row, data_col[1]),
                    cell2str(row, data_col[2])
                );
            } else if (key.equalsIgnoreCase("MacroSource")) {
                SchemeConf.getInstance().addMacroSource(
                    cell2str(row, data_col[0]),
                    cell2str(row, data_col[1]),
                    cell2str(row, data_col[2])
                );
            }

            // 字段映射配置
            else if (key.equalsIgnoreCase("ProtoName")) {
                SchemeConf.getInstance().setProtoName(cell2str(row, data_col[0]));
            } else if (key.equalsIgnoreCase("OutputFile")) {
                SchemeConf.getInstance().setOutputFile(cell2str(row, data_col[0]));
            } else if (key.equalsIgnoreCase("KeyRow")) {
                SchemeConf.getInstance().getKey().setRow(cell2int(row, data_col[0]));
            } else if (key.equalsIgnoreCase("KeyCase")) {
                String letter_case = cell2str(row, data_col[0]);
                if (letter_case.equals("大写")) {
                    SchemeConf.getInstance().getKey().setLetterCase(SchemeKeyConf.KeyCase.UPPER);
                } else if (letter_case.equals("小写")) {
                    SchemeConf.getInstance().getKey().setLetterCase(SchemeKeyConf.KeyCase.LOWER);
                } else {
                    SchemeConf.getInstance().getKey().setLetterCase(SchemeKeyConf.KeyCase.NONE);
                }
            } else if (key.equalsIgnoreCase("KeyWordSplit")) {
                SchemeConf.getInstance().getKey().setWordSplit(cell2str(row, data_col[0]));
            } else if (key.equalsIgnoreCase("KeyPrefix")) {
                SchemeConf.getInstance().getKey().setPrefix(cell2str(row, data_col[0]));
            } else if (key.equalsIgnoreCase("KeySuffix")) {
                SchemeConf.getInstance().getKey().setSuffix(cell2str(row, data_col[0]));
            } else if (key.equalsIgnoreCase("KeyWordRegex")) {
                SchemeConf.getInstance().getKey().buildKeyWordRegex(cell2str(row, data_col[0]));
                SchemeConf.getInstance().getKey().buildKeyWordRegexRemoveRule(cell2str(row, data_col[1]));
                SchemeConf.getInstance().getKey().buildKeyWordRegexPrefixRule(cell2str(row, data_col[2]));
            } else if (key.equalsIgnoreCase("Encoding")) {
                SchemeConf.getInstance().getKey().setEncoding(cell2str(row, data_col[0]));
            }
        }

        return true;
    }

    private String cell2str(Row row, int col) {
        return ExcelEngine.cell2s(row, col);
    }

    private int cell2int(Row row, int col) {
        return ExcelEngine.cell2i(row, col).intValue();
    }
}
