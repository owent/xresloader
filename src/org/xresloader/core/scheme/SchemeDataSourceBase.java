package org.xresloader.core.scheme;

import java.util.ArrayList;

/**
 * Created by owentou on 2015/04/29.
 */
public abstract class SchemeDataSourceBase implements SchemeDataSourceImpl {

    /**
     * 设置一项转表配置
     * 
     * @param key   配置Key
     * @param datas 配置Value
     * @return 成功或失败
     */
    protected boolean set_scheme(String key, ArrayList<String> datas) {
        while (datas.size() < 3) {
            datas.add(null);
        }

        key = key.trim();
        if (key.isEmpty()) {
            return true;
        }

        // 基础配置
        if (key.equalsIgnoreCase("DataSource")) {
            SchemeConf.getInstance().addDataSource(datas.get(0), datas.get(1), datas.get(2));
        } else if (key.equalsIgnoreCase("MacroSource")) {
            SchemeConf.getInstance().addMacroSource(datas.get(0), datas.get(1), datas.get(2));
        }

        // 字段映射配置
        else if (key.equalsIgnoreCase("ProtoName")) {
            SchemeConf.getInstance().setProtoName(datas.get(0));
        } else if (key.equalsIgnoreCase("OutputFile")) {
            SchemeConf.getInstance().setOutputFile(datas.get(0));
        } else if (key.equalsIgnoreCase("KeyRow")) {
            SchemeConf.getInstance().getKey()
                    .setRow(datas.get(0).isEmpty() ? 0 : (int) Math.round(Double.valueOf(datas.get(0))));
        } else if (key.equalsIgnoreCase("KeyCase")) {
            String letter_case = datas.get(0).toLowerCase();
            if (letter_case.equals("大写") || letter_case.equals("upper")) {
                SchemeConf.getInstance().getKey().setLetterCase(SchemeKeyConf.KeyCase.UPPER);
            } else if (letter_case.equals("小写") || letter_case.equals("lower")) {
                SchemeConf.getInstance().getKey().setLetterCase(SchemeKeyConf.KeyCase.LOWER);
            } else {
                SchemeConf.getInstance().getKey().setLetterCase(SchemeKeyConf.KeyCase.NONE);
            }
        } else if (key.equalsIgnoreCase("KeyWordSplit")) {
            SchemeConf.getInstance().getKey().setWordSplit(datas.get(0));
        } else if (key.equalsIgnoreCase("KeyPrefix")) {
            SchemeConf.getInstance().getKey().setPrefix(datas.get(0));
        } else if (key.equalsIgnoreCase("KeySuffix")) {
            SchemeConf.getInstance().getKey().setSuffix(datas.get(0));
        } else if (key.equalsIgnoreCase("KeyWordRegex")) {
            SchemeConf.getInstance().getKey().buildKeyWordRegex(datas.get(0));
            SchemeConf.getInstance().getKey().buildKeyWordRegexRemoveRule(datas.get(1));
            SchemeConf.getInstance().getKey().buildKeyWordRegexPrefixRule(datas.get(2));
        } else if (key.equalsIgnoreCase("Encoding")) {
            SchemeConf.getInstance().getKey().setEncoding(datas.get(0));
        } else if (key.equalsIgnoreCase("UeCfg-UProperty")) {
            SchemeConf.getInstance().setUEOptions(datas.get(0), datas.get(1), datas.get(2));
        } else if (key.equalsIgnoreCase("UeCfg-CodeOutput")) {
            SchemeConf.getInstance().setUECodeOutput(datas.get(0), datas.get(1), datas.get(2));
        } else if (key.equalsIgnoreCase("UeCfg-CaseConvert")) {
            SchemeConf.getInstance().setUECaseConvert(datas.get(0));
        } else if (key.equalsIgnoreCase("UeCfg-DestinationPath")) {
            SchemeConf.getInstance().setUEDestinationPath(datas.get(0));
        } else if (key.equalsIgnoreCase("UeCfg-CsvObjectWrapper")) {
            SchemeConf.getInstance().setUECsvObjectWrapper(datas.get(0), datas.get(1));
        } else if (key.equalsIgnoreCase("CallbackScript")) {
            SchemeConf.getInstance().setCallbackScriptPath(datas.get(0));
        } else {
            return false;
        }

        return true;
    }

    public boolean isSupportMultipleScheme() {
        return true;
    }
}
