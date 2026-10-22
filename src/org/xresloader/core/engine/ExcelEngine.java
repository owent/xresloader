package org.xresloader.core.engine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.data.vfy.DataVerifyImpl;
import org.xresloader.core.scheme.SchemeConf;

/**
 * Created by owentou on 2014/10/9.
 */
public class ExcelEngine {
    private static Pattern checkDate = Pattern.compile("[/\\-\\.]");
    private static Pattern checkTime = Pattern.compile(":");

    /**
     * 开启的workbook缓存，减少打开和分析文件的耗时
     */
    // POI workbooks/evaluators are mutable parser sessions, not shared IO data.
    private static final ThreadLocal<HashMap<String, Workbook>> openedWorkbooks = ThreadLocal.withInitial(HashMap::new);

    public static void clearThreadState() {
        for (Workbook workbook : openedWorkbooks.get().values()) {
            try {
                workbook.close();
            } catch (IOException exception) {
                ProgramOptions.getLogger().warn("Close workbook failed: %s", exception.getMessage());
            }
        }
        openedWorkbooks.remove();
    }

    /**
     * 开启的自定义索引缓存，减少打开和分析文件的耗时
     */
    private static final class CachedTable {
        final CustomDataTableIndex table;
        final AtomicLong lastAccess = new AtomicLong();

        CachedTable(CustomDataTableIndex table) {
            this.table = table;
        }
    }

    private record TableCacheKey(String filePath, String sheetName) {}

    private static final HashMap<TableCacheKey, CachedTable> openedCustomDataTableIndex = new HashMap<>();
    private static final class FileTableLoad {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        // Includes callers waiting for the file lock; guarded by tableCacheLock.
        int users;
    }

    private static final HashMap<String, FileTableLoad> pendingTableLoads = new HashMap<>();
    private static final ReentrantReadWriteLock tableCacheLock = new ReentrantReadWriteLock();
    private static final AtomicLong tableAccessSequence = new AtomicLong();
    private static long openedCustomDataTableRows = 0;

    /**
     * 日期格式列缓存，XSSF在获取Style时性能极其低下，缓存一下有助于提升性能 导致的副作用就是只接受第一个数据行的日期格式
     */
    // static private HashMap<Integer, SimpleDateFormat> dateTypeStyle = new
    // HashMap<Integer, SimpleDateFormat>();

    public static class FormulaWrapper {
        public FormulaEvaluator evalor = null;

        public FormulaWrapper(FormulaEvaluator evalor) {
            this.evalor = evalor;
        }
    }

    public static class CustomDataRowIndex {
        private ArrayList<String> columns = new ArrayList<>();
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

        public int getRowIndex() {
            return rowNumber;
        }

        public CustomDataTableIndex getTable() {
            return ownerSheet;
        }
    }

    public static class CustomDataTableIndex {
        private String filePath = null;
        private String sheetName = null;
        private int lastRowNum = -1;
        private int lastColumnNum = -1;

        private HashMap<Integer, CustomDataRowIndex> rows = new HashMap<>();

        public CustomDataTableIndex(String filePath, String sheetName) {
            this.filePath = filePath;
            this.sheetName = sheetName;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getSheetName() {
            return sheetName;
        }

        public int getLastRowIndex() {
            return lastRowNum;
        }

        public int getLastColumnIndex() {
            return lastColumnNum;
        }

        public void addRow(CustomDataRowIndex row) {
            if (row == null) {
                return;
            }

            rows.put(row.getRowIndex(), row);
            if (row.getRowIndex() > lastRowNum) {
                lastRowNum = row.getRowIndex();
            }
            if (row.getColumnSize() - 1 > lastColumnNum) {
                lastColumnNum = row.getColumnSize() - 1;
            }
        }

        public CustomDataRowIndex getRow(int index) {
            return rows.getOrDefault(index, null);
        }
    }

    public static class DataRowWrapper {
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

