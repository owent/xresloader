package org.xresloader.core.engine;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.data.vfy.DataVerifyImpl;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.util.IOUtils;
import org.apache.commons.collections4.map.LRUMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by owentou on 2014/10/9.
 */
public class ExcelEngine {
    static private Pattern checkDate = Pattern.compile("[/\\-\\.]");
    static private Pattern checkTime = Pattern.compile(":");

    /**
     * 开启的workbook缓存，减少打开和分析文件的耗时
     */
    static private HashMap<String, Workbook> openedWorkbooks = new HashMap<String, Workbook>();

    /**
     * 开启的自定义索引缓存，减少打开和分析文件的耗时
     */
    static private LRUMap<String, CustomDataTableIndex> openedCustomDataTableIndex = new LRUMap<>();
    static private int openedCustomDataTableRows = 0;

    /**
     * 日期格式列缓存，XSSF在获取Style时性能极其低下，缓存一下有助于提升性能 导致的副作用就是只接受第一个数据行的日期格式
     */
    // static private HashMap<Integer, SimpleDateFormat> dateTypeStyle = new
    // HashMap<Integer, SimpleDateFormat>();

    static public class FormulaWrapper {
        public FormulaEvaluator evalor = null;

        public FormulaWrapper(FormulaEvaluator evalor) {
            this.evalor = evalor;
        }
    }

    static public class CustomDataRowIndex {
        private ArrayList<String> columns = new ArrayList<String>();
        private int rowNumber = 0;
        private CustomDataTableIndex ownerSheet = null;

        public CustomDataRowIndex(int rowNum, CustomDataTableIndex owner) {
            this.rowNumber = rowNum;
            this.ownerSheet = owner;
        }

        public ArrayList<String> getColumns() {
            return columns;
        }

        public int getColumnSize() {
            return columns.size();
        }

        public String getCellValue(int index) {
            if (index >= columns.size()) {
                return null;
            }

            String ret = columns.get(index);
            if (ret != null) {
                return ret.trim();
            }
            return ret;
        }

        public int getRowNum() {
            return rowNumber;
        }

        public CustomDataTableIndex getTable() {
            return ownerSheet;
        }
    }

    static public class CustomDataTableIndex {
        private String filePath = null;
        private String sheetName = null;
        private int lastRowNum = -1;

        private HashMap<Integer, CustomDataRowIndex> rows = new HashMap<Integer, CustomDataRowIndex>();

        public CustomDataTableIndex(String file_path, String sheet_name) {
            this.filePath = file_path;
            this.sheetName = sheet_name;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getSheetName() {
            return sheetName;
        }

        public int getLastRowNum() {
            return lastRowNum;
        }

        public void addRow(CustomDataRowIndex row) {
            if (row == null) {
                return;
            }

            rows.put(row.getRowNum(), row);
            if (row.getRowNum() > lastRowNum) {
                lastRowNum = row.getRowNum();
            }
        }

        public CustomDataRowIndex getRow(int index) {
            return rows.getOrDefault(index, null);
        }
    }

    static public class DataRowWrapper {
        private Row userModuleRow = null;
        private CustomDataRowIndex customRow = null;

        public DataRowWrapper(Row row) {
            this.userModuleRow = row;
        }

        public DataRowWrapper(CustomDataRowIndex row) {
            this.customRow = row;
        }

        public Row getUserModuleRow() {
            return userModuleRow;
        }

        public CustomDataRowIndex getCustomRowIndex() {
            return customRow;
        }

        public boolean isValid() {
            return userModuleRow != null || customRow != null;
        }

        public int getLastCellNum() {
            if (null != userModuleRow) {
                return userModuleRow.getLastCellNum();
            }
            if (null != customRow) {
                return customRow.getColumnSize() - 1;
            }
            return -1;
        }

        public int getRowNum() {
            if (null != userModuleRow) {
                return userModuleRow.getRowNum();
            }
            if (null != customRow) {
                return customRow.getRowNum();
            }
            return -1;
        }
    }

    /**
     * 清空缓存
     */
    static public void clearAllCache() {
        // dateTypeStyle.clear();
    }

    /**
     * 获取列名称
     * 
     * @param column 列号(从1开始)
     */
    static public String getColumnName(int column) {
        if (column <= 0) {
            return "UNKNOWN";
        }

        --column;
        String ret = String.valueOf((char) (column % 26) + 'A');
        while (column >= 26) {
            column /= 26;
            ret = String.valueOf((char) (column % 26) + 'A') + ret;
        }

        return ret;
    }

