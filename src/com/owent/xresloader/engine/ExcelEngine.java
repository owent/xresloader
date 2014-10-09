package com.owent.xresloader.engine;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;

/**
 * Created by owentou on 2014/10/9.
 */
public class ExcelEngine {

    /**
     * 打开Excel文件
     * @param file_path 文件路径
     * @return Excel Workbook对象
     */
    static public Workbook openWorkbook(String file_path) {
        Workbook ret = null;
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
     * @param file_path Excel文件
     * @param sheet_name 表名
     * @return Sheet对象
     */
    static public Sheet openSheet(String file_path, String sheet_name) {
        Workbook wb = openWorkbook(file_path);
        if (null == wb)
            return null;

        return wb.getSheet(sheet_name);
    }

    /**
     * 单元格数据转换（String）
     * @param row 行
     * @param col 列号
     * @return
     */
    static public String cell2str(Row row, int col) {
        return cell2str(row, col, null);
    }

    /**
     * 单元格数据转换（String）
     * @param row 行
     * @param col 列号
     * @param evalor 公式管理器
     * @return
     */
    static public String cell2str(Row row, int col, FormulaEvaluator evalor) {
        if (null == row)
            return "";

        Cell c = row.getCell(col);
        if (null == c)
            return "";

        if (null != evalor && Cell.CELL_TYPE_FORMULA == c.getCellType())
            evalor.evaluateInCell(c);

        switch (c.getCellType()){
            case Cell.CELL_TYPE_BLANK:
                return "";
            case Cell.CELL_TYPE_BOOLEAN:
                return String.valueOf(c.getBooleanCellValue());
            case Cell.CELL_TYPE_ERROR:
                return String.valueOf(c.getErrorCellValue());
            case Cell.CELL_TYPE_FORMULA:
                return c.getCellFormula();
            case Cell.CELL_TYPE_NUMERIC:
                return String.valueOf(c.getNumericCellValue());
            case Cell.CELL_TYPE_STRING:
                return c.getStringCellValue().trim();
            default:
                return "";
        }
    }

    /**
     * 单元格数据转换（Integer）
     * @param row 行
     * @param col 列号
     * @return
     */
    static public int cell2int(Row row, int col) {
        return cell2int(row, col, null);
    }

    /**
     * 单元格数据转换（Integer）
     * @param row 行
     * @param col 列号
     * @param evalor 公式管理器
     * @return
     */
    static public int cell2int(Row row, int col, FormulaEvaluator evalor) {
        if (null == row)
            return 0;

        Cell c = row.getCell(col);
        if (null == c)
            return 0;

        if (null != evalor && Cell.CELL_TYPE_FORMULA == c.getCellType())
            evalor.evaluateInCell(c);

        switch (c.getCellType()){
            case Cell.CELL_TYPE_BLANK:
                return 0;
            case Cell.CELL_TYPE_BOOLEAN:
                return c.getBooleanCellValue()? 1: 0;
            case Cell.CELL_TYPE_ERROR:
                return 0;
            case Cell.CELL_TYPE_FORMULA:
                return 0;
            case Cell.CELL_TYPE_NUMERIC:
                return (int)c.getNumericCellValue();
            case Cell.CELL_TYPE_STRING:
                return Integer.parseInt(c.getStringCellValue().trim());
            default:
                return 0;
        }
    }

    /**
     * 单元格数据转换（Double）
     * @param row 行
     * @param col 列号
     * @return
     */
    static public double cell2d(Row row, int col) {
        return cell2d(row, col, null);
    }

    /**
     * 单元格数据转换（Double）
     * @param row 行
     * @param col 列号
     * @param evalor 公式管理器
     * @return
     */
    static public double cell2d(Row row, int col, FormulaEvaluator evalor) {
        if (null == row)
            return 0.0;

        Cell c = row.getCell(col);
        if (null == c)
            return 0.0;

        if (null != evalor && Cell.CELL_TYPE_FORMULA == c.getCellType())
            evalor.evaluateInCell(c);

        switch (c.getCellType()){
            case Cell.CELL_TYPE_BLANK:
                return 0.0;
            case Cell.CELL_TYPE_BOOLEAN:
                return c.getBooleanCellValue()? 1.0: 0.0;
            case Cell.CELL_TYPE_ERROR:
                return 0.0;
            case Cell.CELL_TYPE_FORMULA:
                return 0.0;
            case Cell.CELL_TYPE_NUMERIC:
                return c.getNumericCellValue();
            case Cell.CELL_TYPE_STRING:
                return Double.parseDouble(c.getStringCellValue().trim());
            default:
                return 0.0;
        }
    }

    /**
     * 单元格数据转换（boolean）
     * @param row 行
     * @param col 列号
     * @return
     */
    static public boolean cell2b(Row row, int col) {
        return cell2b(row, col, null);
    }

    /**
     * 单元格数据转换（boolean）
     * @param row 行
     * @param col 列号
     * @param evalor 公式管理器
     * @return
     */
    static public boolean cell2b(Row row, int col, FormulaEvaluator evalor) {
        if (null == row)
            return false;

        Cell c = row.getCell(col);
        if (null == c)
            return false;

        if (null != evalor && Cell.CELL_TYPE_FORMULA == c.getCellType())
            evalor.evaluateInCell(c);

        switch (c.getCellType()){
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
                return !c.getStringCellValue().trim().isEmpty() && c.getStringCellValue().trim() != "0";
            default:
                return false;
        }
    }
}
