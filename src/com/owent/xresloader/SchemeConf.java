package com.owent.xresloader;

/**
 * Created by owentou on 2014/9/30.
 */
public class SchemeConf {

    /**
     * Getter for property 'dateRectRow'.
     *
     * @return Value for property 'dateRectRow'.
     */
    public int getDateRectRow() {
        return dateRectRow;
    }

    /**
     * Setter for property 'dateRectRow'.
     *
     * @param dateRectRow Value to set for property 'dateRectRow'.
     */
    public void setDateRectRow(int dateRectRow) {
        this.dateRectRow = dateRectRow;
    }

    /**
     * Getter for property 'dateRectCol'.
     *
     * @return Value for property 'dateRectCol'.
     */
    public int getDateRectCol() {
        return dateRectCol;
    }

    /**
     * Setter for property 'dateRectCol'.
     *
     * @param dateRectCol Value to set for property 'dateRectCol'.
     */
    public void setDateRectCol(int dateRectCol) {
        this.dateRectCol = dateRectCol;
    }

    /**
     * Getter for property 'macroRectRow'.
     *
     * @return Value for property 'macroRectRow'.
     */
    public int getMacroRectRow() {
        return macroRectRow;
    }

    /**
     * Setter for property 'macroRectRow'.
     *
     * @param macroRectRow Value to set for property 'macroRectRow'.
     */
    public void setMacroRectRow(int macroRectRow) {
        this.macroRectRow = macroRectRow;
    }

    /**
     * Getter for property 'macroRectCol'.
     *
     * @return Value for property 'macroRectCol'.
     */
    public int getMacroRectCol() {
        return macroRectCol;
    }

    /**
     * Setter for property 'macroRectCol'.
     *
     * @param macroRectCol Value to set for property 'macroRectCol'.
     */
    public void setMacroRectCol(int macroRectCol) {
        this.macroRectCol = macroRectCol;
    }

    /**
     * Getter for property 'protoName'.
     *
     * @return Value for property 'protoName'.
     */
    public String getProtoName() {
        return protoName;
    }

    /**
     * Setter for property 'protoName'.
     *
     * @param protoName Value to set for property 'protoName'.
     */
    public void setProtoName(String protoName) {
        this.protoName = protoName;
    }

    /**
     * Getter for property 'outputFile'.
     *
     * @return Value for property 'outputFile'.
     */
    public String getOutputFile() {
        return outputFile;
    }

    /**
     * Setter for property 'outputFile'.
     *
     * @param outputFile Value to set for property 'outputFile'.
     */
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * Getter for property 'key'.
     *
     * @return Value for property 'key'.
     */
    public SchemeKeyConf getKey() {
        return key;
    }

    /**
     * Setter for property 'key'.
     *
     * @param key Value to set for property 'key'.
     */
    public void setKey(SchemeKeyConf key) {
        this.key = key;
    }

    public String getDateSourceTable() {
        return dateSourceTable;
    }

    public void setDateSourceTable(String dateSourceTable) {
        this.dateSourceTable = dateSourceTable;
    }

    public String getDateSourceFile() {
        return dateSourceFile;
    }

    public void setDateSourceFile(String dateSourceFile) {
        this.dateSourceFile = dateSourceFile;
    }

    public String getMacroSourceTable() {
        return macroSourceTable;
    }

    public void setMacroSourceTable(String macroSourceTable) {
        this.macroSourceTable = macroSourceTable;
    }

    public String getMacroSourceFile() {
        return macroSourceFile;
    }

    public void setMacroSourceFile(String macroSourceFile) {
        this.macroSourceFile = macroSourceFile;
    }

    private String dateSourceFile;
    private String dateSourceTable;
    private int dateRectRow;
    private int dateRectCol;
    private String macroSourceFile;
    private String macroSourceTable;
    private int macroRectRow;
    private int macroRectCol;


    private String protoName;
    private String outputFile;
    private SchemeKeyConf key;

    private SchemeDataSourceImpl scheme;

    public int initScheme() {
        if (ProgramOptions.FileType.EXCEL == ProgramOptions.getInstance().dataSourceType) {
            scheme = new SchemeDataSourceExcel();
        } else {
            System.err.println("[ERROR] data source file type error.");
            return -11;
        }

        return scheme.load();
    }

    /**
     * 单例
     */
    private static SchemeConf instance = null;
    private SchemeConf() {
        key = new SchemeKeyConf();
    }

    public static SchemeConf getInstance() {
        if (instance == null) {
            instance = new SchemeConf();
        }
        return instance;
    }
}
