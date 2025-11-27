package org.xresloader.core.data.src;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.engine.IdentifyDescriptor;

/**
 * Created by owentou on 2014/10/9.
 */
public abstract class DataSrcImpl {
    static public final int LOG_PROCESS_BOUND = 5000;

    private static final ThreadLocal<DataSrcImpl> ourInstance = new ThreadLocal<>();
    private int lastDataFieldIndex = 0;

    private static final ThreadLocal<DataContainer<Boolean>> dcCacheBool = ThreadLocal
            .withInitial(() -> new DataContainer<Boolean>());
    private static final ThreadLocal<DataContainer<String>> dcCacheString = ThreadLocal
            .withInitial(() -> new DataContainer<String>());
    private static final ThreadLocal<DataContainer<Double>> dcCacheDouble = ThreadLocal
            .withInitial(() -> new DataContainer<Double>());
    private static final ThreadLocal<DataContainer<Long>> dcCacheLong = ThreadLocal
            .withInitial(() -> new DataContainer<Long>());

    protected DataSrcImpl() {
    }

    public static DataContainer<Boolean> getBoolCache(boolean default_val) {
        dcCacheBool.get().value = default_val;
        dcCacheBool.get().valid = false;
        return dcCacheBool.get();
    }

    public static DataContainer<String> getStringCache(String default_val) {
        dcCacheString.get().value = default_val;
        dcCacheString.get().valid = false;
        return dcCacheString.get();
    }

    public static DataContainer<Double> getDoubleCache(double default_val) {
        dcCacheDouble.get().value = default_val;
        dcCacheDouble.get().valid = false;
        return dcCacheDouble.get();
    }

    public static DataContainer<Long> getLongCache(long default_val) {
        dcCacheLong.get().value = default_val;
        dcCacheLong.get().valid = false;
        return dcCacheLong.get();
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static DataSrcImpl create(Class<?> clazz) {
        try {
            // return ourInstance = (DataSrcImpl) clazz.newInstance();
            ourInstance.set((DataSrcImpl) clazz.getDeclaredConstructor().newInstance());
            return ourInstance.get();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }

        ourInstance.set(null);
        return ourInstance.get();
    }

    public static DataSrcImpl getOurInstance() {
        return ourInstance.get();
    }

    public int init() {
        return -41;
    }

    public abstract boolean isInitialized();

    public abstract ExcelEngine.DataItemGridWrapper getCurrentDataItemGrid();

    /**
     * @brief 获取下一个数据源
     * @note 每次调用这个函数之后最好重做一次列名映射，否则可能数据不匹配
     * @return 有待处理的数据表返回true
     */
    public boolean nextTable() {
        return false;
    }

    /**
     * @brief 获取当前数据源的下一个数据项
     * @return 有数据则返回true
     */
    public boolean nextRow() {
        return false;
    }

    public int getLastColumnNum() {
        ExcelEngine.DataItemGridWrapper currentItemGrid = getCurrentDataItemGrid();
        if (currentItemGrid == null) {
            return -1;
        }

        return currentItemGrid.getOriginColumnIndex(this.lastDataFieldIndex);
    }

    private void setDataFieldIndex(int dataFieldIndex) {
        this.lastDataFieldIndex = dataFieldIndex;
    }

    public int getLastRowNum() {
        ExcelEngine.DataItemGridWrapper currentItemGrid = getCurrentDataItemGrid();
        if (currentItemGrid == null) {
            return -1;
        }

        return currentItemGrid.getOriginRowIndex(this.lastDataFieldIndex);
    }

    public boolean hasCurrentDataGrid() {
        return getLastRowNum() >= 0;
    }

    public String getCurrentTableName() {
        return "";
    }

    public String getCurrentFileName() {
        return "";
    }

    public DataContainer<Boolean> getValue(IdentifyDescriptor ident, boolean dv) throws ConvException {
        if (null != ident) {
            setDataFieldIndex(ident.getDataFieldIndex());
        }
        return getBoolCache(dv);
    }

    public DataContainer<String> getValue(IdentifyDescriptor ident, String dv) throws ConvException {
        if (null != ident) {
            setDataFieldIndex(ident.getDataFieldIndex());
        }
        return getStringCache(dv);
    }

    public DataContainer<Long> getValue(IdentifyDescriptor ident, long dv) throws ConvException {
        if (null != ident) {
            setDataFieldIndex(ident.getDataFieldIndex());
        }
        return getLongCache(dv);
    }

    public DataContainer<Double> getValue(IdentifyDescriptor ident, double dv) throws ConvException {
        if (null != ident) {
            setDataFieldIndex(ident.getDataFieldIndex());
        }
        return getDoubleCache(dv);
    }

    public int getRecordNumber() {
        return 0;
    }

    public IdentifyDescriptor getColumnByName(String _name) {
        return null;
    }

    public boolean containsIdentifyMappingPrefix(String _name) {
        return false;
    }

    public HashMap<String, String> getMacros() {
        return null;
    }

    public LinkedList<IdentifyDescriptor> getMappedColumns() {
        return null;
    }

    static public Boolean getBooleanFromString(String item) {
        if (item == null || item.isEmpty()) {
            return false;
        }

        return !item.equals("0") && !item.equals("0.0") && !item.equalsIgnoreCase("false")
                && !item.equalsIgnoreCase("no") && !item.equalsIgnoreCase("disable");
    }

    public static File getDataFile(String filePath) {
        File file = new File(filePath);
        if (!file.isAbsolute()) {
            File fallbackFile = file;
            for (String testFileDir : ProgramOptions.getInstance().dataSourceDirectory) {
                file = new File(testFileDir, filePath);
                if (file.exists()) {
                    return file;
                }
            }

            if (fallbackFile.exists()) {
                return fallbackFile;
            }
        }

        if (file.exists()) {
            return file;
        }

        return null;
    }
}
