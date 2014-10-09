package com.owent.xresloader;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;


/**
 * Created by owentou on 2014/9/30.
 */
public final class SchemeDataSourceExcel  implements SchemeDataSourceImpl {

    public int load() {

        try {
            String file_path = ProgramOptions.getInstance().dataSourceFile;
            FileInputStream is = new FileInputStream(file_path);
            if(null == is) {
                System.err.println("[ERROR] open file \"" + ProgramOptions.getInstance().dataSourceFile + "\" failed");
                return -21;
            }
            //POIFSFileSystem scheme_file = new POIFSFileSystem(is);

            Workbook wb = null;
            // 类型枚举，以后能支持 ods等非微软格式？
            if (file_path.endsWith(".xls"))
                wb = new HSSFWorkbook(is);
            else
                wb = new XSSFWorkbook(is);

            for(String sn : ProgramOptions.getInstance().dataSourceMetas) {
                if (false == load_scheme(wb, sn)) {
                    System.err.println("[ERROR] convert scheme \"" + sn + "\" failed");
                } else {
                    System.out.println("[INFO] convert scheme \"" + sn + "\" success");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

        return 0;
    }

    public boolean load_scheme(Workbook wb, String sheet_name) {
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
        for(; data_row < row_num; ++ data_row) {
            Row row = table.getRow(data_row);
            int col_num = row.getLastCellNum() + 1;
            for (key_col = 0; key_col < col_num; ++ key_col) {
                String val = cell2str(row, key_col);
                val = val.trim();
                if (val.equals("字段")) {
                    break;
                }
            }

            if (key_col >= col_num)
                continue;

            // 数据列判定
            for (int i = 0; i < col_num; ++ i) {
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
        for(++ data_row; data_row < row_num; ++ data_row) {
            Row row = table.getRow(data_row);
            if (null == row)
                continue;

            String key = cell2str(row, key_col);
            // 基础配置
            if (key.equalsIgnoreCase("DataSource")) {
                SchemeConf.getInstance().setDateSourceFile(cell2str(row, data_col[0]));
                SchemeConf.getInstance().setDateSourceTable(cell2str(row, data_col[1]));
            } else if (key.equalsIgnoreCase("DataRect")) {
                SchemeConf.getInstance().setDateRectRow(cell2int(row,data_col[0]));
                SchemeConf.getInstance().setDateRectCol(cell2int(row,data_col[1]));
            } else if (key.equalsIgnoreCase("MacroSource")) {
                SchemeConf.getInstance().setMacroSourceFile(cell2str(row,data_col[0]));
                SchemeConf.getInstance().setMacroSourceTable(cell2str(row, data_col[1]));
            } else if (key.equalsIgnoreCase("MacroRect")) {
                SchemeConf.getInstance().setMacroRectRow(cell2int(row,data_col[0]));
                SchemeConf.getInstance().setMacroRectCol(cell2int(row,data_col[1]));
            }
            // 字段映射配置
            else if (key.equalsIgnoreCase("ProtoName")) {
                SchemeConf.getInstance().setProtoName(cell2str(row,data_col[0]));
            } else if (key.equalsIgnoreCase("OutputFile")) {
                SchemeConf.getInstance().setOutputFile(cell2str(row,data_col[0]));
            } else if (key.equalsIgnoreCase("KeyRow")) {
                SchemeConf.getInstance().getKey().setRow(cell2int(row,data_col[0]));
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
                SchemeConf.getInstance().getKey().setWordSplit(cell2str(row,data_col[0]));
            } else if (key.equalsIgnoreCase("KeyPrefix")) {
                SchemeConf.getInstance().getKey().setPrefix(cell2str(row,data_col[0]));
            } else if (key.equalsIgnoreCase("KeySuffix")) {
                SchemeConf.getInstance().getKey().setSuffix(cell2str(row,data_col[0]));
            } else if (key.equalsIgnoreCase("KeyTypePrefix")) {
                String v = cell2str(row,data_col[0]);
                if (v.equals("是") || v.equalsIgnoreCase("Yes") || v.equalsIgnoreCase("True") || v.equalsIgnoreCase("1")) {
                    SchemeConf.getInstance().getKey().setEnableTypeSuffix(true);
                } else {
                    SchemeConf.getInstance().getKey().setEnableTypeSuffix(false);
                }
            } else if (key.equalsIgnoreCase("Encoding")) {
                SchemeConf.getInstance().getKey().setEncoding(cell2str(row,data_col[0]));
            }
        }

        return true;
    }

    private String cell2str(Row row, int col) {
        Cell c = row.getCell(col);
        if (null == c)
            return "";

        String v = row.getCell(col).getStringCellValue();
        if (null == v)
            return "";
        return v.trim();
    }

    private int cell2int(Row row, int col) {
        Cell c = row.getCell(col);
        if (null == c)
            return 0;
        return (int)c.getNumericCellValue();
    }
}
