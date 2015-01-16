package com.owent.xresloader.data.src;

import com.owent.xresloader.engine.ExcelEngine;
import com.owent.xresloader.engine.IdentifyEngine;
import com.owent.xresloader.scheme.SchemeConf;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by owentou on 2014/10/9.
 */
public class DataSrcExcel extends DataSrcImpl {

    private class DataSheetInfo {
        public Sheet table = null;
        public FormulaEvaluator formula = null;
        public Row current_row = null;
        public int next_index = 0;
        public int last_row_number = 0;
    }

    private HashMap<String, String> macros = null;
    private HashMap<String, Integer> nameMap = null;
    private LinkedList<DataSheetInfo> tables = new LinkedList<DataSheetInfo>();
    DataSheetInfo current = null;
    int recordNumber = 0;

    public DataSrcExcel() {
        super();

        macros = new HashMap<String, String>();
        nameMap = new HashMap<String, Integer>();
    }

    @Override
    public int init() {
        int ret = init_macros();
        if (ret < 0)
            return ret;

        return init_sheet();
    }

    /**
     * 初始化macros提花规则，先全部转为字符串，有需要后续在使用的时候再转
     *
     * @return
     */
    private int init_macros() {
        macros.clear();

        String file_path = "";
        SchemeConf scfg = SchemeConf.getInstance();
        // 枚举所有macro表信息
        for(SchemeConf.DataInfo src: scfg.getMacroSource()) {
            if (false == src.file_path.isEmpty()) {
                file_path = src.file_path;
            }

            if (file_path.isEmpty() || src.table_name.isEmpty() || src.data_col <= 0 || src.data_row <= 0) {
                System.err.println(
                    String.format("[WARNING] macro source \"%s\" (%s:%d，%d) ignored.", src.file_path, src.table_name, src.data_row, src.data_col)
                );
                continue;
            }

            Sheet tb = ExcelEngine.openSheet(file_path, src.table_name);
            if (null == tb) {
                System.err.println(
                    String.format("[WARNING] open macro source \"%s\" or table %s.", src.file_path, src.table_name)
                );
                continue;
            }

            FormulaEvaluator evalor = tb.getWorkbook().getCreationHelper().createFormulaEvaluator();

            int row_num = tb.getLastRowNum() + 1;
            for (int i = src.data_row - 1; i < row_num; ++i) {
                Row row = tb.getRow(i);
                String key = ExcelEngine.cell2s(row, src.data_col - 1);
                String val = ExcelEngine.cell2s(row, src.data_col, evalor);
                if (!key.isEmpty() && !val.isEmpty()) {
                    if (macros.containsKey(key)) {
                        System.err.println(
                            String.format("[WARNING] macro key \"%s\" is used more than once.", key)
                        );
                    }
                    macros.put(key, val);
                }
            }
        }

        return 0;
    }

    private int init_sheet() {
        tables.clear();
        recordNumber = 0;
        nameMap.clear();

        SchemeConf scfg = SchemeConf.getInstance();
        String file_path = "";

        // 枚举所有数据表信息
        for(SchemeConf.DataInfo src: scfg.getDataSource()) {
            if (false == src.file_path.isEmpty()) {
                file_path = src.file_path;
            }

            if (file_path.isEmpty() || src.table_name.isEmpty() || src.data_col <= 0 || src.data_row <= 0) {
                System.err.println(
                    String.format("[ERROR] data source \"%s\" (%s:%d，%d) ignored.", src.file_path, src.table_name, src.data_row, src.data_col)
                );
                continue;
            }

            Sheet tb = ExcelEngine.openSheet(file_path, src.table_name);
            if (null == tb) {
                System.err.println(
                    String.format("[WARNING] open data source \"%s\" or table %s.", src.file_path, src.table_name)
                );
                continue;
            }

            FormulaEvaluator formula = tb.getWorkbook().getCreationHelper().createFormulaEvaluator();

            // 根据第一个表建立名称关系表
            if (nameMap.isEmpty()) {
                int key_row = scfg.getKey().getRow() - 1;
                Row row = tb.getRow(key_row);
                if (null == row) {
                    System.err.println("[ERROR] get description name row failed");
                    return -53;
                }
                for (int i = src.data_col - 1; i < row.getLastCellNum() + 1; ++i) {
                    String k = ExcelEngine.cell2s(row, i, formula);
                    nameMap.put(IdentifyEngine.n2i(k), i);
                }
            }

            DataSheetInfo res = new DataSheetInfo();
            res.table = tb;
            res.formula = formula;
            res.next_index = src.data_row - 1;
            res.last_row_number = tb.getLastRowNum();
            res.current_row = null;

            tables.add(res);

            // 记录数量计数
            recordNumber += res.last_row_number - src.data_row + 2;
        }

        return 0;
    }

    @Override
    public boolean next() {
        // 当前行超出
        if (null != current && current.next_index > current.last_row_number) {
            current = null;
        }

        if (null == current && tables.isEmpty()) {
            return false;
        }

        if (null == current) {
            current = tables.removeFirst();
        }

        if (null == current) {
            return false;
        }

        current.current_row = current.table.getRow(current.next_index);
        ++current.next_index;

        return null != current.current_row;
    }


    @Override
    public <T> T getValue(String ident, T ret) {
        int index = nameMap.getOrDefault(ident, -1);
        if (index < 0)
            return ret;

        if (ret instanceof Integer) {
            ret = (T) Integer.valueOf(ExcelEngine.cell2i(current.current_row, index, current.formula).toString());
        } else if (ret instanceof Long) {
            ret = (T) ExcelEngine.cell2i(current.current_row, index, current.formula);
        } else if (ret instanceof Short) {
            ret = (T) Short.valueOf(ExcelEngine.cell2i(current.current_row, index, current.formula).toString());
        } else if (ret instanceof Character) {
            ret = (T) Character.valueOf(ExcelEngine.cell2i(current.current_row, index, current.formula).toString().charAt(0));
        } else if (ret instanceof Double) {
            ret = (T) ExcelEngine.cell2d(current.current_row, index, current.formula);
        } else if (ret instanceof Float) {
            ret = (T) Float.valueOf(ExcelEngine.cell2d(current.current_row, index, current.formula).toString());
        } else if (ret instanceof Boolean) {
            ret = (T) ExcelEngine.cell2b(current.current_row, index, current.formula);
        } else {
            ret = (T) ExcelEngine.cell2s(current.current_row, index, current.formula);
        }
        return ret;
    }

    @Override
    public int getRecordNumber() {
        return recordNumber;
    }

    @Override
    public boolean checkName(String _name) {
        return nameMap.containsKey(_name);
    }

    @Override
    public HashMap<String, String> getMacros() {
        return macros;
    }
}
