package org.xresloader.core.data.src;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.engine.ExcelEngine.DataItemGridWrapper;
import org.xresloader.core.engine.ExcelEngine.DataSheetWrapper;
import org.xresloader.core.engine.IdentifyDescriptor;
import org.xresloader.core.engine.IdentifyEngine;
import org.xresloader.core.scheme.SchemeConf;

/**
 * Created by owentou on 2014/10/9.
 */
public class DataSrcExcel extends DataSrcImpl {
    static private class DataSheetInfo extends ExcelEngine.DataSheetWrapper {
        public Sheet userModuleTable = null;
        public ExcelEngine.CustomDataTableIndex customTableIndex = null;
        public ExcelEngine.FormulaWrapper formula = null;
        public ExcelEngine.DataItemGridWrapper currentDataGrid = null;
        public int nextDataItemIndex = 0;
        public int lastDataItemIndex = -1;

        // 行数可以直接读外层接口，列数需要计算然后缓存
        public int lastColumnIndex = -2;
        public HashMap<String, IdentifyDescriptor> nameMapping = new HashMap<>();
        public LinkedList<IdentifyDescriptor> indexMapping = new LinkedList<>();
        public HashMap<String, Boolean> identifyPrefixAvailable = new HashMap<>();

        private final HashMap<Integer, ExcelEngine.DataRowWrapper> rowCache = new HashMap<>();

        DataSheetInfo(SchemeConf.DataInfo dataSource) {
            super(dataSource);
        }

        @Override
        public ExcelEngine.DataRowWrapper getRow(int keyRow) {
            ExcelEngine.DataRowWrapper ret = rowCache.getOrDefault(keyRow, null);
            if (ret != null) {
                return ret;
            }

            if (null != userModuleTable) {
                var data = userModuleTable.getRow(keyRow);
                if (data == null) {
                    return null;
                }
                ret = new ExcelEngine.DataRowWrapper(data);
            } else if (null != customTableIndex) {
                var data = customTableIndex.getRow(keyRow);
                if (data == null) {
                    return null;
                }
                ret = new ExcelEngine.DataRowWrapper(data);
            }
            rowCache.put(keyRow, ret);

            return ret;
        }

        @Override
        public int getLastRowIndex() {
            if (null != userModuleTable) {
                return userModuleTable.getLastRowNum();
            } else if (null != customTableIndex) {
                return customTableIndex.getLastRowIndex();
            } else {
                return -1;
            }
        }

        @Override
        public int getLastColumnIndex() {
            if (this.lastColumnIndex < -2) {
                this.lastColumnIndex = -1;
                if (null != userModuleTable) {
                    for (int i = userModuleTable.getFirstRowNum(); i <= userModuleTable.getLastRowNum(); ++i) {
                        Row row = userModuleTable.getRow(i);
                        if (row == null) {
                            continue;
                        }
                        int lastCol = row.getLastCellNum();
                        if (lastCol > this.lastColumnIndex) {
                            this.lastColumnIndex = lastCol;
                        }
                    }
                    return userModuleTable.getLastRowNum();
                } else if (null != customTableIndex) {
                    this.lastColumnIndex = customTableIndex.getLastColumnIndex();
                }
            }

            return this.lastColumnIndex;
        }

        boolean isValid() {
            return (null != userModuleTable) || (null != customTableIndex);
        }
    }

    static private class DataExcelGridInfo extends ExcelEngine.DataItemGridWrapper {
        private DataSheetInfo sheetInfo = null;

        public DataExcelGridInfo(DataSheetInfo sheetInfo, boolean transpose, int dataItemIndex) {
            super(transpose, dataItemIndex);

            this.sheetInfo = sheetInfo;
        }

        @Override
        public int getLastDataFieldIndex() {
            if (isTranspose()) {
                return sheetInfo.getLastRowIndex();
            } else {
                ExcelEngine.DataRowWrapper rowWrapper = sheetInfo.getRow(this.getDataItemIndex());
                if (rowWrapper == null) {
                    return -1;
                }

                return rowWrapper.getLastCellNum();
            }
        }

        @Override
        public String getCustomRowIndexCellValue(DataItemGridWrapper itemGrid, IdentifyDescriptor ident) {
            int originDataRow = itemGrid.getOriginRowIndex(ident.getDataFieldIndex());
            int originDataColumn = itemGrid.getOriginColumnIndex(ident.getDataFieldIndex());

            ExcelEngine.DataRowWrapper rowWrapper = sheetInfo.getRow(originDataRow);
            if (rowWrapper == null) {
                return null;
            }

            if (null != rowWrapper.getCustomRowIndex()) {
                String ret = rowWrapper.getCustomRowIndex().getCellValue(originDataColumn);
                if (ret != null) {
                    return ret;
                }

                return "";
            }

            return null;
        }

