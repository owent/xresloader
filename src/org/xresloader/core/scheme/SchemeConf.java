package org.xresloader.core.scheme;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.engine.IdentifyEngine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by owentou on 2014/9/30.
 */
public class SchemeConf {

    public class DataInfo {
        public String file_path = "";
        public String table_name = "";
        public int data_row;
        public int data_col;
    }

    public class DataExtUECSV {
        public String blueprintAccess = "BlueprintReadOnly";
        public String category = "XResConfig";
        public String editAccess = "EditAnywhere";
        public Boolean enableCaseConvert = true;
    }

    /**
     * 单例
     */
    private static SchemeConf instance = null;
    private LinkedList<DataInfo> dateSource = new LinkedList<DataInfo>();
    private LinkedList<DataInfo> macroSource = new LinkedList<DataInfo>();
    private String protoName;
    private String outputFile;
    private SchemeKeyConf key;
    private SchemeDataSourceImpl scheme;

    private DataExtUECSV extUECSV = new DataExtUECSV();

    private SchemeConf() {
        key = new SchemeKeyConf();
    }

    public static SchemeConf getInstance() {
        if (instance == null) {
            instance = new SchemeConf();
        }
        return instance;
    }

    public void reset() {
        dateSource = new LinkedList<DataInfo>();
        macroSource = new LinkedList<DataInfo>();
        protoName = "";
        outputFile = "";
        key = new SchemeKeyConf();

        scheme = null;

        extUECSV.blueprintAccess = "BlueprintReadOnly";
        extUECSV.category = "XResConfig";
        extUECSV.editAccess = "EditAnywhere";
        extUECSV.enableCaseConvert = true;
    }

    /**
     * Getter for property 'dateSource'.
     *
     * @return Value for property 'dateSource'.
     */
    public LinkedList<DataInfo> getDataSource() {
        return dateSource;
    }

    /**
     * 添加配置数据源
     * 
     * @param file_path  文件路径
     * @param table_name 表名
     * @param row        行号
     * @param col        列号
     */
    public void addDataSource(String file_path, String table_name, int row, int col) {
        DataInfo data = new DataInfo();
        data.file_path = file_path;
        data.table_name = table_name;
        data.data_row = row;
        data.data_col = col;

        dateSource.add(data);
    }

    /**
     * 添加配置数据源
     * 
     * @param file_path  文件路径
     * @param table_name 表名
     * @param position   行号,列号
     */
    public void addDataSource(String file_path, String table_name, String position) {
        int row = 2;
        int col = 1;

        String[] group = position.split("[^\\d]");
        ArrayList<String> valid_group = new ArrayList<String>();
        for (String n : group) {
            if (false == n.isEmpty()) {
                valid_group.add(n);
            }
        }

        if (valid_group.size() >= 1) {
            row = Integer.parseInt(valid_group.get(0));
        }

        if (valid_group.size() >= 2) {
            col = Integer.parseInt(valid_group.get(1));
        }

        addDataSource(file_path, table_name, row, col);
    }

    /**
     * Getter for property 'macroSource'.
     *
     * @return Value for property 'macroSource'.
     */
    public LinkedList<DataInfo> getMacroSource() {
        return macroSource;
    }

    /**
     * 添加宏数据源
     * 
     * @param file_path  文件路径
     * @param table_name 表名
     * @param row        行号
     * @param col        列号
     */
    public void addMacroSource(String file_path, String table_name, int row, int col) {
        DataInfo data = new DataInfo();
        data.file_path = file_path;
        data.table_name = table_name;
        data.data_row = row;
        data.data_col = col;

        macroSource.add(data);
    }

