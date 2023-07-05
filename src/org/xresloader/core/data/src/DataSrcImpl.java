package org.xresloader.core.data.src;

import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.engine.IdentifyDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by owentou on 2014/10/9.
 */
public abstract class DataSrcImpl {
    static public final int LOG_PROCESS_BOUND = 5000;

    private static DataSrcImpl ourInstance = null;
    private int last_column_index_ = 0;
    private static DataContainer<Boolean> dc_cache_bool_ = new DataContainer<Boolean>();
    private static DataContainer<String> dc_cache_str_ = new DataContainer<String>();
    private static DataContainer<Double> dc_cache_dbl_ = new DataContainer<Double>();
    private static DataContainer<Long> dc_cache_long_ = new DataContainer<Long>();

    protected DataSrcImpl() {
    }

    public static DataContainer<Boolean> getBoolCache(boolean default_val) {
        dc_cache_bool_.value = default_val;
        dc_cache_bool_.valid = false;
        return dc_cache_bool_;
    }

    public static DataContainer<String> getStringCache(String default_val) {
        dc_cache_str_.value = default_val;
        dc_cache_str_.valid = false;
        return dc_cache_str_;
    }

    public static DataContainer<Double> getDoubleCache(double default_val) {
        dc_cache_dbl_.value = default_val;
        dc_cache_dbl_.valid = false;
        return dc_cache_dbl_;
    }

    public static DataContainer<Long> getLongCache(long default_val) {
        dc_cache_long_.value = default_val;
        dc_cache_long_.valid = false;
        return dc_cache_long_;
    }

    public static DataSrcImpl create(Class<?> clazz) {
        try {
            // return ourInstance = (DataSrcImpl) clazz.newInstance();
            return ourInstance = (DataSrcImpl) clazz.getDeclaredConstructor().newInstance();
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

        return ourInstance = null;
    }

    public static DataSrcImpl getOurInstance() {
        return ourInstance;
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
}
