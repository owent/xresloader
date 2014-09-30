package com.owent.xresloader;

/**
 * Created by owentou on 2014/9/30.
 */
public class SchemeKeyConf {
    public enum KeyCase {NONE, UPPER, LOWER};

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

    /**
     * Getter for property 'enableTypeSuffix'.
     *
     * @return Value for property 'enableTypeSuffix'.
     */
    public String getEnableTypeSuffix() {
        return enableTypeSuffix;
    }

    /**
     * Setter for property 'enableTypeSuffix'.
     *
     * @param enableTypeSuffix Value to set for property 'enableTypeSuffix'.
     */
    public void setEnableTypeSuffix(String enableTypeSuffix) {
        this.enableTypeSuffix = enableTypeSuffix;
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

    private int row;
    private KeyCase letterCase;
    private String wordSplit;
    private String prefix;
    private String suffix;
    private String enableTypeSuffix;
    private String encoding;
}
