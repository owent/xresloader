package com.owent.xresloader.engine;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.src.DataSrcImpl;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.extractor.ExcelExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
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
     * 日期格式列缓存，XSSF在获取Style时性能极其低下，缓存一下有助于提升性能
     * 导致的副作用就是只接受第一个数据行的日期格式
     */
    //static private HashMap<Integer, SimpleDateFormat> dateTypeStyle = new HashMap<Integer, SimpleDateFormat>();


    /**
     * 清空缓存
     */
    static public void clearAllCache() {
        //dateTypeStyle.clear();
    }

    /**
     * 打开Excel文件
     *
     * @param file_path 文件路径
     * @return Excel Workbook对象
     */
    static public Workbook openWorkbook(String file_path) {
        // 无论打开什么Excel文件，都要情况缓存
        clearAllCache();

        if(!IdentifyEngine.isAbsPath(file_path)) {
            file_path = ProgramOptions.getInstance().dataSourceDirectory + '/' + file_path;
        }

        try {
            file_path = new File(file_path).getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Workbook ret = openedWorkbooks.get(file_path);
        if (null != ret)
            return ret;

        FileInputStream is = null;
        try {
            is = new FileInputStream(file_path);

            ExcelExtractor extractor = null;
            // 类型枚举，以后能支持 ods等非微软格式？
            if (file_path.endsWith(".xls")) {
                ret = new HSSFWorkbook(is);
                extractor = new org.apache.poi.hssf.extractor.ExcelExtractor((HSSFWorkbook)ret);
            } else {
                ret = new XSSFWorkbook(is);
                extractor = new org.apache.poi.xssf.extractor.XSSFExcelExtractor((XSSFWorkbook)ret);
            }

            extractor.setFormulasNotResults(false);
        } catch (java.io.IOException e) {
            e.printStackTrace();
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
    static public String cell2s(Row row, int col) {
        return cell2s(row, col, null);
    }

    /**
     * 单元格数据转换（String）
     *
     * @param row    行
     * @param col    列号
     * @param evalor 公式管理器
     * @return
     */
    static public String cell2s(Row row, int col, FormulaEvaluator evalor) {
        if (null == row)
            return "";

        Cell c = row.getCell(col);
        if (null == c)
            return "";

        CellValue cv = null;
        if (null != evalor && Cell.CELL_TYPE_FORMULA == c.getCellType())
            cv = evalor.evaluate(c);

        int type = (null == cv)? c.getCellType(): cv.getCellType();
        switch (type) {
            case Cell.CELL_TYPE_BLANK:
                return "";
            case Cell.CELL_TYPE_BOOLEAN:
                return String.valueOf((null == cv)? c.getBooleanCellValue(): cv.getBooleanValue());
            case Cell.CELL_TYPE_ERROR:
                return String.valueOf((null == cv)? c.getErrorCellValue(): cv.getErrorValue());
            case Cell.CELL_TYPE_FORMULA:
                return (null == cv)? c.getCellFormula(): "";
            case Cell.CELL_TYPE_NUMERIC:
                if(DateUtil.isCellDateFormatted(c)) {
                    // 参照POI DateUtil.isADateFormat函数，去除无效字符
                    String fs = c.getCellStyle().getDataFormatString()
                        .replaceAll("\\\\-","-")
                        .replaceAll("\\\\,",",")
                        .replaceAll("\\\\\\.",".")
                        .replaceAll("\\\\ "," ")
                        .replaceAll("AM/PM","")
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
                            fs = "yyyy-MM-dd HH-mm-ss";
                        else
                            fs = String.join(" ", rfs);

                    } else {
                        idx = fs.indexOf(";");
                        if(idx > 0 && idx < fs.length() - 1 ) {
                            fs = fs.substring(0, idx);
                        }
                    }


                    SimpleDateFormat df = new SimpleDateFormat(fs);
                    return df.format(c.getDateCellValue()).trim();
                }

                return String.valueOf((null == cv)? c.getNumericCellValue(): cv.getNumberValue());
            case Cell.CELL_TYPE_STRING:
                return tryMacro((null == cv)? c.getStringCellValue().trim(): cv.getStringValue());
            default:
                return "";
        }
    }

    /**
     * 单元格数据转换（Integer）
     *
     * @param row 行
     * @param col 列号
     * @return
     */
    static public Long cell2i(Row row, int col) {
        return cell2i(row, col, null);
    }

    /**
     * 单元格数据转换（Integer）
     *
     * @param row    行
     * @param col    列号
     * @param evalor 公式管理器
     * @return
     */
    static public Long cell2i(Row row, int col, FormulaEvaluator evalor) {
        if (null == row)
            return 0L;

        Cell c = row.getCell(col);
        if (null == c)
            return 0L;

        CellValue cv = null;
        if (null != evalor && Cell.CELL_TYPE_FORMULA == c.getCellType())
            cv = evalor.evaluate(c);

        int type = (null == cv)? c.getCellType(): cv.getCellType();
        switch (type) {
            case Cell.CELL_TYPE_BLANK:
                return 0L;
            case Cell.CELL_TYPE_BOOLEAN:
                return c.getBooleanCellValue() ? 1L : 0L;
            case Cell.CELL_TYPE_ERROR:
                return 0L;
            case Cell.CELL_TYPE_FORMULA:
                return 0L;
            case Cell.CELL_TYPE_NUMERIC:
                return Math.round(c.getNumericCellValue());
            case Cell.CELL_TYPE_STRING:
                return Math.round(Double.valueOf(tryMacro(c.getStringCellValue().trim())));
            default:
                return 0L;
        }
    }

    /**
     * 单元格数据转换（Double）
     *
     * @param row 行
     * @param col 列号
     * @return
     */
    static public Double cell2d(Row row, int col) {
        return cell2d(row, col, null);
    }

    /**
     * 单元格数据转换（Double）
     *
     * @param row    行
     * @param col    列号
     * @param evalor 公式管理器
     * @return
     */
    static public Double cell2d(Row row, int col, FormulaEvaluator evalor) {
        if (null == row)
            return 0.0;

        Cell c = row.getCell(col);
        if (null == c)
            return 0.0;

        CellValue cv = null;
        if (null != evalor && Cell.CELL_TYPE_FORMULA == c.getCellType())
            cv = evalor.evaluate(c);

        int type = (null == cv)? c.getCellType(): cv.getCellType();
        switch (type) {
            case Cell.CELL_TYPE_BLANK:
                return 0.0;
            case Cell.CELL_TYPE_BOOLEAN:
                return c.getBooleanCellValue() ? 1.0 : 0.0;
            case Cell.CELL_TYPE_ERROR:
                return 0.0;
            case Cell.CELL_TYPE_FORMULA:
                return 0.0;
            case Cell.CELL_TYPE_NUMERIC:
                return c.getNumericCellValue();
            case Cell.CELL_TYPE_STRING:
                return Double.valueOf(tryMacro(c.getStringCellValue().trim()));
            default:
                return 0.0;
        }
    }

    /**
     * 单元格数据转换（boolean）
     *
     * @param row 行
     * @param col 列号
     * @return
     */
    static public Boolean cell2b(Row row, int col) {
        return cell2b(row, col, null);
    }

    /**
     * 单元格数据转换（boolean）
     *
     * @param row    行
     * @param col    列号
     * @param evalor 公式管理器
     * @return
     */
    static public Boolean cell2b(Row row, int col, FormulaEvaluator evalor) {
        if (null == row)
            return false;

        Cell c = row.getCell(col);
        if (null == c)
            return false;

        CellValue cv = null;
        if (null != evalor && Cell.CELL_TYPE_FORMULA == c.getCellType())
            cv = evalor.evaluate(c);

        int type = (null == cv)? c.getCellType(): cv.getCellType();
        switch (type) {
            case Cell.CELL_TYPE_BLANK:
                return false;
            case Cell.CELL_TYPE_BOOLEAN:
                return c.getBooleanCellValue();
            case Cell.CELL_TYPE_ERROR:
                return false;
            case Cell.CELL_TYPE_FORMULA:
                return false;
            case Cell.CELL_TYPE_NUMERIC:
                return c.getNumericCellValue() != 0;
            case Cell.CELL_TYPE_STRING:
                return !c.getStringCellValue().trim().isEmpty() && tryMacro(c.getStringCellValue().trim()) != "0";
            default:
                return false;
        }
    }
}
