package com.owent.xresloader.engine;

import com.owent.xresloader.scheme.SchemeConf;
import com.owent.xresloader.scheme.SchemeKeyConf;

import java.util.LinkedList;

/**
 * Created by owentou on 2014/10/10.
 */
public class IdentifyEngine {

    /**
     * 把配置名称转换成标识符
     * @param _name 配置名称
     * @return
     */
    static public String n2i(String _name) {
        String[] segs = _name.trim().split("\\.");

        for(int i = 0; i < segs.length; ++ i) {
            segs[i] = make_word(segs[i]);
        }

        return String.join(".", segs);
    }

    static private String make_word(String ident) {
        SchemeKeyConf cfg = SchemeConf.getInstance().getKey();

        // 过滤不合法前缀
        int invalid_index = 0;
        for(; invalid_index < ident.length(); ++ invalid_index) {
            char c = ident.charAt(invalid_index);
            if(Character.isLetter(c) || '_' == c || '$' == c)
                break;
        }
        ident = ident.substring(invalid_index);

        // 分词
        LinkedList<String> words = new LinkedList<String>();
        String this_word = "";
        for(char c: ident.toCharArray()) {
            if(Character.isUpperCase(c) || '_' == c || '$' == c) {
                if(!this_word.isEmpty()) {
                    if (SchemeKeyConf.KeyCase.LOWER == cfg.getLetterCase())
                        this_word = this_word.toLowerCase();
                    else if (SchemeKeyConf.KeyCase.UPPER == cfg.getLetterCase())
                        this_word = this_word.toUpperCase();

                    words.add(this_word);
                }

                this_word = String.valueOf(c);
            } else {
                this_word += c;
            }
        }

        // 最后一个词
        if(!this_word.isEmpty()) {
            if (SchemeKeyConf.KeyCase.LOWER == cfg.getLetterCase())
                this_word = this_word.toLowerCase();
            else if (SchemeKeyConf.KeyCase.UPPER == cfg.getLetterCase())
                this_word = this_word.toUpperCase();

            words.add(this_word);
        }

        // 加前后缀，加分词符
        return cfg.getPrefix() + String.join(cfg.getWordSplit(), words) + cfg.getSuffix();
    }
}
