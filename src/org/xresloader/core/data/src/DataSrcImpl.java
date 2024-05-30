package org.xresloader.core.data.src;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.engine.IdentifyDescriptor;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by owentou on 2014/10/9.
 */
public abstract class DataSrcImpl {
    static public final int LOG_PROCESS_BOUND = 5000;

    private static ThreadLocal<DataSrcImpl> ourInstance = new ThreadLocal<>();
    private int last_column_index_ = 0;
    private static ThreadLocal<DataContainer<Boolean>> dc_cache_bool_ = ThreadLocal
            .withInitial(() -> new DataContainer<Boolean>());
    private static ThreadLocal<DataContainer<String>> dc_cache_str_ = ThreadLocal
            .withInitial(() -> new DataContainer<String>());
    private static ThreadLocal<DataContainer<Double>> dc_cache_dbl_ = ThreadLocal
            .withInitial(() -> new DataContainer<Double>());
    private static ThreadLocal<DataContainer<Long>> dc_cache_long_ = ThreadLocal
            .withInitial(() -> new DataContainer<Long>());

    protected DataSrcImpl() {
    }

    public static DataContainer<Boolean> getBoolCache(boolean default_val) {
        dc_cache_bool_.get().value = default_val;
        dc_cache_bool_.get().valid = false;
        return dc_cache_bool_.get();
    }

    public static DataContainer<String> getStringCache(String default_val) {
        dc_cache_str_.get().value = default_val;
        dc_cache_str_.get().valid = false;
        return dc_cache_str_.get();
    }

    public static DataContainer<Double> getDoubleCache(double default_val) {
        dc_cache_dbl_.get().value = default_val;
        dc_cache_dbl_.get().valid = false;
        return dc_cache_dbl_.get();
    }

    public static DataContainer<Long> getLongCache(long default_val) {
        dc_cache_long_.get().value = default_val;
        dc_cache_long_.get().valid = false;
        return dc_cache_long_.get();
    }

    public static DataSrcImpl create(Class<?> clazz) {
        try {
            // return ourInstance = (DataSrcImpl) clazz.newInstance();
            ourInstance.set((DataSrcImpl) clazz.getDeclaredConstructor().newInstance());
            return ourInstance.get();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
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

    public int getLastColomnNum() {
        return last_column_index_;
    }

    protected void setLastColumnNum(int c) {
        last_column_index_ = c;
    }

    public int getCurrentRowNum() {
        return 0;
    }

    public boolean hasCurrentRow() {
        return getCurrentRowNum() >= 0;
    }

    public String getCurrentTableName() {
        return "";
    }

    public String getCurrentFileName() {
        return "";
    }

    public DataContainer<Boolean> getValue(IdentifyDescriptor ident, boolean dv) throws ConvException {
        if (null != ident) {
            setLastColumnNum(ident.index);
        }
        return getBoolCache(dv);
    }

    public DataContainer<String> getValue(IdentifyDescriptor ident, String dv) throws ConvException {
        if (null != ident) {
            setLastColumnNum(ident.index);
        }
        return getStringCache(dv);
    }

    public DataContainer<Long> getValue(IdentifyDescriptor ident, long dv) throws ConvException {
        if (null != ident) {
            setLastColumnNum(ident.index);
        }
        return getLongCache(dv);
    }

    public DataContainer<Double> getValue(IdentifyDescriptor ident, double dv) throws ConvException {
        if (null != ident) {
            setLastColumnNum(ident.index);
        }
        return getDoubleCache(dv);
    }

    public int getRecordNumber() {
        return 0;
    }

    public IdentifyDescriptor getColumnByName(String _name) {
        return null;
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
