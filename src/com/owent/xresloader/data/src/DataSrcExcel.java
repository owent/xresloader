package com.owent.xresloader.data.src;

import com.owent.xresloader.engine.ExcelEngine;
import com.owent.xresloader.engine.IdentifyEngine;
import com.owent.xresloader.scheme.SchemeConf;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.HashMap;

/**
 * Created by owentou on 2014/10/9.
 */
public class DataSrcExcel extends DataSrcImpl {

    private HashMap<String, String> macros = null;
    private HashMap<String, Integer> nameMap = null;
    private Sheet currentSheet = null;
    private FormulaEvaluator currentSheetFormula = null;
    private Row currentRow = null;
    private int currentIndex;

    public DataSrcExcel(){
        super();

        macros = new HashMap<String, String>();
        nameMap = new HashMap<String, Integer>();
    }

    public int init() {
        int ret = init_macros();
        if (ret < 0)
            return ret;

        return init_sheet();
    }

    /**
     * 初始化macros提花规则，先全部转为字符串，有需要后续在使用的时候再转
     * @return
     */
    private int init_macros() {
        macros.clear();

        SchemeConf scfg = SchemeConf.getInstance();
        if(scfg.getMacroSourceFile().isEmpty() || scfg.getMacroSourceTable().isEmpty())
            return 0;

        Sheet tb = ExcelEngine.openSheet(scfg.getMacroSourceFile(), scfg.getMacroSourceTable());
        if (null == tb) {
            System.err.println("[WARNING] open macro file \"" + scfg.getMacroSourceFile() + "\" or table \"" + scfg.getMacroSourceTable() + "\" failed");
            return 0;
        }
        FormulaEvaluator evalor = tb.getWorkbook().getCreationHelper().createFormulaEvaluator();

        int row_num = tb.getLastRowNum() + 1;
        for(int i = scfg.getMacroRectRow() - 1; i < row_num; ++ i) {
            Row row = tb.getRow(i);
            String key = ExcelEngine.cell2s(row, scfg.getDateRectCol() - 1);
            String val = ExcelEngine.cell2s(row, scfg.getDateRectCol(), evalor);
            if (!key.isEmpty() && !val.isEmpty()) {
                macros.put(key, val);
            }
        }

        return 0;
    }

    private int init_sheet() {
        SchemeConf scfg = SchemeConf.getInstance();
        if(scfg.getDateSourceFile().isEmpty() || scfg.getDateSourceTable().isEmpty()) {
            System.err.println("[ERROR] convert failed without data source file or table");
            return -51;
        }

        currentSheet = ExcelEngine.openSheet(scfg.getDateSourceFile(), scfg.getDateSourceTable());
        if (null == currentSheet) {
            System.err.println("[WARNING] open data file \"" + scfg.getDateSourceFile() + "\" or table \"" + scfg.getDateSourceTable() + "\" failed");
            return -52;
        }

        currentSheetFormula = currentSheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
        // 建立名称关系表
        nameMap.clear();
        int key_row = scfg.getKey().getRow() - 1;
        Row row = currentSheet.getRow(key_row);
        if (null == row) {
            System.err.println("[ERROR] get description name row failed");
            return -53;
        }
        for(int i = scfg.getDateRectCol() - 1; i < row.getLastCellNum() + 1; ++ i) {
            String k = ExcelEngine.cell2s(row, i, currentSheetFormula);
            nameMap.put(IdentifyEngine.n2i(k), i);
        }

        currentIndex = scfg.getDateRectRow();

        return 0;
    }

    public boolean next() {
        if (currentIndex > currentSheet.getLastRowNum())
            return false;

        currentRow = currentSheet.getRow(currentIndex);
        ++ currentIndex;

        return null != currentRow;
    }

    public <T> T getValue(String ident, Class<T> clazz) {
        T ret = null;
        try {
            ret = clazz.newInstance();
            int index = nameMap.getOrDefault(ident, -1);
            if (index < 0)
                return ret;

            if (ret instanceof Integer || ret instanceof Long || ret instanceof Short || ret instanceof Character) {
                ret = ((T) ExcelEngine.cell2i(currentRow, index, currentSheetFormula));
            } else if (ret instanceof Float || ret instanceof Double) {
                ret = ((T)ExcelEngine.cell2d(currentRow, index, currentSheetFormula));
            } else if (ret instanceof Boolean) {
                ret = ((T)ExcelEngine.cell2b(currentRow, index, currentSheetFormula));
            } else {
                ret = ((T)ExcelEngine.cell2s(currentRow, index, currentSheetFormula));
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public int getRecordNumber() {
        if (null == currentSheet)
            return 0;

        return currentSheet.getLastRowNum() - SchemeConf.getInstance().getDateRectRow() + 2;
    }
}
