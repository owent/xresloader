package org.xresloader.core.engine;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcImpl;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.extractor.ExcelExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
     * 日期格式列缓存，XSSF在获取Style时性能极其低下，缓存一下有助于提升性能 导致的副作用就是只接受第一个数据行的日期格式
     */
    // static private HashMap<Integer, SimpleDateFormat> dateTypeStyle = new
    // HashMap<Integer, SimpleDateFormat>();

    /**
     * 清空缓存
     */
    static public void clearAllCache() {
        // dateTypeStyle.clear();
    }

    /**
     * 打开Excel文件
     *
     * @param file_path 文件路径
     * @return Excel Workbook对象
     */
    static public Workbook openWorkbook(String file_path) {
        // 无论打开什么Excel文件，都要清空缓存
        clearAllCache();

        File file_check = new File(file_path);
        if (!file_check.isAbsolute()) {
            file_path = ProgramOptions.getInstance().dataSourceDirectory + '/' + file_path;
            file_check = new File(file_path);
        }

        try {
            file_path = file_check.getCanonicalPath();
            if (false == file_check.exists()) {
                return null;
            }
            file_path = file_check.getCanonicalPath();
        } catch (IOException e) {
            ProgramOptions.getLoger().error("%s", e.getMessage());
        }

        Workbook ret = openedWorkbooks.get(file_path);
        if (null != ret) {
            return ret;
        }

        FileInputStream is = null;
        try {
            is = new FileInputStream(file_path);

            ExcelExtractor extractor = null;
            // 类型枚举，以后能支持 ods等非微软格式？
            if (file_path.endsWith(".xls")) {
                ret = new HSSFWorkbook(is);
                extractor = new org.apache.poi.hssf.extractor.ExcelExtractor((HSSFWorkbook) ret);
            } else {
                ret = new XSSFWorkbook(is);
                extractor = new org.apache.poi.xssf.extractor.XSSFExcelExtractor((XSSFWorkbook) ret);
            }

            extractor.setFormulasNotResults(false);
        } catch (java.io.IOException e) {
            ProgramOptions.getLoger().error("%s", e.getMessage());
        }

        openedWorkbooks.put(file_path, ret);
        return ret;
    }

    /**
     * 打开工作簿
     *
     * @param file_path  Excel文件
     * @param sheet_name 表名
     * @return Sheet对象
     */
    static public Sheet openSheet(String file_path, String sheet_name) {
        Workbook wb = openWorkbook(file_path);
        if (null == wb)
            return null;

        return wb.getSheet(sheet_name);
    }

    static public String tryMacro(String m) {
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
     * @param row 行
     * @param col 列号
     * @return
     */
    static public void cell2s(DataContainer<String> out, Row row, IdentifyDescriptor col) {
        cell2s(out, row, col, null);
    }

    static private Byte cal_cell2err(Cell c, CellValue cv) {
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
     * @param row    行
     * @param col    列号
     * @param evalor 公式管理器
     * @return
     */
    static public void cell2s(DataContainer<String> out, Row row, IdentifyDescriptor col, FormulaEvaluator evalor) {
        if (null == row) {
            return;
        }

        Cell c = row.getCell(col.index);
        if (null == c) {
            return;
        }

        CellValue cv = null;
        if (CellType.FORMULA == c.getCellTypeEnum()) {
            if (null != evalor) {
                cv = evalor.evaluate(c);
            } else {
                out.set(c.toString());
                return;
            }
        }

        CellType type = (null == cv) ? c.getCellTypeEnum() : cv.getCellTypeEnum();
        switch (type) {
        case BLANK:
            break;
        case BOOLEAN:
            out.set(cal_cell2bool(c, cv).toString());
            break;
        case ERROR:
            out.set(cal_cell2err(c, cv).toString());
            break;
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

            out.set(String.valueOf(cal_cell2num(c, cv)));
            break;
        case STRING:
            // return ret.set(tryMacro(cal_cell2str(c, cv).trim()));
            String val = cal_cell2str(c, cv).trim();
            if (!val.isEmpty()) {
                out.set(val);
            }
            break;
        default:
            break;
        }
    }

    /**
     * 单元格数据转换（Integer）
     *
     * @param row 行
     * @param col 列号
     * @return
     */
    static public void cell2i(DataContainer<Long> out, Row row, IdentifyDescriptor col) throws ConvException {
        cell2i(out, row, col, null);
    }

    /**
     * 单元格数据转换（Integer）
     *
     * @param row    行
     * @param col    列号
     * @param evalor 公式管理器
     * @return
     */
    static public void cell2i(DataContainer<Long> out, Row row, IdentifyDescriptor col, FormulaEvaluator evalor)
            throws ConvException {
        if (null == row)
            return;

        Cell c = row.getCell(col.index);
        if (null == c)
            return;

        CellValue cv = null;
        if (CellType.FORMULA == c.getCellTypeEnum()) {
            if (null != evalor)
                cv = evalor.evaluate(c);
            else
                return;
        }

        CellType type = (null == cv) ? c.getCellTypeEnum() : cv.getCellTypeEnum();
        switch (type) {
        case BLANK:
            break;
        case BOOLEAN: {
            boolean res = cal_cell2bool(c, cv);
            out.set(col.getAndVerify(res ? 1 : 0));
            break;
        }
        case ERROR:
            break;
        case FORMULA:
            break;
        case NUMERIC: {
            long val = 0;
            if (DateUtil.isCellDateFormatted(c)) {
                val = dateToUnixTimestamp(c.getDateCellValue());
            } else {
                val = Math.round(cal_cell2num(c, cv));
            }

            out.set(col.getAndVerify(val));
            break;
        }
        case STRING: {
            String val = cal_cell2str(c, cv).trim();
            if (val.isEmpty()) {
                break;
            }

            out.set(col.getAndVerify(tryMacro(val)));
            break;
        }
        default:
            break;
        }
    }

    /**
     * 单元格数据转换（Double）
     *
     * @param row 行
     * @param col 列号
     * @return
     */
    static public void cell2d(DataContainer<Double> out, Row row, IdentifyDescriptor col) throws ConvException {
        cell2d(out, row, col, null);
    }

    /**
     * 单元格数据转换（Double）
     *
     * @param row    行
     * @param col    列号
     * @param evalor 公式管理器
     * @return
     */
    static public void cell2d(DataContainer<Double> out, Row row, IdentifyDescriptor col, FormulaEvaluator evalor)
            throws ConvException {
        if (null == row)
            return;

        Cell c = row.getCell(col.index);
        if (null == c)
            return;

        CellValue cv = null;
        if (CellType.FORMULA == c.getCellTypeEnum()) {
            if (null != evalor) {
                cv = evalor.evaluate(c);
            } else {
                return;
            }
        }

        CellType type = (null == cv) ? c.getCellTypeEnum() : cv.getCellTypeEnum();
        switch (type) {
        case BLANK:
            break;
        case BOOLEAN:
            out.set(cal_cell2bool(c, cv) ? 1.0 : 0.0);
            break;
        case ERROR:
            break;
        case FORMULA:
            break;
        case NUMERIC:
            if (DateUtil.isCellDateFormatted(c)) {
                out.set((double) dateToUnixTimestamp(c.getDateCellValue()));
                break;
            }
            out.set(cal_cell2num(c, cv));
            break;
        case STRING: {
            String val = cal_cell2str(c, cv).trim();
            if (val.isEmpty()) {
                break;
            }

            try {
                out.set(Double.valueOf(tryMacro(val)));
            } catch (java.lang.NumberFormatException e) {
                throw new ConvException(String.format("%s can not be converted to a number", val));
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
     * @param row 行
     * @param col 列号
     * @return
     */
    static public void cell2b(DataContainer<Boolean> out, Row row, IdentifyDescriptor col) {
        cell2b(out, row, col, null);
    }

    /**
     * 单元格数据转换（boolean）
     *
     * @param row    行
     * @param col    列号
     * @param evalor 公式管理器
     * @return
     */
    static public void cell2b(DataContainer<Boolean> out, Row row, IdentifyDescriptor col, FormulaEvaluator evalor) {
        if (null == row)
            return;

        Cell c = row.getCell(col.index);
        if (null == c)
            return;

        CellValue cv = null;
        if (CellType.FORMULA == c.getCellTypeEnum()) {
            if (null != evalor) {
                cv = evalor.evaluate(c);
            } else {
                out.set(true);
                return;
            }
        }

        CellType type = (null == cv) ? c.getCellTypeEnum() : cv.getCellTypeEnum();
        switch (type) {
        case BLANK:
            break;
        case BOOLEAN:
            out.set(cal_cell2bool(c, cv));
            break;
        case ERROR:
            break;
        case FORMULA:
            break;
        case NUMERIC:
            out.set(cal_cell2num(c, cv) != 0);
            break;
        case STRING:
            String item = tryMacro(cal_cell2str(c, cv).trim()).toLowerCase();
            if (item.isEmpty()) {
                break;
            }

            out.set(!item.equals("0") && !item.equals("0.0") && !item.equalsIgnoreCase("false")
                    && !item.equalsIgnoreCase("no") && !item.equalsIgnoreCase("disable"));
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
}
