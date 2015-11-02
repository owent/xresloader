package com.owent.xresloader.scheme;

import java.util.regex.Pattern;

/**
 * Created by owentou on 2014/9/30.
 */
public class SchemeKeyConf {
    private int row;

    private KeyCase letterCase = KeyCase.NONE;
    private String wordSplit = "_";
    private String prefix = "";
    private String suffix = "";
    private Pattern keyWordRegex = null;
    private Pattern keyWordRegexRemoveRule = null;
    private Pattern keyWordRegexPrefixRule = null;
    private String encoding = "utf-8";

    /**
     * Getter for property 'row'.
     *
     * @return Value for property 'row'.
     */
    public int getRow() {
        return row;
    }

    /**
     * Setter for property 'row'.
     *
     * @param row Value to set for property 'row'.
     */
    public void setRow(int row) {
        this.row = row;
    }

    /**
     * Getter for property 'letterCase'.
     *
     * @return Value for property 'letterCase'.
     */
    public KeyCase getLetterCase() {
        return letterCase;
    }

    /**
     * Setter for property 'letterCase'.
     *
     * @param letterCase Value to set for property 'letterCase'.
     */
    public void setLetterCase(KeyCase letterCase) {
        this.letterCase = letterCase;
    }

    /**
     * Getter for property 'wordSplit'.
     *
     * @return Value for property 'wordSplit'.
     */
    public String getWordSplit() {
        return wordSplit;
    }

    /**
     * Setter for property 'wordSplit'.
     *
     * @param wordSplit Value to set for property 'wordSplit'.
     */
    public void setWordSplit(String wordSplit) {
        this.wordSplit = wordSplit;
    }

    /**
     * Getter for property 'prefix'.
     *
     * @return Value for property 'prefix'.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Setter for property 'prefix'.
     *
     * @param prefix Value to set for property 'prefix'.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Getter for property 'suffix'.
     *
     * @return Value for property 'suffix'.
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Setter for property 'suffix'.
     *
     * @param suffix Value to set for property 'suffix'.
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public Pattern getKeyWordRegex() {
        return keyWordRegex;
    }

    public void buildKeyWordRegex(String s) {
        if (!s.isEmpty())
            keyWordRegex = Pattern.compile(s);
    }

    public Pattern getKeyWordRegexRemoveRule() {
        return keyWordRegexRemoveRule;
    }

    public void buildKeyWordRegexRemoveRule(String s) {
        if (!s.isEmpty())
            keyWordRegexRemoveRule = Pattern.compile(s);
    }

    public Pattern getKeyWordRegexPrefixRule() {
        return keyWordRegexPrefixRule;
    }

    public void buildKeyWordRegexPrefixRule(String s) {
        if (!s.isEmpty())
            keyWordRegexPrefixRule = Pattern.compile(s);
    }

    /**
     * Getter for property 'encoding'.
     *
     * @return Value for property 'encoding'.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Setter for property 'encoding'.
     *
     * @param encoding Value to set for property 'encoding'.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    public enum KeyCase {NONE, UPPER, LOWER}
}