    /**
     * 添加宏数据源
     * 
     * @param file_path  文件路径
     * @param table_name 表名
     * @param position   行号,列号
     */
    public void addMacroSource(String file_path, String table_name, String position) {
        int row = 2;
        int col = 1;

        String[] group = position.split("[^\\d]");
        ArrayList<String> valid_group = new ArrayList<String>();
        for (String n : group) {
            if (false == n.isEmpty()) {
                valid_group.add(n);
            }
        }

        if (valid_group.size() >= 1) {
            row = Integer.parseInt(valid_group.get(0));
        }

        if (valid_group.size() >= 2) {
            col = Integer.parseInt(valid_group.get(1));
        }

        addMacroSource(file_path, table_name, row, col);
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

    private String outputFilePathCache = "";
    private String outputFileAbsPathCache = "";

    /**
     * Getter for property 'outputFile'.
     *
     * @return Value for property 'outputFile'.
     */
    public String getOutputFile() {
        if (!this.outputFilePathCache.isEmpty()) {
            return this.outputFilePathCache;
        }

        if (false == ProgramOptions.getInstance().constPrint.isEmpty()) {
            this.outputFilePathCache = ProgramOptions.getInstance().constPrint;
            return this.outputFilePathCache;
        }

        ProgramOptions.RenameRule rename_rule = ProgramOptions.getInstance().renameRule;
        if (null != rename_rule) {
            this.outputFilePathCache = rename_rule.match.matcher(outputFile).replaceAll(rename_rule.replace);
        }

        return this.outputFilePathCache;
    }

    public String getOutputFileAbsPath() {
        if (!this.outputFileAbsPathCache.isEmpty()) {
            return this.outputFileAbsPathCache;
        }

        String filePath = this.getOutputFile();
        File fd = new File(filePath);
        if (!fd.isAbsolute()) {
            filePath = ProgramOptions.getInstance().outputDirectory + '/' + filePath;
            fd = new File(filePath);
        }

        try {
            this.outputFileAbsPathCache = fd.getCanonicalFile().getAbsolutePath();
        } catch (IOException e) {
            this.outputFileAbsPathCache = fd.getAbsolutePath();
        }
        return this.outputFileAbsPathCache;
    }

    /**
     * Setter for property 'outputFile'.
     *
     * @param outputFile Value to set for property 'outputFile'.
     */
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
        this.outputFilePathCache = "";
        this.outputFileAbsPathCache = "";
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

    public SchemeDataSourceImpl getScheme() {
        return scheme;
    }

    public int initScheme() {
        switch (ProgramOptions.getInstance().dataSourceType) {
        case BIN: {
            scheme = new SchemeDataSourceCmd();
            break;
        }
        case EXCEL: {
            scheme = new SchemeDataSourceExcel();
            break;
        }
        case INI: {
            scheme = new SchemeDataSourceConf();
            break;
        }
        case JSON: {
            scheme = new SchemeDataSourceJson();
            break;
        }
        default: {
            ProgramOptions.getLoger().error("data source file type error.");
            return -11;
        }
        }

        return scheme.load();
    }

    /**
     * 添加配置数据源
     * 
     * @param category        分类名称
     * @param blueprintAccess 蓝图权限(BlueprintReadOnly/BlueprintReadWrite/BlueprintGetter/BlueprintSetter)
     * @param editAccess      编辑权限(EditAnywhere/EditInstanceOnly/EditDefaultsOnly)
     */
    public void setUECSVOptions(String category, String blueprintAccess, String editAccess) {
        extUECSV.category = category;

        if (blueprintAccess == null || blueprintAccess.isEmpty()) {
            extUECSV.blueprintAccess = "";
        } else if (blueprintAccess.equalsIgnoreCase("BlueprintReadOnly")
                || blueprintAccess.equalsIgnoreCase("BlueprintReadWrite")
                || blueprintAccess.equalsIgnoreCase("BlueprintGetter")
                || blueprintAccess.equalsIgnoreCase("BlueprintSetter")) {
            extUECSV.blueprintAccess = blueprintAccess;
        } else if (!blueprintAccess.isEmpty()) {
            ProgramOptions.getLoger().warn(
                    "BlueprintAccess for UECSV can only be one of BlueprintReadOnly/BlueprintReadWrite/BlueprintGetter/BlueprintSetter, the invalid %s will be ignored",
                    blueprintAccess);
        }

        if (editAccess != null || editAccess.isEmpty()) {
            extUECSV.editAccess = "";
        } else if (editAccess.equalsIgnoreCase("EditAnywhere") || editAccess.equalsIgnoreCase("EditInstanceOnly")
                || editAccess.equalsIgnoreCase("EditDefaultsOnly")) {
            extUECSV.editAccess = editAccess;
        } else if (!editAccess.isEmpty()) {
            ProgramOptions.getLoger().warn(
                    "EditAccess for UECSV can only be one of EditAnywhere/EditInstanceOnly/EditDefaultsOnly, the invalid %s will be ignored",
                    editAccess);
        }
    }

    public void setUECSVCaseConvert(String data) {
        if (data == null || data.isEmpty() || 0 == data.compareTo("0") || 0 == data.compareToIgnoreCase("no")
                || 0 == data.compareToIgnoreCase("false") || 0 == data.compareToIgnoreCase("disable")) {
            extUECSV.enableCaseConvert = false;
        } else {
            extUECSV.enableCaseConvert = true;
        }
    }

    public DataExtUECSV getUECSVOptions() {
        return extUECSV;
    }
}
