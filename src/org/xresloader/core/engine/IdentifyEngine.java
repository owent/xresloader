package org.xresloader.core.engine;

import java.util.LinkedList;

import org.xresloader.core.scheme.SchemeConf;
import org.xresloader.core.scheme.SchemeKeyConf;

/**
 * Created by owentou on 2014/10/10.
 */
public class IdentifyEngine {

    /**
     * 把配置名称转换成标识符描述
     *
     * @param name           配置名称
     * @param dataFieldIndex 数据字段序号（实际值）
     * @return 标识符描述对象
     */
    public static IdentifyDescriptor n2i(String name, int dataFieldIndex) {
        IdentifyDescriptor ret = new IdentifyDescriptor(dataFieldIndex);
        int verifyIndex = name.lastIndexOf('@');
        if (verifyIndex >= 0) {
            ret.dataSourceFieldValidator = name.substring(verifyIndex + 1);
            ret.name = normalize(name.substring(0, verifyIndex));
        } else {
            ret.name = normalize(name);
        }

        return ret;
    }

    /**
     * 把配置名称转换成标识符
     *
     * @param name 配置名称
     * @return
     */
    public static String normalize(String name) {
        String[] segs = name.trim().split("\\.");
        for (int i = 0; i < segs.length; ++i) {
            segs[i] = makeWord(segs[i]).trim();
        }

        return String.join(".", segs);
    }

    private static String makeWord(String ident) {
        SchemeKeyConf cfg = SchemeConf.getInstance().getKey();

        // 如果无分词规则就直接返回
        if (null == cfg.getKeyWordRegex()) {
            if (SchemeKeyConf.KeyCase.LOWER == cfg.getLetterCase())
                ident = ident.toLowerCase();
            else if (SchemeKeyConf.KeyCase.UPPER == cfg.getLetterCase())
                ident = ident.toUpperCase();

            return cfg.getPrefix() + ident + cfg.getSuffix();
        }

        // 过滤不合法前缀
        if (null != cfg.getKeyWordRegexPrefixRule()) {
            int invalidIndex = 0;
            for (; invalidIndex < ident.length(); ++invalidIndex) {
                if (cfg.getKeyWordRegexPrefixRule().matcher(ident.substring(invalidIndex, invalidIndex + 1))
                        .matches())
                    break;
            }
            ident = ident.substring(invalidIndex);
        }

        // 分词
        LinkedList<String> words = new LinkedList<>();
        String thisWord = "";
        for (char c : ident.toCharArray()) {
            if (cfg.getKeyWordRegex().matcher(String.valueOf(c)).matches()) {
                if (!thisWord.isEmpty()) {
                    if (SchemeKeyConf.KeyCase.LOWER == cfg.getLetterCase())
                        thisWord = thisWord.toLowerCase();
                    else if (SchemeKeyConf.KeyCase.UPPER == cfg.getLetterCase())
                        thisWord = thisWord.toUpperCase();

                    words.add(thisWord);
                }

                if (null != cfg.getKeyWordRegexRemoveRule()
                        && !cfg.getKeyWordRegexRemoveRule().matcher(String.valueOf(c)).matches())
                    thisWord = String.valueOf(c);
                else
                    thisWord = "";
            } else {
                thisWord += c;
            }
        }

        // 最后一个词
        if (!thisWord.isEmpty()) {
            if (SchemeKeyConf.KeyCase.LOWER == cfg.getLetterCase())
                thisWord = thisWord.toLowerCase();
            else if (SchemeKeyConf.KeyCase.UPPER == cfg.getLetterCase())
                thisWord = thisWord.toUpperCase();

            words.add(thisWord);
        }

        // 加前后缀，加分词符
        return cfg.getPrefix() + String.join(cfg.getWordSplit(), words) + cfg.getSuffix();
    }
}