        public int getRowIndex() {
            if (null != userModuleRow) {
                return userModuleRow.getRowNum();
            }
            if (null != customRow) {
                return customRow.getRowIndex();
            }
            return -1;
        }
    }

    public static abstract class DataSheetWrapper {
        private final SchemeConf.DataInfo dataSource;

        public DataSheetWrapper(SchemeConf.DataInfo dataSource) {
            this.dataSource = dataSource;
        }

        public SchemeConf.DataInfo getDataSource() {
            return dataSource;
        }

        public abstract DataRowWrapper getRow(int keyRow);

        public abstract int getLastRowIndex();

        public abstract int getLastColumnIndex();
    }

    public static abstract class DataItemGridWrapper {
        boolean transpose;
        int dataItemIndex;

        /**
         * Constructor
         * 
         * @param transpose     是否转置
         * @param dataItemIndex 数据项索引（从0开始）
         */
        public DataItemGridWrapper(boolean transpose, int dataItemIndex) {
            this.transpose = transpose;
            this.dataItemIndex = dataItemIndex;
        }

        public boolean isTranspose() {
            return transpose;
        }

        public int getDataItemIndex() {
            return dataItemIndex;
        }

        /**
         * 获取最后一个数据字段索引（从0开始）
         * 
         * @return 最后一个数据字段索引
         */
        public abstract int getLastDataFieldIndex();

        /**
         * 获取自定义行索引单元格的值
         * 
         * @param itemGrid 数据网格(数据体)
         * @param ident    标识符(数据字段信息)
         * @return 自定义行索引单元格的值,如果不是自定义索引则返回null
         */
        public abstract String getCustomRowIndexCellValue(DataItemGridWrapper itemGrid, IdentifyDescriptor ident);

        /**
         * 获取POI索引单元格
         * 
         * @param itemGrid 数据网格(数据体)
         * @param ident    标识符(数据字段信息)
         * @return POI单元格,如果不是POI索引或数据不存在则返回null
         */
        public abstract Cell getUserModuleRowCell(DataItemGridWrapper itemGrid, IdentifyDescriptor ident);

        /**
         * 获取数据源信息
         * 
         * @return 数据源信息
         */
        public abstract SchemeConf.DataInfo getDataSource();

        /**
         * 获取原始行索引
         * 
         * @param dataFieldIndex 数据字段索引
         * @return 原始行索引
         */
        public int getOriginRowIndex(int dataFieldIndex) {
            if (this.transpose) {
                return dataFieldIndex;
            } else {
                return this.dataItemIndex;
            }
        }

        /**
         * 获取原始列索引
         * 
         * @param dataFieldIndex 数据字段索引
         * @return 原始列索引
         */
        public int getOriginColumnIndex(int dataFieldIndex) {
            if (this.transpose) {
                return this.dataItemIndex;
            } else {
                return dataFieldIndex;
            }
        }
    }

    /**
     * 清空缓存
     */
    public static void clearAllCache() {
        // dateTypeStyle.clear();
    }

    /**
     * 获取列名称
     * 
     * @param column 列号(从1开始)
     */
    public static String getColumnName(int column) {
        if (column <= 0) {
            return "UNKNOWN";
        }

        --column;
        String ret = String.valueOf((char) ((column % 26) + 'A'));
        while (column >= 26) {
            column /= 26;
            ret = String.valueOf((char) ((column % 26) + 'A')) + ret;
        }

        return ret;
    }