        @Override
        public Cell getUserModuleRowCell(DataItemGridWrapper itemGrid, IdentifyDescriptor ident) {
            int originDataRow = itemGrid.getOriginRowIndex(ident.getDataFieldIndex());
            int originDataColumn = itemGrid.getOriginColumnIndex(ident.getDataFieldIndex());

            ExcelEngine.DataRowWrapper rowWrapper = sheetInfo.getRow(originDataRow);
            if (rowWrapper == null) {
                return null;
            }

            Row row = rowWrapper.getUserModuleRow();
            if (null == row) {
                return null;
            }

            return row.getCell(originDataColumn);
        }

        @Override
        public SchemeConf.DataInfo getDataSource() {
            if (sheetInfo == null) {
                return null;
            }

            return sheetInfo.getDataSource();
        }
    }

    private HashMap<String, String> macros = null;
    private final LinkedList<DataSheetInfo> tables = new LinkedList<>();
    private DataSheetInfo current = null;
    private int recordNumber = 0;
    private boolean initialized = false;

    public DataSrcExcel() {
        super();

        macros = null;
    }

    @Override
    public int init() {
        try {
            int ret = initMacros();
            if (ret < 0)
                return ret;

            ret = initSheet();
            if (ret >= 0) {
                initialized = true;
            }
            return ret;
        } catch (ConvException ex) {
            ProgramOptions.getLoger().error("Initialize DataSrcExcel failed: %s", ex.getMessage());
            return -1;
        }
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public ExcelEngine.DataItemGridWrapper getCurrentDataItemGrid() {
        if (this.current == null) {
            return null;
        }

        return this.current.currentDataGrid;
    }

    private class MacroFileCache {
        public SchemeConf.DataInfo file = null;
        public HashMap<String, String> macros = new HashMap<>();

        public MacroFileCache(SchemeConf.DataInfo _f, String fixed_file_name) {
            file = _f;
            file.filePath = fixed_file_name;
        }
    }

    /***
     * macro表cache
     */
    static private class MacroCacheInfo {
        /*** filePath:key->value ***/
        private final HashMap<String, MacroFileCache> cache = new HashMap<>();
        private final HashMap<String, String> empty = new HashMap<>(); // 空项特殊处理
    }

    private static final MacroCacheInfo macro_cache = new MacroCacheInfo();

    /**
     * 创建DataSheetWrapper对象
     * 
     * @param file          文件
     * @param sheetName     表名
     * @param enableFormula 是否开启实时公式支持（不开启则会使用自定义索引，大幅增加性能）
     * @return
     */
    public static DataSheetWrapper createDataSheetWrapper(SchemeConf.DataInfo dataSource,
            boolean enableFormula) {
        File file = DataSrcImpl.getDataFile(dataSource.filePath);
        if (file == null) {
            ProgramOptions.getLoger().warn("Open data source file \"%s\" failed, not found.", dataSource.filePath,
                    dataSource.tableName);
            return null;
        }

        DataSheetInfo ret = new DataSheetInfo(dataSource);
        // XLSX 可以使用流式读取引擎
        if (false == enableFormula) {
            ret.customTableIndex = ExcelEngine.openStreamTableIndex(file, dataSource.tableName);
            if (ret.customTableIndex == null) {
                ProgramOptions.getLoger().error("Open data source file \"%s\" or sheet \"%s\" failed.",
                        file.getPath(), dataSource.tableName);
                return null;
            }
        } else {
            ret.userModuleTable = ExcelEngine.openUserModuleSheet(file, dataSource.tableName);
            if (null == ret.userModuleTable) {
                ProgramOptions.getLoger().error("Open data source file \"%s\" or sheet \"%s\" failed.",
                        file.getPath(), dataSource.tableName);
                return null;
            }

            // 公式支持
            ret.formula = new ExcelEngine.FormulaWrapper(
                    ret.userModuleTable.getWorkbook().getCreationHelper().createFormulaEvaluator());
        }

        if (!ret.isValid()) {
            return null;
        }

        return ret;
    }

    public static DataItemGridWrapper createDataExcelGridWrapper(DataSheetWrapper sheetWrapper, boolean transpose,
            int dataItemIndex) {
        if (sheetWrapper == null || !(sheetWrapper instanceof DataSheetInfo)) {
            return null;
        }

        return new DataExcelGridInfo((DataSheetInfo) sheetWrapper, transpose, dataItemIndex);
    }

    /***
     * 构建macro表cache，由于macro表大多数情况下都一样，所以加缓存优化
     */
    HashMap<String, String> initMacroWithCache(List<SchemeConf.DataInfo> src_list) throws ConvException {
        LinkedList<HashMap<String, String>> data_filled = new LinkedList<>();

        IdentifyDescriptor columnIdent = new IdentifyDescriptor(-1);

        // 枚举所有macro表信息
        for (SchemeConf.DataInfo src : src_list) {
            String filePath = "";
            if (false == src.filePath.isEmpty()) {
                filePath = src.filePath;
            }
            String fp_name = filePath + "/" + src.tableName;

            // 优先读缓存
            MacroFileCache res = macro_cache.cache.getOrDefault(fp_name, null);
            if (null != res) {
                // macros 表不处理数据结束行列
                if (res.file.filePath.equals(filePath) && res.file.tableName.equals(src.tableName)
                        && res.file.dataRowBegin == src.dataRowBegin
                        && res.file.dataColumnBegin == src.dataColumnBegin) {
                    data_filled.add(res.macros);
                    continue;
                } else {
                    ProgramOptions.getLoger().warn(
                            "Try to open macro source \"%s:%s\" (row=%d,col=%d) but already has cache \"%s:%s\" (row=%d,col=%d). the old macros will be covered",
                            filePath, src.tableName, src.dataRowBegin, src.dataColumnBegin, res.file.filePath,
                            res.file.tableName, res.file.dataRowBegin, res.file.dataColumnBegin);
                }
            }
            res = new MacroFileCache(src, filePath);

            if (filePath.isEmpty() || src.tableName.isEmpty() || src.dataColumnBegin <= 0 || src.dataRowBegin <= 0) {
                ProgramOptions.getLoger().warn("Macro source \"%s\" (%s:%d, %d) ignored.", filePath, src.tableName,
                        src.dataRowBegin, src.dataColumnBegin);
                continue;
            }

            File file = DataSrcImpl.getDataFile(filePath);
            if (file == null) {
                ProgramOptions.getLoger().warn("Open data source file \"%s\" failed, not found.", filePath,
                        src.tableName);
                continue;
            }

            ExcelEngine.CustomDataTableIndex tb = ExcelEngine.openStreamTableIndex(file, src.tableName);
            if (null == tb) {
                ProgramOptions.getLoger().warn("Open macro source \"%s\" or sheet %s failed.", filePath,
                        src.tableName);
                continue;
            }
            DataSheetWrapper sheetWrapper = createDataSheetWrapper(src, false);
            if (null == sheetWrapper) {
                continue;
            }

            int row_num = tb.getLastRowIndex() + 1;
            for (int i = src.dataRowBegin - 1; i < row_num; ++i) {
                DataItemGridWrapper itemGrid = createDataExcelGridWrapper(sheetWrapper, false, i);
                columnIdent.resetDataSourcePosition(src.dataColumnBegin - 1);
                DataContainer<String> data_cache = getStringCache("");
                ExcelEngine.cell2s(data_cache, itemGrid, columnIdent, DataSrcImpl.getOurInstance());
                String key = data_cache.get();
                columnIdent.resetDataSourcePosition(src.dataColumnBegin);
                data_cache = getStringCache("");
                ExcelEngine.cell2s(data_cache, itemGrid, columnIdent, null);

                String val = data_cache.get();
                if (null != key && null != val && !key.isEmpty() && !val.isEmpty()) {
                    if (res.macros.containsKey(key)) {
                        ProgramOptions.getLoger().warn("Macro key \"%s\" is used more than once.", key);
                    }
                    res.macros.put(key, val);
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

        HashMap<String, String> ret = new HashMap<>();
        for (HashMap<String, String> copy_from : data_filled) {
            ret.putAll(copy_from);
        }

        return ret;
    }

    /**
     * 初始化macros提花规则，先全部转为字符串，有需要后续在使用的时候再转
     *
     * @return
     */
    private int initMacros() throws ConvException {
        SchemeConf scfg = SchemeConf.getInstance();
        macros = initMacroWithCache(scfg.getMacroSource());

        return 0;
    }

    private int initSheet() throws ConvException {
        tables.clear();
        recordNumber = 0;

        SchemeConf scfg = SchemeConf.getInstance();
        String filePath = "";

        IdentifyDescriptor columnIdent = new IdentifyDescriptor(-1);

        // 枚举所有数据表信息
        for (SchemeConf.DataInfo src : scfg.getDataSource()) {
            if (false == src.filePath.isEmpty()) {
                filePath = src.filePath;
            }

            if (filePath.isEmpty() || src.tableName.isEmpty() || src.dataColumnBegin <= 0 || src.dataRowBegin <= 0) {
                ProgramOptions.getLoger().error("Data source file \"%s\" (%s:%d, %d) ignored.", src.filePath,
                        src.tableName, src.dataRowBegin, src.dataColumnBegin);
                continue;
            }

            // 如果没有指定文件路径，则使用上一次的路径
            if (src.filePath.isEmpty()) {
                src.filePath = filePath;
            }

            DataSheetWrapper sheetWrapper = createDataSheetWrapper(src,
                    ProgramOptions.getInstance().enableFormular);
            if (sheetWrapper == null) {
                return -55;
            }
            DataSheetInfo sheetInfo = (DataSheetInfo) sheetWrapper;

            // 建立名称关系表
            boolean transpose = ProgramOptions.getInstance().dataSourceMappingTranspose;
            {
                int originKeyRow = scfg.getKey().getRow() - 1;
                DataItemGridWrapper itemGrid = createDataExcelGridWrapper(sheetWrapper, transpose, originKeyRow);

                // 起始和结束边界
                int lastDataFieldIndex = itemGrid.getLastDataFieldIndex();
                int firstDataFieldIndex = src.dataColumnBegin - 1;
                if (transpose) {
                    firstDataFieldIndex = src.dataRowBegin - 1;
                    if (src.dataRowEnd > 0 && lastDataFieldIndex >= src.dataRowEnd) {
                        lastDataFieldIndex = src.dataRowEnd - 1;
                    }
                } else {
                    if (src.dataColumnEnd > 0 && lastDataFieldIndex >= src.dataColumnEnd) {
                        lastDataFieldIndex = src.dataColumnEnd - 1;
                    }
                }

                for (int i = firstDataFieldIndex; i < lastDataFieldIndex + 1; ++i) {
                    columnIdent.resetDataSourcePosition(i);
                    DataContainer<String> k = getStringCache("");
                    ExcelEngine.cell2s(k, itemGrid, columnIdent, sheetInfo.formula, DataSrcImpl.getOurInstance());
                    IdentifyDescriptor ident = IdentifyEngine.n2i(k.get(), i);
                    String[] multipleNames = ident.name.split("[,;\\|]");
                    if (multipleNames.length > 1) {
                        for (String realName : multipleNames) {
                            String trimName = realName.trim();
                            if (trimName.isEmpty()) {
                                continue;
                            }
                            IdentifyDescriptor copyIdent;
                            try {
                                copyIdent = ident.clone();
                            } catch (CloneNotSupportedException ex) {
                                throw new ConvException("IdentifyDescriptor clone failed: " + ex.getMessage());
                            }
                            copyIdent.name = trimName;
                            sheetInfo.nameMapping.put(copyIdent.name, ident);
                            sheetInfo.indexMapping.add(copyIdent);
                        }
                    } else {
                        sheetInfo.nameMapping.put(ident.name, ident);
                        sheetInfo.indexMapping.add(ident);
                    }
                }
            }

            if (transpose) {
                sheetInfo.nextDataItemIndex = src.dataColumnBegin - 1;
                sheetInfo.lastDataItemIndex = sheetInfo.getLastColumnIndex();
                if (sheetInfo.getDataSource().dataColumnEnd > 0
                        && sheetInfo.lastDataItemIndex >= sheetInfo.getDataSource().dataColumnEnd) {
                    sheetInfo.lastDataItemIndex = sheetInfo.getDataSource().dataColumnEnd - 1;
                }
            } else {
                sheetInfo.nextDataItemIndex = src.dataRowBegin - 1;
                sheetInfo.lastDataItemIndex = sheetInfo.getLastRowIndex();
                if (sheetInfo.getDataSource().dataRowEnd > 0
                        && sheetInfo.lastDataItemIndex >= sheetInfo.getDataSource().dataRowEnd) {
                    sheetInfo.lastDataItemIndex = sheetInfo.getDataSource().dataRowEnd - 1;
                }
            }
            tables.add(sheetInfo);

            // 记录数量计数
            if (sheetInfo.lastDataItemIndex >= sheetInfo.nextDataItemIndex) {
                recordNumber += sheetInfo.lastDataItemIndex - sheetInfo.nextDataItemIndex + 1;
            }
        }

        return 0;
    }

    @Override
    public boolean nextTable() {
        current = null;
        if (tables.isEmpty()) {
            return false;
        }

        while (true) {
            current = tables.removeFirst();
            if (null != current) {
                break;
            }
        }

        return current != null;
    }

    @Override
    public boolean nextRow() {
        if (null == current) {
            return false;
        }

        boolean transpose = ProgramOptions.getInstance().dataSourceMappingTranspose;
        while (true) {
            // 当前行超出
            if (current.nextDataItemIndex > current.lastDataItemIndex) {
                current.currentDataGrid = null;

                if (current.lastDataItemIndex > LOG_PROCESS_BOUND
                        && current.lastDataItemIndex % LOG_PROCESS_BOUND != 0) {
                    ProgramOptions.getLoger().info("  > File: %s, Table: %s, process %d/%d rows",
                            current.getDataSource().filePath,
                            current.getDataSource().tableName, current.lastDataItemIndex + 1,
                            current.lastDataItemIndex + 1);
                }
                return false;
            }

            current.currentDataGrid = createDataExcelGridWrapper(current, transpose, current.nextDataItemIndex);
            if (current.nextDataItemIndex >= LOG_PROCESS_BOUND && current.nextDataItemIndex % LOG_PROCESS_BOUND == 0) {
                ProgramOptions.getLoger().info("  > File: %s, Table: %s, process %d/%d rows",
                        current.getDataSource().filePath,
                        current.getDataSource().tableName, current.nextDataItemIndex,
                        current.lastDataItemIndex + 1);
            }

            ++current.nextDataItemIndex;

            // 过滤空行
            if (null != current.currentDataGrid) {
                break;
            }
        }

        return null != current && null != current.currentDataGrid;
    }

    @Override
    public DataContainer<Boolean> getValue(IdentifyDescriptor ident, boolean dv) throws ConvException {
        DataContainer<Boolean> ret = super.getValue(ident, dv);
        if (null == ident) {
            return ret;
        }

        ExcelEngine.cell2b(ret, current.currentDataGrid, ident, current.formula);
        return ret;
    }

    @Override
    public DataContainer<String> getValue(IdentifyDescriptor ident, String dv) throws ConvException {
        DataContainer<String> ret = super.getValue(ident, dv);
        if (null == ident) {
            return ret;
        }

        ExcelEngine.cell2s(ret, current.currentDataGrid, ident, current.formula, DataSrcImpl.getOurInstance());
        return ret;
    }

    @Override
    public DataContainer<Long> getValue(IdentifyDescriptor ident, long dv) throws ConvException {
        DataContainer<Long> ret = super.getValue(ident, dv);
        if (null == ident) {
            return ret;
        }

        ExcelEngine.cell2i(ret, current.currentDataGrid, ident, current.formula);
        return ret;
    }

    @Override
    public DataContainer<Double> getValue(IdentifyDescriptor ident, double dv) throws ConvException {
        DataContainer<Double> ret = super.getValue(ident, dv);
        if (null == ident) {
            return ret;
        }

        ExcelEngine.cell2d(ret, current.currentDataGrid, ident, current.formula);
        return ret;
    }

    @Override
    public int getRecordNumber() {
        return recordNumber;
    }

    @Override
    public IdentifyDescriptor getColumnByName(String name) {
        return current.nameMapping.getOrDefault(name, null);
    }

    @Override
    public boolean containsIdentifyMappingPrefix(String name) {
        var ret = current.identifyPrefixAvailable.getOrDefault(name, null);
        if (ret != null) {
            return ret;
        }

        boolean res = false;
        for (HashMap.Entry<String, IdentifyDescriptor> ident : current.nameMapping.entrySet()) {
            if (!ident.getKey().startsWith(name)) {
                continue;
            }

            if (ident.getKey().length() == name.length()) {
                res = true;
                break;
            }

            if (ident.getKey().charAt(name.length()) == '.' || ident.getKey().charAt(name.length()) == '[') {
                res = true;
                break;
            }
        }

        current.identifyPrefixAvailable.put(name, res);
        return res;
    }

    @Override
    public HashMap<String, String> getMacros() {
        return macros;
    }

    @Override
    public boolean hasCurrentDataGrid() {
        if (null == current) {
            return false;
        }

        return current.currentDataGrid != null;
    }

    @Override
    public String getCurrentTableName() {
        if (null == current) {
            return "";
        }

        if (null != current.userModuleTable) {
            return current.userModuleTable.getSheetName();
        }

        if (null != current.customTableIndex) {
            return current.customTableIndex.getSheetName();
        }

        return "";
    }

    @Override
    public String getCurrentFileName() {
        if (null == current) {
            return "";
        }

        if (null == current.getDataSource().filePath) {
            return "";
        }

        return current.getDataSource().filePath;
    }

    @Override
    public LinkedList<IdentifyDescriptor> getMappedColumns() {
        if (null == current) {
            return null;
        }

        return current.indexMapping;
    }
}
