package com.owent.xresloader.data.src;

import com.owent.xresloader.engine.ExcelEngine;
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

    public DataSrcExcel(){
        super();

        macros = new HashMap<String, String>();
    }

    public int init() {
        int ret = init_macros();
        if (ret < 0)
            return ret;

        return init_sheet();
    }

    private int init_macros() {
        SchemeConf scfg = SchemeConf.getInstance();
        if(scfg.getMacroSourceFile().isEmpty() || scfg.getMacroSourceTable().isEmpty())
            return 0;

        Sheet tb = ExcelEngine.openSheet(scfg.getMacroSourceFile(), scfg.getMacroSourceTable());
        FormulaEvaluator evalor =tb.getWorkbook().getCreationHelper().createFormulaEvaluator();

        int row_num = tb.getLastRowNum() + 1;
        for(int i = scfg.getMacroRectRow() - 1; i < row_num; ++ i) {
            Row row = tb.getRow(i);
            String key = ExcelEngine.cell2str(row, scfg.getDateRectCol() - 1);
            String val = ExcelEngine.cell2str(row, scfg.getDateRectCol(), evalor);
            if (!key.isEmpty() && !val.isEmpty()) {
                macros.put(key, val);
            }
        }

        return 0;
    }

    private int init_sheet() {
        return 0;
    }
}