    /**
     * 打开Excel文件
     *
     * @param filePath 文件路径
     * @return Excel Workbook对象
     */
    public static Workbook openWorkbook(File file) {
        // 无论打开什么Excel文件，都要清空缓存
        clearAllCache();

        String filePath;
        try {
            filePath = file.getCanonicalPath();
        } catch (IOException e) {
            ProgramOptions.getLogger().error("%s", e.getMessage());
            filePath = file.getPath();
        }

        Workbook ret = openedWorkbooks.get().get(filePath);
        if (null != ret) {
            return ret;
        }

        try (InputStream input = InputFileCache.open(new File(filePath))) {
            // 类型枚举，以后能支持 ods等非微软格式？
            if (filePath.toLowerCase().endsWith(".xls")) {
                ret = new HSSFWorkbook(input);
            } else {
                ret = new XSSFWorkbook(input);
            }

            openedWorkbooks.get().put(filePath, ret);
        } catch (java.io.IOException e) {
            ProgramOptions.getLogger().error("Open file % failed, %s", file.getPath(), e.getMessage());
        } catch (POIXMLException e) {
            ProgramOptions.getLogger().error("Open and unpack file % failed, %s", file.getPath(), e.getMessage());
            for (var stack : e.getStackTrace()) {
                ProgramOptions.getLogger().error("\t%s.%s(%s:%d)", stack.getClassName(), stack.getMethodName(),
                        stack.getFileName(),
                        stack.getLineNumber());
            }
        } catch (RuntimeException e) {
            ProgramOptions.getLogger().error("Open file % failed, %s", file.getPath(), e.getMessage());
        }

        return ret;
    }

    /**
     * 打开工作簿
     *
     * @param filePath Excel文件
     * @param sheetName 表名
     * @return Sheet对象
     */
    public static Sheet openUserModuleSheet(File file, String sheetName) {
        Workbook wb = openWorkbook(file);
        if (null == wb)
            return null;

        return wb.getSheet(sheetName);
    }

    @SuppressWarnings("UseSpecificCatch")
    public static CustomDataTableIndex openStreamTableIndex(File file, String sheetName) {
        String realPath = null;
        try {
            realPath = file.getCanonicalPath().replaceAll("\\\\", "/");
        } catch (Exception e) {
        }
        try {
            if (realPath == null) {
                realPath = file.getAbsolutePath().replaceAll("\\\\", "/");
            }
        } catch (Exception e) {
        }
        if (realPath == null) {
            realPath = file.getPath().replaceAll("\\\\", "/");
        }
        return getOrBuildStreamTableIndex(realPath, sheetName,
                () -> file.getName().toLowerCase().endsWith(".xls")
                        ? ExcelHSSFStreamSheetHandle.buildCustomTableIndex(file, sheetName)
                        : ExcelXSSFStreamSheetHandle.buildCustomTableIndex(file, sheetName));
    }

    private static CustomDataTableIndex findCachedTable(TableCacheKey cacheKey) {
        tableCacheLock.readLock().lock();
        try {
            CachedTable cached = openedCustomDataTableIndex.get(cacheKey);
            if (cached != null) {
                cached.lastAccess.set(tableAccessSequence.incrementAndGet());
                return cached.table;
            }
            return null;
        } finally {
            tableCacheLock.readLock().unlock();
        }
    }

    static CustomDataTableIndex getOrBuildStreamTableIndex(String realPath, String sheetName,
            Supplier<CustomDataTableIndex> loader) {
        TableCacheKey cacheKey = new TableCacheKey(realPath, sheetName);
        CustomDataTableIndex cached = findCachedTable(cacheKey);
        if (cached != null) {
            return cached;
        }

        FileTableLoad fileLoad;
        // Retain before waiting: eviction or another caller finishing must not replace this file's lock.
        tableCacheLock.writeLock().lock();
        try {
            fileLoad = pendingTableLoads.computeIfAbsent(realPath, ignored -> new FileTableLoad());
            ++fileLoad.users;
        } finally {
            tableCacheLock.writeLock().unlock();
        }
        try {
            // Never wait for a file lock while holding the global index lock.
            fileLoad.lock.readLock().lock();
            try {
                cached = findCachedTable(cacheKey);
                if (cached != null) {
                    return cached;
                }
            } finally {
                fileLoad.lock.readLock().unlock();
            }

            // Drop the read lock before acquiring write, then recheck for a completed load.
            fileLoad.lock.writeLock().lock();
            try {
                cached = findCachedTable(cacheKey);
                if (cached != null) {
                    return cached;
                }
                CustomDataTableIndex table = loader.get();
                if (table != null) {
                    cacheTable(cacheKey, table);
                }
                return table;
            } finally {
                fileLoad.lock.writeLock().unlock();
            }
        } finally {
            tableCacheLock.writeLock().lock();
            try {
                if (--fileLoad.users == 0) {
                    pendingTableLoads.remove(realPath);
                }
            } finally {
                tableCacheLock.writeLock().unlock();
            }
        }
    }

