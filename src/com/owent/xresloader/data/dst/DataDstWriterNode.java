package com.owent.xresloader.data.dst;

import com.owent.xresloader.engine.IdentifyDescriptor;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by owentou on 2014/10/11.
 */
public class DataDstWriterNode {
    public enum JAVA_TYPE {
        INT, LONG, BOOLEAN, STRING, BYTES, FLOAT, DOUBLE, MESSAGE
    }

    public static class DataDstChildrenNode {
        public boolean isList = false;
        public Object fieldDescriptor = null;
        public ArrayList<DataDstWriterNode> nodes = null;
    }

    private HashMap<String, DataDstChildrenNode> children = null;
    private JAVA_TYPE type = JAVA_TYPE.INT;
    public Object descriptor = null;
    public IdentifyDescriptor identify = null;

    public DataDstWriterNode(Object desc, JAVA_TYPE _type) {
        descriptor = desc;
        type = _type;
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

    public HashMap<String, DataDstChildrenNode> getChildren() {
        if (null == children)
            children = new HashMap<String, DataDstChildrenNode>();
        return children;
    }

    public JAVA_TYPE getType() { return type; }

    public String getChildPath(String prefix, int list_index, String child_name) {
        DataDstChildrenNode res = getChildren().getOrDefault(child_name, null);
        if (null == res)
            return null;

        if (res.isList)
            return String.format("%s[%d].%s", prefix, list_index, child_name);

        return getChildPath(prefix, child_name);
    }

    public String getChildPath(String prefix, String child_name) {
        return prefix.isEmpty() ? child_name : String.format("%s.%s", prefix, child_name);
    }

    public void addChild(String child_name, DataDstWriterNode node, Object _field_descriptor, boolean isList) {
        DataDstChildrenNode res = getChildren().getOrDefault(child_name, null);
        if (null == res) {
            res = new DataDstChildrenNode();
            getChildren().put(child_name, res);
            res.isList = isList;
            res.fieldDescriptor = _field_descriptor;
        }

        if (null == res.nodes) {
            res.nodes = new ArrayList<DataDstWriterNode>();
        }
        res.nodes.add(node);
    }

    /**
     * 创建节点
     * @param _descriptor 原始协议描述器
     * @param _type Java映射类型
     * @return 创建爱你的节点
     */
    static public DataDstWriterNode create(Object _descriptor, JAVA_TYPE _type) {
        return new DataDstWriterNode(_descriptor,  _type);
    }
}
