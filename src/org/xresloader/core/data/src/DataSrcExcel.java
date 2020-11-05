package org.xresloader.core.data.src;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.engine.IdentifyDescriptor;
import org.xresloader.core.engine.IdentifyEngine;
import org.xresloader.core.scheme.SchemeConf;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.model.StylesTable;

import org.apache.poi.util.XMLHelper;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import java.io.InputStream;

/**
 * Created by owentou on 2014/10/9.
 */
public class DataSrcExcel extends DataSrcImpl {

    private class DataSheetInfo {
        public String fileName = "";
        public Sheet userModuleTable = null;
        public ExcelEngine.CustomDataTableIndex customTableIndex = null;
        public ExcelEngine.FormulaWrapper formula = null;
        public ExcelEngine.DataRowWrapper currentRow = null;
        public int nextIndex = 0;
        public int lastRowNumber = -1;
        public HashMap<String, IdentifyDescriptor> nameMapping = new HashMap<String, IdentifyDescriptor>();
        public LinkedList<IdentifyDescriptor> indexMapping = new LinkedList<IdentifyDescriptor>();
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

    private class XSSFStreamSheetHandle implements XSSFSheetXMLHandler.SheetContentsHandler {
        private int maxRowNumber = 0;
        private int maxColumnNumber = 0;
        private int currentRow = -1;
        private int currentCol = -1;
        private ExcelEngine.CustomDataRowIndex currentRowIndex = null;
        private ExcelEngine.CustomDataTableIndex currentTableIndex = null;

        public XSSFStreamSheetHandle(ExcelEngine.CustomDataTableIndex table) {
            if (table != null) {
                currentTableIndex = table;
            } else {
                currentTableIndex = new ExcelEngine.CustomDataTableIndex("", "");
            }
        }

        @Override
        public void startRow(int rowNum) {
            if (this.maxRowNumber < rowNum) {
                this.maxRowNumber = rowNum;
            }
            this.currentRow = rowNum;
            this.currentCol = -1;
            this.currentRowIndex = new ExcelEngine.CustomDataRowIndex(rowNum, currentTableIndex);
            this.currentRowIndex.getColumns().ensureCapacity(this.maxColumnNumber + 1);
        }

        @Override
        public void endRow(int rowNum) {
            if (currentTableIndex != null) {
                currentTableIndex.addRow(currentRowIndex);
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            // gracefully handle missing CellRef here in a similar way as XSSFCell does
            if (cellReference == null) {
                cellReference = new CellAddress(currentRow, currentCol).formatAsString();
            }

            // Did we miss any cells?
            int thisCol = (new CellReference(cellReference)).getCol();
            while (currentRowIndex.getColumns().size() <= thisCol) {
                currentRowIndex.getColumns().add(null);
            }
            currentCol = thisCol;
            if (maxColumnNumber < currentCol) {
                maxColumnNumber = currentCol;
            }

            currentRowIndex.getColumns().set(thisCol, formattedValue);
        }
    }

    /***
     * macro表cache
     */
    static private class MacroCacheInfo {
        /*** file_path:key->value ***/
        private HashMap<String, MacroFileCache> cache = new HashMap<String, MacroFileCache>();
        private HashMap<String, String> empty = new HashMap<String, String>(); // 空项特殊处理
    }

    static private MacroCacheInfo macro_cache = new MacroCacheInfo();

    /***
     * 构建macro表cache，由于macro表大多数情况下都一样，所以加缓存优化
     */
    HashMap<String, String> init_macro_with_cache(List<SchemeConf.DataInfo> src_list) {
        LinkedList<HashMap<String, String>> data_filled = new LinkedList<HashMap<String, String>>();

        IdentifyDescriptor column_ident = new IdentifyDescriptor();

        // 枚举所有macro表信息
        for (SchemeConf.DataInfo src : src_list) {
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
                    ProgramOptions.getLoger().warn(
                            "try to open macro source \"%s:%s\" (row=%d,col=%d) but already has cache \"%s:%s\" (row=%d,col=%d). the old macros will be covered",
                            file_path, src.table_name, src.data_row, src.data_col, res.file.file_path,
                            res.file.table_name, res.file.data_row, res.file.data_col);
                }
            }
            res = new MacroFileCache(src, file_path);

            if (file_path.isEmpty() || src.table_name.isEmpty() || src.data_col <= 0 || src.data_row <= 0) {
                ProgramOptions.getLoger().warn("macro source \"%s\" (%s:%d，%d) ignored.", file_path, src.table_name,
                        src.data_row, src.data_col);
                continue;
            }

            Sheet tb = ExcelEngine.openSheet(file_path, src.table_name);
            if (null == tb) {
                ProgramOptions.getLoger().warn("open macro source \"%s\" or sheet %s failed.", file_path,
                        src.table_name);
                continue;
            }

            ExcelEngine.FormulaWrapper formula = new ExcelEngine.FormulaWrapper(
                    tb.getWorkbook().getCreationHelper().createFormulaEvaluator());

