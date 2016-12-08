package com.owent.xresloader.data.src;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.err.ConvException;
import com.owent.xresloader.engine.ExcelEngine;
import com.owent.xresloader.engine.IdentifyDescriptor;
import com.owent.xresloader.engine.IdentifyEngine;
import com.owent.xresloader.scheme.SchemeConf;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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
        public HashMap<String, IdentifyDescriptor> name_mapping = new HashMap<String, IdentifyDescriptor>();
    }

    private HashMap<String, String> macros = null;
    private LinkedList<DataSheetInfo> tables = new LinkedList<DataSheetInfo>();
    DataSheetInfo current = null;
    int recordNumber = 0;

    public DataSrcExcel() {
        super();

        macros = null;
    }

    @Override
    public int init() {
        int ret = init_macros();
        if (ret < 0)
            return ret;

        return init_sheet();
    }

    private class MacroFileCache {
        public SchemeConf.DataInfo file = null;
        public HashMap<String, String> macros = new HashMap<String, String>();

        public MacroFileCache(SchemeConf.DataInfo _f, String fixed_file_name) {
            file = _f;
            file.file_path = fixed_file_name;
        }
    }

    /***
     * macro表cache
     */
    static private class MacroCacheInfo {
        /*** file_path:key->value ***/
        private HashMap<String, MacroFileCache> cache = new HashMap<String, MacroFileCache>();
        private HashMap<String, String> empty = new HashMap<String, String>(); // 空项特殊处理
    };
    static private MacroCacheInfo macro_cache = new MacroCacheInfo();

    /***
     * 构建macro表cache，由于macro表大多数情况下都一样，所以加缓存优化
     */
    HashMap<String, String> init_macro_with_cache(List<SchemeConf.DataInfo> src_list) {
        LinkedList<HashMap<String, String> > data_filled = new LinkedList<HashMap<String, String> >();

        IdentifyDescriptor column_ident = new IdentifyDescriptor();

        // 枚举所有macro表信息
        for(SchemeConf.DataInfo src: src_list) {
            String file_path = "";
            if (false == src.file_path.isEmpty()) {
                file_path = src.file_path;
            }
            String fp_name = file_path + "/" + src.table_name;

            // 优先读缓存
            MacroFileCache res = macro_cache.cache.getOrDefault(fp_name, null);
            if (null != res) {
                if (res.file.file_path.equals(file_path) && res.file.table_name.equals(src.table_name)
                        && res.file.data_row == src.data_row && res.file.data_col == src.data_col) {
                    data_filled.add(res.macros);
                    continue;
                } else {
                    ProgramOptions.getLoger().warn("try to open macro source \"%s:%s\" (row=%d,col=%d) but already has cache \"%s:%s\" (row=%d,col=%d). the old macros will be covered",
                            file_path, src.table_name, src.data_row, src.data_col,
                            res.file.file_path, res.file.table_name, res.file.data_row, res.file.data_col);
                }
            }
            res = new MacroFileCache(src, file_path);

            if (file_path.isEmpty() || src.table_name.isEmpty() || src.data_col <= 0 || src.data_row <= 0) {
                ProgramOptions.getLoger().warn("macro source \"%s\" (%s:%d，%d) ignored.", file_path, src.table_name, src.data_row, src.data_col);
                continue;
            }

            Sheet tb = ExcelEngine.openSheet(file_path, src.table_name);
            if (null == tb) {
                ProgramOptions.getLoger().warn("open macro source \"%s\" or table %s failed.", file_path, src.table_name);
                continue;
            }

            FormulaEvaluator evalor = tb.getWorkbook().getCreationHelper().createFormulaEvaluator();

            int row_num = tb.getLastRowNum() + 1;
            for (int i = src.data_row - 1; i < row_num; ++i) {
                Row row = tb.getRow(i);
                column_ident.index = src.data_col - 1;
                DataContainer<String> key = ExcelEngine.cell2s(row, column_ident);
                column_ident.index = src.data_col;
                DataContainer<String> val = ExcelEngine.cell2s(row, column_ident, evalor);
                if (key.valid && val.valid && !key.get().isEmpty() && !val.get().isEmpty()) {
                    if (res.macros.containsKey(key)) {
                        ProgramOptions.getLoger().warn("macro key \"%s\" is used more than once.", key);
                    }
                    res.macros.put(key.get(), val.get());
                }
            }

            macro_cache.cache.put(fp_name, res);
            data_filled.add(res.macros);
        }

        // 空对象特殊处理
        if (data_filled.isEmpty()) {
            return macro_cache.empty;
        }

        // 只有一个macro项，则直接返回
        if (1 == data_filled.size()) {
            return data_filled.getFirst();
        }

        HashMap<String, String> ret = new HashMap<String, String>();
        for(HashMap<String, String> copy_from: data_filled) {
            ret.putAll(copy_from);
        }

        return ret;
    }

    /**
     * 初始化macros提花规则，先全部转为字符串，有需要后续在使用的时候再转
     *
     * @return
     */
    private int init_macros() {
        SchemeConf scfg = SchemeConf.getInstance();
        macros = init_macro_with_cache(scfg.getMacroSource());

        return 0;
    }

    private int init_sheet() {
        tables.clear();
        recordNumber = 0;

        SchemeConf scfg = SchemeConf.getInstance();
        String file_path = "";

        IdentifyDescriptor column_ident = new IdentifyDescriptor();

        // 枚举所有数据表信息
        for(SchemeConf.DataInfo src: scfg.getDataSource()) {
            if (false == src.file_path.isEmpty()) {
                file_path = src.file_path;
            }

            if (file_path.isEmpty() || src.table_name.isEmpty() || src.data_col <= 0 || src.data_row <= 0) {
                ProgramOptions.getLoger().error("data source \"%s\" (%s:%d，%d) ignored.", src.file_path, src.table_name, src.data_row, src.data_col);
                continue;
            }

            Sheet tb = ExcelEngine.openSheet(file_path, src.table_name);
            if (null == tb) {
                ProgramOptions.getLoger().error("open data source \"%s\" or table %s.", src.file_path, src.table_name);
                continue;
            }

            // 公式支持
            FormulaEvaluator formula = null;
            if (ProgramOptions.getInstance().enableFormular) {
                formula = tb.getWorkbook().getCreationHelper().createFormulaEvaluator();
            }

            DataSheetInfo res = new DataSheetInfo();
            // 根据第一个表建立名称关系表
            {
                int key_row = scfg.getKey().getRow() - 1;
                Row row = tb.getRow(key_row);
                if (null == row) {
                    ProgramOptions.getLoger().error("try to get description name of %s in \"%s\" row %d failed",
                            src.table_name, src.file_path, key_row);
                    return -53;
                }

                for (int i = src.data_col - 1; i < row.getLastCellNum() + 1; ++i) {
                    column_ident.index = i;
                    DataContainer<String> k = ExcelEngine.cell2s(row, column_ident, formula);
                    IdentifyDescriptor ident = IdentifyEngine.n2i(k.get(), i);
                    res.name_mapping.put(ident.name, ident);
                }
            }


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
    public boolean next_table() {
        current = null;
        if (tables.isEmpty()) {
            return false;
        }

        while(true) {
            current = tables.removeFirst();
            if (null != current) {
                break;
            }
        }

        return current != null;
    }

    @Override
    public boolean next_row() {
        if (null == current) {
            return false;
        }

        while(true) {
            // 当前行超出
            if (current.next_index > current.last_row_number) {
                current.current_row = null;
                return false;
            }

            current.current_row = current.table.getRow(current.next_index);
            ++current.next_index;

            // 过滤空行
            if (null != current.current_row) {
                break;
            }
        }

        return null != current && null != current.current_row;
    }

    @Override
    public <T> DataContainer<T> getValue(IdentifyDescriptor ident, T ret_default) throws ConvException {
        DataContainer<T> ret = new DataContainer<T>();
        ret.value = ret_default;

        if (null == ident) {
            return ret;
        }

        if (ret_default instanceof Integer) {
            DataContainer<Long> dt = ExcelEngine.cell2i(current.current_row, ident, current.formula);
            ret.valid = dt.valid;
            ret.value = (T)Integer.valueOf(dt.value.intValue());
            return ret;
        } else if (ret_default instanceof Long) {
            DataContainer<Long> dt = ExcelEngine.cell2i(current.current_row, ident, current.formula);
            ret.valid = dt.valid;
            ret.value = (T)Long.valueOf(dt.value.longValue());
            return ret;
        } else if (ret_default instanceof Short) {
            DataContainer<Long> dt = ExcelEngine.cell2i(current.current_row, ident, current.formula);
            ret.valid = dt.valid;
            ret.value = (T)Short.valueOf(dt.value.shortValue());
            return ret;
        } else if (ret_default instanceof Byte) {
            DataContainer<Long> dt = ExcelEngine.cell2i(current.current_row, ident, current.formula);
            ret.valid = dt.valid;
            ret.value = (T)Byte.valueOf(dt.value.byteValue());
            return ret;
        } else if (ret_default instanceof Double) {
            DataContainer<Double> dt = ExcelEngine.cell2d(current.current_row, ident, current.formula);
            ret.valid = dt.valid;
            ret.value = (T)Double.valueOf(dt.value.doubleValue());
            return ret;
        } else if (ret_default instanceof Float) {
            DataContainer<Double> dt = ExcelEngine.cell2d(current.current_row, ident, current.formula);
            ret.valid = dt.valid;
            ret.value = (T)Float.valueOf(dt.value.floatValue());
            return ret;
        } else if (ret_default instanceof Boolean) {
            ret = (DataContainer<T>) ExcelEngine.cell2b(current.current_row, ident, current.formula);
        } else if (ret_default instanceof String) {
            ret = (DataContainer<T>) ExcelEngine.cell2s(current.current_row, ident, current.formula);
        } else {
            ProgramOptions.getLoger().error("default value not supported");
        }

        return ret;
    }

    @Override
    public int getRecordNumber() {
        return recordNumber;
    }

    @Override
    public IdentifyDescriptor getColumnByName(String _name) {
        return current.name_mapping.getOrDefault(_name, null);
    }

    @Override
    public HashMap<String, String> getMacros() {
        return macros;
    }
}