    private static void cacheTable(TableCacheKey cacheKey, CustomDataTableIndex table) {
        tableCacheLock.writeLock().lock();
        try {
            long maxRows = ProgramOptions.getInstance().dataSourceLruCacheRows;
            if (maxRows <= 0) {
                maxRows = Integer.MAX_VALUE;
            }
            long rows = (long) table.getLastRowIndex() + 1;
            if (rows < maxRows) {
                CachedTable cached = new CachedTable(table);
                cached.lastAccess.set(tableAccessSequence.incrementAndGet());
                openedCustomDataTableIndex.put(cacheKey, cached);
                openedCustomDataTableRows += rows;
                while (openedCustomDataTableRows >= maxRows && !openedCustomDataTableIndex.isEmpty()) {
                    // Readers update only atomic access stamps; the map is never mutated under a read lock.
                    var oldest = openedCustomDataTableIndex.entrySet().stream()
                            .min(java.util.Comparator.comparingLong(entry -> entry.getValue().lastAccess.get())).orElseThrow();
                    openedCustomDataTableRows -= (long) oldest.getValue().table.getLastRowIndex() + 1;
                    openedCustomDataTableIndex.remove(oldest.getKey());
                }
            }
        } finally {
            tableCacheLock.writeLock().unlock();
        }
    }

    public static String tryMacro(DataSrcImpl dataSrcImpl, String m) {
        if (m == null || m.isEmpty()) {
            return m;
        }

        if (null == dataSrcImpl)
            return m;

        HashMap<String, String> hm = dataSrcImpl.getMacros();
        if (null == hm)
            return m;

        return hm.getOrDefault(m, m);
    }

    public static String tryMacro(String m) {
        return tryMacro(DataSrcImpl.getOurInstance(), m);
    }

    /**
     * 单元格数据转换（String）
     *
     * @param itemGrid    数据网格
     * @param ident       定位标识
     * @param dataSrcImpl 指定数据源
     * @return
     */
    public static void cell2s(DataContainer<String> out, DataItemGridWrapper itemGrid, IdentifyDescriptor ident,
            DataSrcImpl dataSrcImpl)
            throws ConvException {
        cell2s(out, itemGrid, ident, null, dataSrcImpl);
    }

    private static byte evaluateCellError(Cell c, CellValue cv) {
        if (null == cv) {
            return c.getErrorCellValue();
        }

        return cv.getErrorValue();
    }

    private static double evaluateCellNumber(Cell c, CellValue cv) {
        if (null == cv) {
            return c.getNumericCellValue();
        }

        return cv.getNumberValue();
    }

    private static String evaluateCellString(Cell c, CellValue cv) {
        if (null == cv) {
            return c.getStringCellValue();
        }

        return cv.getStringValue();
    }

    private static Boolean evaluateCellBoolean(Cell c, CellValue cv) {
        if (null == cv) {
            return c.getBooleanCellValue();
        }

        return cv.getBooleanValue();
    }