    /**
     * 打开Excel文件
     *
     * @param file_path 文件路径
     * @return Excel Workbook对象
     */
    static public Workbook openWorkbook(File file) {
        // 无论打开什么Excel文件，都要清空缓存
        clearAllCache();

        String file_path = null;
        try {
            file_path = file.getCanonicalPath();
        } catch (IOException e) {
            ProgramOptions.getLoger().error("%s", e.getMessage());
            file_path = file.getPath();
        }

        Workbook ret = openedWorkbooks.get(file_path);
        if (null != ret) {
            return ret;
        }

        FileInputStream is = null;
        try {
            is = new FileInputStream(file_path);

            // 类型枚举，以后能支持 ods等非微软格式？
            if (file_path.toLowerCase().endsWith(".xls")) {
                ret = new HSSFWorkbook(is);
                org.apache.poi.hssf.extractor.ExcelExtractor extractor = new org.apache.poi.hssf.extractor.ExcelExtractor(
                        (HSSFWorkbook) ret);
                extractor.setFormulasNotResults(false);
                extractor.close();
            } else {
                ret = new XSSFWorkbook(is);
                org.apache.poi.xssf.extractor.XSSFExcelExtractor extractor = new org.apache.poi.xssf.extractor.XSSFExcelExtractor(
                        (XSSFWorkbook) ret);
                extractor.setFormulasNotResults(false);
                extractor.close();
            }

            openedWorkbooks.put(file_path, ret);
        } catch (java.io.IOException e) {
            ProgramOptions.getLoger().error("Open file % failed, %s", file.getPath(), e.getMessage());
        } catch (POIXMLException e) {
            ProgramOptions.getLoger().error("Open and unpack file % failed, %s", file.getPath(), e.getMessage());
            for (var stack : e.getStackTrace()) {
                ProgramOptions.getLoger().error("\t%s.%s(%s:%d)", stack.getClassName(), stack.getMethodName(),
                        stack.getFileName(),
                        stack.getLineNumber());
            }
        } catch (RuntimeException e) {
            ProgramOptions.getLoger().error("Open file % failed, %s", file.getPath(), e.getMessage());
        }

        return ret;
    }

    /**
     * 打开工作簿
     *
     * @param file_path  Excel文件
     * @param sheet_name 表名
     * @return Sheet对象
     */
    static public Sheet openUserModuleSheet(File file, String sheet_name) {
        Workbook wb = openWorkbook(file);
        if (null == wb)
            return null;

        return wb.getSheet(sheet_name);
    }

    static public CustomDataTableIndex openStreamTableIndex(File file, String sheet_name) {
        String realPath = null;
        try {
            realPath = file.getCanonicalPath().replaceAll("\\\\", "/");
        } catch (Exception _e) {
        }
        try {
            if (realPath == null) {
                realPath = file.getAbsolutePath().replaceAll("\\\\", "/");
            }
        } catch (Exception _e) {
        }
        if (realPath == null) {
            realPath = file.getPath().replaceAll("\\\\", "/");
        }
        CustomDataTableIndex ret;
        String lruCacheKey = String.format("%s|%s", realPath, sheet_name);
        ret = openedCustomDataTableIndex.getOrDefault(lruCacheKey, null);
        if (ret != null) {
            return ret;
        }

        if (file.getName().toLowerCase().endsWith(".xls")) {
            ret = ExcelHSSFStreamSheetHandle.buildCustomTableIndex(file, sheet_name);
        } else {
            ret = ExcelXSSFStreamSheetHandle.buildCustomTableIndex(file, sheet_name);
        }

        if (ret != null) {
            int maxCacheRows = ProgramOptions.getInstance().dataSourceLruCacheRows;
            if (maxCacheRows <= 0) {
                maxCacheRows = Integer.MAX_VALUE;
            }
            if (ret.getLastRowNum() + 1 < maxCacheRows) {
                openedCustomDataTableIndex.put(lruCacheKey, ret);
                openedCustomDataTableRows += ret.getLastRowNum() + 1;

                while (openedCustomDataTableRows >= maxCacheRows) {
                    Map.Entry<String, CustomDataTableIndex> first = openedCustomDataTableIndex.entrySet().iterator()
                            .next();
                    openedCustomDataTableRows -= first.getValue().getLastRowNum() + 1;
                    openedCustomDataTableIndex.remove(first.getKey());
                }
            }
        }
        return ret;
    }