            int row_num = tb.getLastRowNum() + 1;
            for (int i = src.data_row - 1; i < row_num; ++i) {
                ExcelEngine.DataRowWrapper rowWrapper = new ExcelEngine.DataRowWrapper(tb.getRow(i));
                column_ident.index = src.data_col - 1;
                DataContainer<String> data_cache = getStringCache("");
                ExcelEngine.cell2s(data_cache, rowWrapper, column_ident);
                String key = data_cache.get();

                column_ident.index = src.data_col;
                data_cache = getStringCache("");
                ExcelEngine.cell2s(data_cache, rowWrapper, column_ident, formula);

                String val = data_cache.get();
                if (null != key && null != val && !key.isEmpty() && !val.isEmpty()) {
                    if (res.macros.containsKey(key)) {
                        ProgramOptions.getLoger().warn("macro key \"%s\" is used more than once.", key);
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

        HashMap<String, String> ret = new HashMap<String, String>();
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
        for (SchemeConf.DataInfo src : scfg.getDataSource()) {
            if (false == src.file_path.isEmpty()) {
                file_path = src.file_path;
            }

            if (file_path.isEmpty() || src.table_name.isEmpty() || src.data_col <= 0 || src.data_row <= 0) {
                ProgramOptions.getLoger().error("data source file \"%s\" (%s:%d，%d) ignored.", src.file_path,
                        src.table_name, src.data_row, src.data_col);
                continue;
            }

            DataSheetInfo res = new DataSheetInfo();
            // XLSX 可以使用流式读取引擎
            if (false == ProgramOptions.getInstance().enableFormular && false == file_path.endsWith(".xls")) {
                try (OPCPackage xlsx_package = OPCPackage.open(file_path, PackageAccess.READ)) {
                    ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(xlsx_package);
                    XSSFReader xssf_reader = new XSSFReader(xlsx_package);
                    StylesTable styles = xssf_reader.getStylesTable();
                    XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssf_reader.getSheetsData();
                    while (iter.hasNext() && res.customTableIndex == null) {
                        try (InputStream stream = iter.next()) {
                            String sheet_name = iter.getSheetName();
                            if (!sheet_name.equals(src.table_name)) {
                                continue;
                            }

                            ExcelEngine.CustomDataTableIndex tableIndex = new ExcelEngine.CustomDataTableIndex(
                                    file_path, sheet_name);

                            DataFormatter formatter = new DataFormatter();
                            InputSource sheet_source = new InputSource(stream);
                            try {
                                XMLReader sheet_parser = XMLHelper.newXMLReader();
                                ContentHandler handler = new XSSFSheetXMLHandler(styles, null, strings,
                                        new XSSFStreamSheetHandle(tableIndex), formatter, false);
                                sheet_parser.setContentHandler(handler);
                                sheet_parser.parse(sheet_source);

                                res.customTableIndex = tableIndex;
                            } catch (org.xml.sax.SAXException e) {
                                ProgramOptions.getLoger().error(
                                        "Open source file \"%s\" and parse sheet \"%s\" failed, SAX engine appears to be broken - %s.",
                                        src.file_path, src.table_name, e.getMessage());
                            } catch (ParserConfigurationException e) {
                                ProgramOptions.getLoger().error(
                                        "Open source file \"%s\" and parse sheet \"%s\" failed, SAX parser appears to be broken - %s.",
                                        src.file_path, src.table_name, e.getMessage());
                            } catch (java.io.IOException e) {
                                ProgramOptions.getLoger().error(
                                        "Open source file \"%s\" and parse sheet \"%s\" failed, %s.", src.file_path,
                                        src.table_name, e.getMessage());
                            }
                        }
                    }
                } catch (org.apache.poi.openxml4j.exceptions.OpenXML4JException e) {
                    ProgramOptions.getLoger().error(
                            "Open source file \"%s\" and parse sheet \"%s\" failed, OpenXML4J engine appears to be broken - %s.",
                            src.file_path, src.table_name, e.getMessage());
                } catch (org.xml.sax.SAXException e) {
                    ProgramOptions.getLoger().error(
                            "Open source file \"%s\" and parse sheet \"%s\" failed, SAX engine appears to be broken - %s.",
                            src.file_path, src.table_name, e.getMessage());
                } catch (java.io.IOException e) {
                    ProgramOptions.getLoger().error("Open source file \"%s\" and parse sheet \"%s\" failed, %s.",
                            src.file_path, src.table_name, e.getMessage());
                }

                if (res.customTableIndex != null) {
                    res.lastRowNumber = res.customTableIndex.getLastRowNum();
                } else {
                    ProgramOptions.getLoger().error("Open data source file \"%s\" or sheet \"%s\" failed.",
                            src.file_path, src.table_name);
                    continue;
                }
            } else {
                res.userModuleTable = ExcelEngine.openSheet(file_path, src.table_name);
                if (null == res.userModuleTable) {
                    ProgramOptions.getLoger().error("Open data source file \"%s\" or sheet \"%s\" failed.",
                            src.file_path, src.table_name);
                    continue;
                }
                res.lastRowNumber = res.userModuleTable.getLastRowNum();

                // 公式支持
                res.formula = new ExcelEngine.FormulaWrapper(
                        res.userModuleTable.getWorkbook().getCreationHelper().createFormulaEvaluator());
            }

            // 根据第一个表建立名称关系表
            {
                int key_row = scfg.getKey().getRow() - 1;
                ExcelEngine.DataRowWrapper rowWrapper = null;
                if (null != res.userModuleTable) {
                    rowWrapper = new ExcelEngine.DataRowWrapper(res.userModuleTable.getRow(key_row));
                } else if (null != res.customTableIndex) {
                    rowWrapper = new ExcelEngine.DataRowWrapper(res.customTableIndex.getRow(key_row));
                }

                if (null == rowWrapper) {
                    ProgramOptions.getLoger().error("try to get description name of %s in \"%s\" row %d failed.",
                            src.table_name, src.file_path, key_row);
                    return -53;
                }

                for (int i = src.data_col - 1; i < rowWrapper.getLastCellNum() + 1; ++i) {
                    column_ident.index = i;
                    DataContainer<String> k = getStringCache("");
                    ExcelEngine.cell2s(k, rowWrapper, column_ident, res.formula);
                    IdentifyDescriptor ident = IdentifyEngine.n2i(k.get(), i);
                    res.nameMapping.put(ident.name, ident);
                    res.indexMapping.add(ident);
                }
            }

            res.fileName = file_path;
            res.nextIndex = src.data_row - 1;
            tables.add(res);

            // 记录数量计数
            if (res.lastRowNumber > 0) {
                recordNumber += res.lastRowNumber - src.data_row + 2;
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

        while (true) {
            // 当前行超出
            if (current.nextIndex > current.lastRowNumber) {
                current.currentRow = null;
                return false;
            }

            if (null != current.userModuleTable) {
                current.currentRow = new ExcelEngine.DataRowWrapper(current.userModuleTable.getRow(current.nextIndex));
            } else if (null != current.customTableIndex) {
                current.currentRow = new ExcelEngine.DataRowWrapper(current.customTableIndex.getRow(current.nextIndex));
            } else {
                current.currentRow = null;
            }
            ++current.nextIndex;

            // 过滤空行
            if (null != current.currentRow && current.currentRow.isValid()) {
                break;
            }
        }

        return null != current && null != current.currentRow && current.currentRow.isValid();
    }

    @Override
    public DataContainer<Boolean> getValue(IdentifyDescriptor ident, boolean dv) throws ConvException {
        DataContainer<Boolean> ret = super.getValue(ident, dv);
        if (null == ident) {
            return ret;
        }

        ExcelEngine.cell2b(ret, current.currentRow, ident, current.formula);
        return ret;
    }

    @Override
    public DataContainer<String> getValue(IdentifyDescriptor ident, String dv) throws ConvException {
        DataContainer<String> ret = super.getValue(ident, dv);
        if (null == ident) {
            return ret;
        }

        ExcelEngine.cell2s(ret, current.currentRow, ident, current.formula);
        return ret;
    }

    @Override
    public DataContainer<Long> getValue(IdentifyDescriptor ident, long dv) throws ConvException {
        DataContainer<Long> ret = super.getValue(ident, dv);
        if (null == ident) {
            return ret;
        }

        ExcelEngine.cell2i(ret, current.currentRow, ident, current.formula);
        return ret;
    }

    @Override
    public DataContainer<Double> getValue(IdentifyDescriptor ident, double dv) throws ConvException {
        DataContainer<Double> ret = super.getValue(ident, dv);
        if (null == ident) {
            return ret;
        }

        ExcelEngine.cell2d(ret, current.currentRow, ident, current.formula);
        return ret;
    }

    @Override
    public int getRecordNumber() {
        return recordNumber;
    }

    @Override
    public IdentifyDescriptor getColumnByName(String _name) {
        return current.nameMapping.getOrDefault(_name, null);
    }

    @Override
    public HashMap<String, String> getMacros() {
        return macros;
    }

    @Override
    public int getCurrentRowNum() {
        if (null == current.currentRow) {
            return 0;
        }

        return current.currentRow.getRowNum();
    }

    @Override
    public String getCurrentTableName() {
        if (null == current.userModuleTable) {
            return "";
        }

        return current.userModuleTable.getSheetName();
    }

    @Override
    public String getCurrentFileName() {
        if (null == current.fileName) {
            return "";
        }

        return current.fileName;
    }

    @Override
    public LinkedList<IdentifyDescriptor> getColumns() {
        if (null == current) {
            return null;
        }

        return current.indexMapping;
    }
}
