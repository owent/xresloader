package com.owent.xresloader.engine;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.src.DataSrcImpl;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
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
     * 打开Excel文件
     *
     * @param file_path 文件路径
     * @return Excel Workbook对象
     */
    static public Workbook openWorkbook(String file_path) {
        Workbook ret = null;
        if(!IdentifyEngine.isAbsPath(file_path)) {
            file_path = ProgramOptions.getInstance().dataSourceDirectory + '/' + file_path;
        }

        FileInputStream is = null;
        try {
            is = new FileInputStream(file_path);

            // 类型枚举，以后能支持 ods等非微软格式？
            if (file_path.endsWith(".xls"))
                ret = new HSSFWorkbook(is);
            else
                ret = new XSSFWorkbook(is);

        } catch (java.io.IOException e) {
            e.printStackTrace();
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

        if (null != evalor && Cell.CELL_TYPE_FORMULA == c.getCellType())
            evalor.evaluateInCell(c);

        switch (c.getCellType()) {
            case Cell.CELL_TYPE_BLANK:
                return "";
            case Cell.CELL_TYPE_BOOLEAN:
                return String.valueOf(c.getBooleanCellValue());
            case Cell.CELL_TYPE_ERROR:
                return String.valueOf(c.getErrorCellValue());
            case Cell.CELL_TYPE_FORMULA:
                return c.getCellFormula();
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
                return String.valueOf(c.getNumericCellValue());
            case Cell.CELL_TYPE_STRING:
                return tryMacro(c.getStringCellValue().trim());
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

        if (null != evalor && Cell.CELL_TYPE_FORMULA == c.getCellType())
            evalor.evaluateInCell(c);

        switch (c.getCellType()) {
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

        if (null != evalor && Cell.CELL_TYPE_FORMULA == c.getCellType())
            evalor.evaluateInCell(c);

        switch (c.getCellType()) {
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

        if (null != evalor && Cell.CELL_TYPE_FORMULA == c.getCellType())
            evalor.evaluateInCell(c);

        switch (c.getCellType()) {
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
