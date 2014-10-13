package com.owent.xresloader.data.dst;

import java.util.HashMap;

/**
 * Created by owentou on 2014/10/11.
 */
public class DataDstWriterNode {
    private int listCount = 0; // 0 代表非List
    private HashMap<String, DataDstWriterNode> children = null;
    private JavaType type = JavaType.OBJECT;
    public DataDstWriterNode() {
    }

    static public String makeChildPath(String prefix, String child_name, int list_index) {
        if (list_index >= 0)
            return prefix.isEmpty() ?
                    String.format("%s[%d]", child_name, list_index) :
                    String.format("%s.%s[%d]", prefix, child_name, list_index);

        return makeChildPath(prefix, child_name);
    }

    static public String makeChildPath(String prefix, String child_name) {
        return prefix.isEmpty() ? child_name : String.format("%s.%s", prefix, child_name);
    }

    static public String makeNodeName(String _name, int list_index) {
        if (list_index >= 0)
            return String.format("%s[%d]", _name, list_index);

        return makeNodeName(_name);
    }

    static public String makeNodeName(String _name) {
        return _name;
    }

    public JavaType getType() {
        return type;
    }

    public void setType(String type) {
        JavaType t = JavaType.valueOf(type);
        this.type = null == t ? JavaType.OBJECT : t;
    }

    public void setType(JavaType type) {
        this.type = type;
    }

    public boolean isList() {
        return listCount > 0;
    }

    public int getListCount() {
        return listCount;
    }

    public void setListCount(int listCount) {
        this.listCount = listCount;
    }

    public boolean isLeaf() {
        return JavaType.OBJECT != type;
    }

    public HashMap<String, DataDstWriterNode> getChildren() {
        if (null == children)
            children = new HashMap<String, DataDstWriterNode>();
        return children;
    }

    public String getChildPath(String prefix, int list_index, String child_name) {
        if (!getChildren().containsKey(child_name))
            return null;

        if (isList())
            return String.format("%s[%d].%s", prefix, list_index, child_name);

        return getChildPath(prefix, child_name);
    }

    public String getChildPath(String prefix, String child_name) {
        if (!getChildren().containsKey(child_name))
            return null;

        return prefix.isEmpty() ? child_name : String.format("%s.%s", prefix, child_name);
    }

    public void addChild(String child_name, DataDstWriterNode node) {
        getChildren().put(child_name, node);
    }

    public boolean hasChild(String child_name) {
        return children.containsKey(child_name);
    }

    public enum JavaType {INT, LONG, FLOAT, DOUBLE, BOOLEAN, STRING, BYTE_STRING, ENUM, OBJECT}
}