    /**
     * 单元格数据转换（String）
     *
     * @param itemGrid    数据网格
     * @param ident       定位标识
     * @param formula     公式管理器
     * @param dataSrcImpl 指定数据源
     * @return
     */
    public static void cell2s(DataContainer<String> out, DataItemGridWrapper itemGrid, IdentifyDescriptor ident,
            FormulaWrapper formula, DataSrcImpl dataSrcImpl) throws ConvException {
        if (null == itemGrid) {
            return;
        }

        String plainValue = itemGrid.getCustomRowIndexCellValue(itemGrid, ident);
        if (null != plainValue) {
            if (!plainValue.isEmpty()) {
                if (dataSrcImpl != null && dataSrcImpl.isInitialized()
                        && ProgramOptions.getInstance().enableStringMacro) {
                    out.set(DataVerifyImpl.getAndVerifyToString(ident.getValidator(), ident.getTypeValidator(),
                            ident.name,
                            tryMacro(dataSrcImpl, plainValue)));
                } else {
                    out.set(plainValue);
                }
            }
            return;
        }

        Cell c = itemGrid.getUserModuleRowCell(itemGrid, ident);
        if (null == c) {
            return;
        }

        CellValue cv = null;
        if (CellType.FORMULA == c.getCellType()) {
            if (null != formula && null != formula.evalor) {
                try {
                    cv = formula.evalor.evaluate(c);
                } catch (NotImplementedException e) {
                    ProgramOptions.getLogger().warn(
                            "Formular has unsupported function(s).We will use cached data.Consider using --disable-excel-formular?%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > Formular Content: %s",
                            ProgramOptions.getEndl(), itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1), ProgramOptions.getEndl(),
                            c.getCellFormula());
                    cv = null;
                } catch (Exception e) {
                    ProgramOptions.getLogger().warn(
                            "Evaluate formular failed: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1));
                    cv = null;
                }
            } else {
                if (dataSrcImpl != null && dataSrcImpl.isInitialized()
                        && ProgramOptions.getInstance().enableStringMacro) {
                    out.set(DataVerifyImpl.getAndVerifyToString(ident.getValidator(), ident.getTypeValidator(),
                            ident.name,
                            tryMacro(dataSrcImpl, c.toString())));
                } else {
                    out.set(DataVerifyImpl.getAndVerifyToString(ident.getValidator(), ident.getTypeValidator(),
                            ident.name,
                            c.toString()));
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
            case BLANK -> {
            }
            case BOOLEAN -> out.set(evaluateCellBoolean(c, cv).toString());
            case ERROR -> {
                byte errorCode = evaluateCellError(c, cv);
                try {
                    out.set(FormulaError.forInt(errorCode).getString());
                } catch (IllegalArgumentException e) {
                    out.set(e.getMessage());
                }
            }
            case FORMULA -> {
                if (null == cv) {
                    out.set(c.getCellFormula());
                }
            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(c)) {
                    // 参照POI DateUtil.isADateFormat函数，去除无效字符
                    String fs = c.getCellStyle().getDataFormatString().replaceAll("\\\\-", "-").replaceAll("\\\\,", ",")
                            .replaceAll("\\\\\\.", ".").replaceAll("\\\\ ", " ").replaceAll("AM/PM", "")
                            .replaceAll("\\[[^]]*\\]", "");

                    // 默认格式
                    int idx = fs.indexOf(";@");
                    if (idx > 0 && idx < fs.length()) {
                        // 包含年月日
                        LinkedList<String> rfs = new LinkedList<>();

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

                double dv = evaluateCellNumber(c, cv);
                if (ident.getRatio() != 1) {
                    dv = dv * ident.getRatio();
                }
                if (dv == (long) dv) {
                    out.set(String.format("%d", (long) dv));
                } else {
                    out.set(String.format("%s", dv));
                }
            }
            case STRING -> {
                String val = evaluateCellString(c, cv).trim();
                if (!val.isEmpty()) {
                    // Const 和 option导出时，没有数据源，也不需要文本/宏替换
                    if (null != dataSrcImpl && dataSrcImpl.isInitialized()
                            && ProgramOptions.getInstance().enableStringMacro) {
                        out.set(DataVerifyImpl.getAndVerifyToString(ident.getValidator(), ident.getTypeValidator(),
                                ident.name, tryMacro(dataSrcImpl, val)));
                    } else {
                        out.set(DataVerifyImpl.getAndVerifyToString(ident.getValidator(), ident.getTypeValidator(),
                                ident.name, val));
                    }
                }
            }
            default -> {
            }
        }
    }

    /**
     * 单元格数据转换（Integer）
     *
     * @param itemGrid 数据网格
     * @param ident    定位标识
     * @return
     */
    public static void cell2i(DataContainer<Long> out, DataItemGridWrapper itemGrid, IdentifyDescriptor ident)
            throws ConvException {
        cell2i(out, itemGrid, ident, null);
    }

    /**
     * 单元格数据转换（Integer）
     *
     * @param itemGrid 数据网格
     * @param ident    定位标识
     * @param formula  公式管理器
     * @return
     */
    public static void cell2i(DataContainer<Long> out, DataItemGridWrapper itemGrid, IdentifyDescriptor ident,
            FormulaWrapper formula) throws ConvException {

        if (null == itemGrid) {
            return;
        }

        String plainValue = itemGrid.getCustomRowIndexCellValue(itemGrid, ident);
        if (null != plainValue) {
            if (!plainValue.isEmpty()) {
                out.set(DataVerifyImpl.getAndVerifyToLong(ident.getValidator(), ident.getTypeValidator(), ident.name,
                        tryMacro(plainValue)));
            }
            return;
        }

        Cell c = itemGrid.getUserModuleRowCell(itemGrid, ident);
        if (null == c) {
            return;
        }

        CellValue cv = null;
        if (CellType.FORMULA == c.getCellType()) {
            if (null != formula && null != formula.evalor)
                try {
                    cv = formula.evalor.evaluate(c);
                } catch (NotImplementedException e) {
                    ProgramOptions.getLogger().warn(
                            "Formular has unsupported function(s).We will use cached data.Consider using --disable-excel-formular?%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > Formular Content: %s",
                            ProgramOptions.getEndl(), itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1), ProgramOptions.getEndl(),
                            c.getCellFormula());
                    cv = null;
                } catch (Exception e) {
                    ProgramOptions.getLogger().warn(
                            "Evaluate formular failed: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1));
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
            case BLANK -> {
            }
            case BOOLEAN -> {
                boolean res = evaluateCellBoolean(c, cv);
                out.set(DataVerifyImpl.getAndVerifyNumeric(ident.getValidator(), ident.getTypeValidator(), ident.name,
                        res ? 1 : 0));
            }
            case ERROR -> {
                byte errorCode = evaluateCellError(c, cv);
                try {
                    ProgramOptions.getLogger().warn(
                            "Error formula: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            FormulaError.forInt(errorCode).getString(), ProgramOptions.getEndl(),
                            itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1));
                } catch (IllegalArgumentException e) {
                    ProgramOptions.getLogger().warn(
                            "Error or unsupported cell value: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1));
                }
            }
            case FORMULA -> {
            }
            case NUMERIC -> {
                long val;
                if (DateUtil.isCellDateFormatted(c)) {
                    val = dateToUnixTimestamp(c.getDateCellValue());
                } else {
                    if (ident.getRatio() == 1) {
                        val = Math.round(evaluateCellNumber(c, cv));
                    } else {
                        val = Math.round(evaluateCellNumber(c, cv) * ident.getRatio());
                    }
                }

                out.set(DataVerifyImpl.getAndVerifyNumeric(ident.getValidator(), ident.getTypeValidator(), ident.name,
                        val));
            }
            case STRING -> {
                String val = evaluateCellString(c, cv).trim();
                if (val.isEmpty()) {
                }

                out.set(DataVerifyImpl.getAndVerifyToLong(ident.getValidator(), ident.getTypeValidator(), ident.name,
                        tryMacro(val)));
            }
            default -> {
            }
        }
    }

    /**
     * 单元格数据转换（Double）
     *
     * @param itemGrid 数据网格
     * @param ident    定位标识
     * @return
     */
    public static void cell2d(DataContainer<Double> out, DataItemGridWrapper itemGrid, IdentifyDescriptor ident)
            throws ConvException {
        cell2d(out, itemGrid, ident, null);
    }

    /**
     * 单元格数据转换（Double）
     *
     * @param itemGrid 数据网格
     * @param ident    定位标识
     * @param formula  公式管理器
     * @return
     */
    public static void cell2d(DataContainer<Double> out, DataItemGridWrapper itemGrid, IdentifyDescriptor ident,
            FormulaWrapper formula) throws ConvException {

        if (null == itemGrid) {
            return;
        }

        String plainValue = itemGrid.getCustomRowIndexCellValue(itemGrid, ident);
        if (null != plainValue) {
            if (!plainValue.isEmpty()) {
                try {
                    out.set(DataVerifyImpl.getAndVerifyToDouble(ident.getValidator(), ident.getTypeValidator(),
                            ident.name,
                            tryMacro(plainValue)));
                } catch (java.lang.NumberFormatException e) {
                    int originRow = itemGrid.getOriginRowIndex(ident.getDataFieldIndex());
                    int originCol = itemGrid.getOriginColumnIndex(ident.getDataFieldIndex());
                    throw new ConvException(
                            String.format(
                                    "File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > %s can not be converted to a number",
                                    itemGrid.getDataSource().filePath, itemGrid.getDataSource().tableName,
                                    originRow + 1, originCol + 1,
                                    getColumnName(originCol + 1),
                                    ProgramOptions.getEndl(),
                                    plainValue));
                }
            }
            return;
        }

        Cell c = itemGrid.getUserModuleRowCell(itemGrid, ident);
        if (null == c) {
            return;
        }

        CellValue cv = null;
        if (CellType.FORMULA == c.getCellType()) {
            if (null != formula && null != formula.evalor) {
                try {
                    cv = formula.evalor.evaluate(c);
                } catch (NotImplementedException e) {
                    ProgramOptions.getLogger().warn(
                            "Formular has unsupported function(s).We will use cached data.Consider using --disable-excel-formular?%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > Formular Content: %s",
                            ProgramOptions.getEndl(), itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1), ProgramOptions.getEndl(),
                            c.getCellFormula());
                    cv = null;
                } catch (Exception e) {
                    ProgramOptions.getLogger().warn(
                            "Evaluate formular failed: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1));
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
            case BLANK -> {
            }
            case BOOLEAN -> out.set(evaluateCellBoolean(c, cv) ? 1.0 : 0.0);
            case ERROR -> {
                byte errorCode = evaluateCellError(c, cv);
                try {
                    ProgramOptions.getLogger().warn(
                            "Error formula: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            FormulaError.forInt(errorCode).getString(), ProgramOptions.getEndl(),
                            itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1));
                } catch (IllegalArgumentException e) {
                    ProgramOptions.getLogger().warn(
                            "Error or unsupported cell value: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1));
                }
            }
            case FORMULA -> {
            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(c)) {
                    out.set((double) dateToUnixTimestamp(c.getDateCellValue()));
                    break;
                }
                if (ident.getRatio() == 1) {
                    out.set(evaluateCellNumber(c, cv));
                } else {
                    out.set(evaluateCellNumber(c, cv) * ident.getRatio());
                }
            }
            case STRING -> {
                String val = evaluateCellString(c, cv).trim();
                if (val.isEmpty()) {
                }

                try {
                    out.set(DataVerifyImpl.getAndVerifyToDouble(ident.getValidator(), ident.getTypeValidator(),
                            ident.name,
                            tryMacro(val)));
                } catch (java.lang.NumberFormatException e) {
                    int originRow = itemGrid.getOriginRowIndex(ident.getDataFieldIndex());
                    int originCol = itemGrid.getOriginColumnIndex(ident.getDataFieldIndex());
                    throw new ConvException(
                            String.format(
                                    "File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > %s can not be converted to a number",
                                    itemGrid.getDataSource().filePath, itemGrid.getDataSource().tableName,
                                    originRow + 1, originCol + 1,
                                    getColumnName(originCol + 1), ProgramOptions.getEndl(), val));
                }
            }
            default -> {
            }
        }
    }

    /**
     * 单元格数据转换（boolean）
     *
     * @param itemGrid 数据网格
     * @param ident    定位标识 * @return
     */
    public static void cell2b(DataContainer<Boolean> out, DataItemGridWrapper itemGrid, IdentifyDescriptor ident)
            throws ConvException {
        cell2b(out, itemGrid, ident, null);
    }

    /**
     * 单元格数据转换（boolean）
     *
     * @param itemGrid 数据网格
     * @param ident    定位标识
     * @param formula  公式管理器
     * @return
     */
    public static void cell2b(DataContainer<Boolean> out, DataItemGridWrapper itemGrid, IdentifyDescriptor ident,
            FormulaWrapper formula) throws ConvException {
        if (null == itemGrid) {
            return;
        }

        String plainValue = itemGrid.getCustomRowIndexCellValue(itemGrid, ident);
        if (null != plainValue) {
            if (!plainValue.isEmpty()) {
                out.set(DataSrcImpl.getBooleanFromString(
                        DataVerifyImpl.getAndVerifyToString(ident.getValidator(), ident.getTypeValidator(), ident.name,
                                tryMacro(plainValue))));
            }
            return;
        }

        Cell c = itemGrid.getUserModuleRowCell(itemGrid, ident);
        if (null == c) {
            return;
        }

        CellValue cv = null;
        if (CellType.FORMULA == c.getCellType()) {
            if (null != formula && null != formula.evalor) {
                try {
                    cv = formula.evalor.evaluate(c);
                } catch (NotImplementedException e) {
                    ProgramOptions.getLogger().warn(
                            "Formular has unsupported function(s).We will use cached data.Consider using --disable-excel-formular?%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > Formular Content: %s",
                            ProgramOptions.getEndl(), itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1), ProgramOptions.getEndl(),
                            c.getCellFormula());
                    cv = null;
                } catch (Exception e) {
                    ProgramOptions.getLogger().warn(
                            "Evaluate formular failed: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1));
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
            case BLANK -> {
            }
            case BOOLEAN -> out.set(evaluateCellBoolean(c, cv));
            case ERROR -> {
                byte errorCode = evaluateCellError(c, cv);
                try {
                    ProgramOptions.getLogger().warn(
                            "Error formula: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            FormulaError.forInt(errorCode).getString(), ProgramOptions.getEndl(),
                            itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1));
                } catch (IllegalArgumentException e) {
                    ProgramOptions.getLogger().warn(
                            "Error or unsupported cell value: %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)",
                            e.getMessage(), ProgramOptions.getEndl(), itemGrid.getDataSource().filePath,
                            itemGrid.getDataSource().tableName, c.getRowIndex() + 1,
                            c.getColumnIndex() + 1, getColumnName(c.getColumnIndex() + 1));
                }
            }
            case FORMULA -> {
            }
            case NUMERIC -> out.set(evaluateCellNumber(c, cv) != 0 && ident.getRatio() != 0);
            case STRING -> {
                String item = DataVerifyImpl
                        .getAndVerifyToString(ident.getValidator(), ident.getTypeValidator(), ident.name,
                                tryMacro(evaluateCellString(c, cv).trim()))
                        .toLowerCase();
                if (item.isEmpty()) {
                    break;
                }

                out.set(DataSrcImpl.getBooleanFromString(item));
            }
            default -> {
            }
        }
    }

    private static long dateToUnixTimestamp(Date d) {
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

    public static void setMaxByteArraySize(int maxSize) {
        IOUtils.setByteArrayMaxOverride(maxSize);
    }
}