    static public String tryMacro(String m) {
        if (m == null || m.isEmpty()) {
            return m;
        }

        if (null == DataSrcImpl.getOurInstance())
            return m;

        HashMap<String, String> hm = DataSrcImpl.getOurInstance().getMacros();
        if (null == hm)
            return m;

        return hm.getOrDefault(m, m);
    }

    /**
     * 单元格数据转换（String）
     *
     * @param rowWrapper 行
     * @param col        列号
     * @return
     */
    static public void cell2s(DataContainer<String> out, DataRowWrapper rowWrapper, IdentifyDescriptor col) {
        cell2s(out, rowWrapper, col, null);
    }

    static private byte cal_cell2err(Cell c, CellValue cv) {
        if (null == cv) {
            return c.getErrorCellValue();
        }

        return cv.getErrorValue();
    }

    static private double cal_cell2num(Cell c, CellValue cv) {
        if (null == cv) {
            return c.getNumericCellValue();
        }

        return cv.getNumberValue();
    }

    static private String cal_cell2str(Cell c, CellValue cv) {
        if (null == cv) {
            return c.getStringCellValue();
        }

        return cv.getStringValue();
    }

    static private Boolean cal_cell2bool(Cell c, CellValue cv) {
        if (null == cv) {
            return c.getBooleanCellValue();
        }

        return cv.getBooleanValue();
    }

    /**
     * 单元格数据转换（String）
     *
     * @param rowWrapper 行
     * @param col        列号
     * @param formula    公式管理器
     * @return
     */
    static public void cell2s(DataContainer<String> out, DataRowWrapper rowWrapper, IdentifyDescriptor col,
            FormulaWrapper formula) {
        if (null == rowWrapper) {
            return;
        }

        if (null != rowWrapper.getCustomRowIndex()) {
            String val = rowWrapper.getCustomRowIndex().getCellValue(col.index);
            if (val != null && !val.isEmpty()) {
                if (DataSrcImpl.getOurInstance().isInitialized() && ProgramOptions.getInstance().enableStringMacro) {
                    out.set(tryMacro(val));
                } else {
                    out.set(val);
                }
            }
            return;
        }

        Row row = rowWrapper.getUserModuleRow();
        if (null == row) {
            return;
        }

        Cell c = row.getCell(col.index);
        if (null == c) {
            return;
        }

        CellValue cv = null;
        if (CellType.FORMULA == c.getCellType()) {
            if (null != formula && null != formula.evalor) {
                try {
                    cv = formula.evalor.evaluate(c);
                } catch (NotImplementedException e) {
                    ProgramOptions.getLoger().warn(
                            "Formular has unsupported function(s).We will use cached data.Consider using --disable-excel-formular?%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > Formular Content: %s",
                            ProgramOptions.getEndl(), DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1), ProgramOptions.getEndl(),
                            c.getCellFormula());
                    cv = null;
                } catch (Exception e) {
                    ProgramOptions.getLoger().warn(
                            "Evaluate formular failed: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1));
                    cv = null;
                }
            } else {
                if (DataSrcImpl.getOurInstance().isInitialized() && ProgramOptions.getInstance().enableStringMacro) {
                    out.set(tryMacro(c.toString()));
                } else {
                    out.set(c.toString());
                }
                return;
            }
        }

        CellType type;
        if (null != cv) {
            type = cv.getCellType();
        } else if (c.getCellType() == CellType.FORMULA) {
            type = c.getCachedFormulaResultType();
        } else {
            type = c.getCellType();
        }

        switch (type) {
            case BLANK:
                break;
            case BOOLEAN:
                out.set(cal_cell2bool(c, cv).toString());
                break;
            case ERROR: {
                byte error_code = cal_cell2err(c, cv);
                try {
                    out.set(FormulaError.forInt(error_code).getString());
                } catch (IllegalArgumentException e) {
                    out.set(e.getMessage());
                }
                break;
            }
            case FORMULA:
                if (null == cv) {
                    out.set(c.getCellFormula());
                }
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(c)) {
                    // 参照POI DateUtil.isADateFormat函数，去除无效字符
                    String fs = c.getCellStyle().getDataFormatString().replaceAll("\\\\-", "-").replaceAll("\\\\,", ",")
                            .replaceAll("\\\\\\.", ".").replaceAll("\\\\ ", " ").replaceAll("AM/PM", "")
                            .replaceAll("\\[[^]]*\\]", "");

                    // 默认格式
                    int idx = fs.indexOf(";@");
                    if (idx > 0 && idx < fs.length()) {
                        // 包含年月日
                        LinkedList<String> rfs = new LinkedList<String>();

                        if (checkDate.matcher(fs).find())
                            rfs.addLast("yyyy-MM-dd");

                        if (checkTime.matcher(fs).find())
                            rfs.addLast("HH:mm:ss");

                        if (rfs.isEmpty())
                            fs = "yyyy-MM-dd HH:mm:ss";
                        else
                            fs = String.join(" ", rfs);

                    } else {
                        idx = fs.indexOf(";");
                        if (idx > 0 && idx < fs.length() - 1) {
                            fs = fs.substring(0, idx);
                        }
                    }

                    SimpleDateFormat df = new SimpleDateFormat(fs);
                    out.set(df.format(c.getDateCellValue()).trim());
                    break;
                }

                double dv = cal_cell2num(c, cv);
                if (col.getRatio() != 1) {
                    dv = dv * col.getRatio();
                }
                if (dv == (long) dv) {
                    out.set(String.format("%d", (long) dv));
                } else {
                    out.set(String.format("%s", dv));
                }
                break;
            case STRING:
                String val = cal_cell2str(c, cv).trim();
                if (!val.isEmpty()) {
                    // Const 和 option导出时，没有数据源，也不需要文本/宏替换
                    DataSrcImpl data_source = DataSrcImpl.getOurInstance();
                    if (null != data_source && data_source.isInitialized()
                            && ProgramOptions.getInstance().enableStringMacro) {
                        out.set(tryMacro(val));
                    } else {
                        out.set(val);
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * 单元格数据转换（Integer）
     *
     * @param rowWrapper 行
     * @param col        列号
     * @return
     */
    static public void cell2i(DataContainer<Long> out, DataRowWrapper rowWrapper, IdentifyDescriptor col)
            throws ConvException {
        cell2i(out, rowWrapper, col, null);
    }

    /**
     * 单元格数据转换（Integer）
     *
     * @param rowWrapper 行
     * @param col        列号
     * @param formula    公式管理器
     * @return
     */
    static public void cell2i(DataContainer<Long> out, DataRowWrapper rowWrapper, IdentifyDescriptor col,
            FormulaWrapper formula) throws ConvException {

        if (null == rowWrapper) {
            return;
        }

        if (null != rowWrapper.getCustomRowIndex()) {
            String val = rowWrapper.getCustomRowIndex().getCellValue(col.index);
            if (val != null && !val.isEmpty()) {
                out.set(DataVerifyImpl.getAndVerifyToLong(col.getValidator(), col.name, tryMacro(val)));
            }
            return;
        }

        Row row = rowWrapper.getUserModuleRow();
        if (null == row) {
            return;
        }

        Cell c = row.getCell(col.index);
        if (null == c) {
            return;
        }

        CellValue cv = null;
        if (CellType.FORMULA == c.getCellType()) {
            if (null != formula && null != formula.evalor)
                try {
                    cv = formula.evalor.evaluate(c);
                } catch (NotImplementedException e) {
                    ProgramOptions.getLoger().warn(
                            "Formular has unsupported function(s).We will use cached data.Consider using --disable-excel-formular?%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > Formular Content: %s",
                            ProgramOptions.getEndl(), DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1), ProgramOptions.getEndl(),
                            c.getCellFormula());
                    cv = null;
                } catch (Exception e) {
                    ProgramOptions.getLoger().warn(
                            "Evaluate formular failed: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1));
                    cv = null;
                }
            else
                return;
        }

        CellType type;
        if (null != cv) {
            type = cv.getCellType();
        } else if (c.getCellType() == CellType.FORMULA) {
            type = c.getCachedFormulaResultType();
        } else {
            type = c.getCellType();
        }
        switch (type) {
            case BLANK:
                break;
            case BOOLEAN: {
                boolean res = cal_cell2bool(c, cv);
                out.set(DataVerifyImpl.getAndVerify(col.getValidator(), col.name, res ? 1 : 0));
                break;
            }
            case ERROR: {
                byte error_code = cal_cell2err(c, cv);
                try {
                    ProgramOptions.getLoger().warn(
                            "Error formula: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            FormulaError.forInt(error_code).getString(), ProgramOptions.getEndl(),
                            DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1));
                } catch (IllegalArgumentException e) {
                    ProgramOptions.getLoger().warn(
                            "Error or unsupported cell value: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1));
                }
                break;
            }
            case FORMULA:
                break;
            case NUMERIC: {
                long val = 0;
                if (DateUtil.isCellDateFormatted(c)) {
                    val = dateToUnixTimestamp(c.getDateCellValue());
                } else {
                    if (col.getRatio() == 1) {
                        val = Math.round(cal_cell2num(c, cv));
                    } else {
                        val = Math.round(cal_cell2num(c, cv) * col.getRatio());
                    }
                }

                out.set(DataVerifyImpl.getAndVerify(col.getValidator(), col.name, val));
                break;
            }
            case STRING: {
                String val = cal_cell2str(c, cv).trim();
                if (val.isEmpty()) {
                    break;
                }

                out.set(DataVerifyImpl.getAndVerifyToLong(col.getValidator(), col.name, tryMacro(val)));
                break;
            }
            default:
                break;
        }
    }

    /**
     * 单元格数据转换（Double）
     *
     * @param rowWrapper 行
     * @param col        列号
     * @return
     */
    static public void cell2d(DataContainer<Double> out, DataRowWrapper rowWrapper, IdentifyDescriptor col)
            throws ConvException {
        cell2d(out, rowWrapper, col, null);
    }

    /**
     * 单元格数据转换（Double）
     *
     * @param rowWrapper 行
     * @param col        列号
     * @param formula    公式管理器
     * @return
     */
    static public void cell2d(DataContainer<Double> out, DataRowWrapper rowWrapper, IdentifyDescriptor col,
            FormulaWrapper formula) throws ConvException {

        if (null == rowWrapper) {
            return;
        }

        if (null != rowWrapper.getCustomRowIndex()) {
            String val = rowWrapper.getCustomRowIndex().getCellValue(col.index);

            if (val != null && !val.isEmpty()) {
                try {
                    out.set(DataVerifyImpl.getAndVerifyToDouble(col.getValidator(), col.name, tryMacro(val)));
                } catch (java.lang.NumberFormatException e) {
                    throw new ConvException(
                            String.format("Table %s, Row %d, Column %d : %s can not be converted to a number",
                                    rowWrapper.getCustomRowIndex().getTable().getSheetName(),
                                    rowWrapper.getCustomRowIndex().getRowNum() + 1, col.index + 1, val));
                }
            }
            return;
        }

        Row row = rowWrapper.getUserModuleRow();
        if (null == row) {
            return;
        }

        Cell c = row.getCell(col.index);
        if (null == c) {
            return;
        }

        CellValue cv = null;
        if (CellType.FORMULA == c.getCellType()) {
            if (null != formula && null != formula.evalor) {
                try {
                    cv = formula.evalor.evaluate(c);
                } catch (NotImplementedException e) {
                    ProgramOptions.getLoger().warn(
                            "Formular has unsupported function(s).We will use cached data.Consider using --disable-excel-formular?%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > Formular Content: %s",
                            ProgramOptions.getEndl(), DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1), ProgramOptions.getEndl(),
                            c.getCellFormula());
                    cv = null;
                } catch (Exception e) {
                    ProgramOptions.getLoger().warn(
                            "Evaluate formular failed: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1));
                    cv = null;
                }
            } else {
                return;
            }
        }

        CellType type;
        if (null != cv) {
            type = cv.getCellType();
        } else if (c.getCellType() == CellType.FORMULA) {
            type = c.getCachedFormulaResultType();
        } else {
            type = c.getCellType();
        }
        switch (type) {
            case BLANK:
                break;
            case BOOLEAN:
                out.set(cal_cell2bool(c, cv) ? 1.0 : 0.0);
                break;
            case ERROR: {
                byte error_code = cal_cell2err(c, cv);
                try {
                    ProgramOptions.getLoger().warn(
                            "Error formula: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            FormulaError.forInt(error_code).getString(), ProgramOptions.getEndl(),
                            DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1));
                } catch (IllegalArgumentException e) {
                    ProgramOptions.getLoger().warn(
                            "Error or unsupported cell value: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1));
                }
                break;
            }
            case FORMULA:
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(c)) {
                    out.set((double) dateToUnixTimestamp(c.getDateCellValue()));
                    break;
                }
                if (col.getRatio() == 1) {
                    out.set(cal_cell2num(c, cv));
                } else {
                    out.set(cal_cell2num(c, cv) * col.getRatio());
                }
                break;
            case STRING: {
                String val = cal_cell2str(c, cv).trim();
                if (val.isEmpty()) {
                    break;
                }

                try {
                    out.set(DataVerifyImpl.getAndVerifyToDouble(col.getValidator(), col.name, tryMacro(val)));
                } catch (java.lang.NumberFormatException e) {
                    throw new ConvException(
                            String.format("Table %s, Row %d, Column %d : %s can not be converted to a number",
                                    row.getSheet().getSheetName(), c.getRowIndex() + 1, c.getColumnIndex() + 1, val));
                }
                break;
            }
            default:
                break;
        }
    }

    /**
     * 单元格数据转换（boolean）
     *
     * @param rowWrapper 行
     * @param col        列号
     * @return
     */
    static public void cell2b(DataContainer<Boolean> out, DataRowWrapper rowWrapper, IdentifyDescriptor col) {
        cell2b(out, rowWrapper, col, null);
    }

    /**
     * 单元格数据转换（boolean）
     *
     * @param rowWrapper 行
     * @param col        列号
     * @param formula    公式管理器
     * @return
     */
    static public void cell2b(DataContainer<Boolean> out, DataRowWrapper rowWrapper, IdentifyDescriptor col,
            FormulaWrapper formula) {
        if (null == rowWrapper) {
            return;
        }

        if (null != rowWrapper.getCustomRowIndex()) {
            String val = rowWrapper.getCustomRowIndex().getCellValue(col.index);
            if (val != null && !val.isEmpty()) {
                out.set(DataSrcImpl.getBooleanFromString(tryMacro(val)));
            }
            return;
        }

        Row row = rowWrapper.getUserModuleRow();
        if (null == row) {
            return;
        }

        Cell c = row.getCell(col.index);
        if (null == c) {
            return;
        }

        CellValue cv = null;
        if (CellType.FORMULA == c.getCellType()) {
            if (null != formula && null != formula.evalor) {
                try {
                    cv = formula.evalor.evaluate(c);
                } catch (NotImplementedException e) {
                    ProgramOptions.getLoger().warn(
                            "Formular has unsupported function(s).We will use cached data.Consider using --disable-excel-formular?%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > Formular Content: %s",
                            ProgramOptions.getEndl(), DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1), ProgramOptions.getEndl(),
                            c.getCellFormula());
                    cv = null;
                } catch (Exception e) {
                    ProgramOptions.getLoger().warn(
                            "Evaluate formular failed: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1));
                    cv = null;
                }
            } else {
                out.set(true);
                return;
            }
        }

        CellType type;
        if (null != cv) {
            type = cv.getCellType();
        } else if (c.getCellType() == CellType.FORMULA) {
            type = c.getCachedFormulaResultType();
        } else {
            type = c.getCellType();
        }
        switch (type) {
            case BLANK:
                break;
            case BOOLEAN:
                out.set(cal_cell2bool(c, cv));
                break;
            case ERROR: {
                byte error_code = cal_cell2err(c, cv);
                try {
                    ProgramOptions.getLoger().warn(
                            "Error formula: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            FormulaError.forInt(error_code).getString(), ProgramOptions.getEndl(),
                            DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1));
                } catch (IllegalArgumentException e) {
                    ProgramOptions.getLoger().warn(
                            "Error or unsupported cell value: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName(), row.getRowNum() + 1,
                            c.getRowIndex() + 1, getColumnName(c.getRowIndex() + 1));
                }
                break;
            }
            case FORMULA:
                break;
            case NUMERIC:
                out.set(cal_cell2num(c, cv) != 0 && col.getRatio() != 0);
                break;
            case STRING:
                String item = tryMacro(cal_cell2str(c, cv).trim()).toLowerCase();
                if (item.isEmpty()) {
                    break;
                }

                out.set(DataSrcImpl.getBooleanFromString(item));
                break;
            default:
                break;
        }
    }

    static private long dateToUnixTimestamp(Date d) {
        if (null == d) {
            return 0;
        }

        Calendar c = new GregorianCalendar();
        c.setTime(d);
        int y = c.get(Calendar.YEAR);
        // @see Date.getYear();
        // unix timstamp时间搓是负数的都认为日期无效，仅时间有效
        if (y <= 1970) {
            // int day = c.get(Calendar.DAY_OF_YEAR);
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            int s = c.get(Calendar.SECOND);
            return h * 3600 + m * 60 + s;
        }
        return d.getTime() / 1000;
    }

    static public void setMaxByteArraySize(int maxSize) {
        IOUtils.setByteArrayMaxOverride(maxSize);
    }
}
