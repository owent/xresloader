package org.xresloader.core.data.dst;

import org.xresloader.core.engine.IdentifyDescriptor;
import org.xresloader.Xresloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by owentou on 2014/10/11.
 */
public class DataDstWriterNode {
    public enum JAVA_TYPE {
        INT, LONG, BOOLEAN, STRING, BYTES, FLOAT, DOUBLE, MESSAGE
    }

    public static class DataDstChildrenNode {
        public boolean isList = false;
        public boolean isRequired = false;
        public Object fieldDescriptor = null;
        public ArrayList<DataDstWriterNode> nodes = null;
    }

    public class DataDstMessageExtUE {
        public String helper = null;
    }

    public class DataDstMessageExt {
        public String description = null;
        public List<Xresloader.IndexDescriptor> kvIndex = null;
        public List<Xresloader.IndexDescriptor> klIndex = null;

        private DataDstMessageExtUE ue = null;

        public DataDstMessageExtUE mutableUE() {
            if (null != ue) {
                return ue;
            }

            ue = new DataDstMessageExtUE();
            return ue;
        }
    }

    private HashMap<String, DataDstChildrenNode> children = null;
    private JAVA_TYPE type = JAVA_TYPE.INT;
    public Object descriptor = null; // 关联的Message描述信息，不同的DataDstImpl子类不一样。这里的数据和抽象数据结构的类型有关
    public IdentifyDescriptor identify = null; // 关联的Field信息，同一个抽象数据结构类型可能对应的数据几乎不一样。和具体某个结构内的字段有关
    public String packageName = null;
    private DataDstMessageExt extension = null;

    private DataDstWriterNode(Object desc, JAVA_TYPE _type, String pkgName) {
        descriptor = desc;
        type = _type;
        packageName = pkgName;
    }

    public DataDstMessageExt mutableExtension() {
        if (null != extension) {
            return extension;
        }

        extension = new DataDstMessageExt();
        return extension;
    }

    static public String makeChildPath(String prefix, String child_name, int list_index) {
        if (list_index >= 0)
            return prefix.isEmpty() ? String.format("%s[%d]", child_name, list_index)
                    : String.format("%s.%s[%d]", prefix, child_name, list_index);

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

    public JAVA_TYPE getType() {
        return type;
    }

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

    public void addChild(String child_name, DataDstWriterNode node, Object _field_descriptor, boolean isList,
            boolean isRequired) {
        DataDstChildrenNode res = getChildren().getOrDefault(child_name, null);
        if (null == res) {
            res = new DataDstChildrenNode();
            getChildren().put(child_name, res);
            res.isList = isList;
            res.isRequired = isRequired;
            res.fieldDescriptor = _field_descriptor;
        }

        if (null == res.nodes) {
            res.nodes = new ArrayList<DataDstWriterNode>();
        }
        res.nodes.add(node);
    }

    /**
     * 创建节点
     * 
     * @param _descriptor 原始协议描述器
     * @param _type       Java映射类型
     * @return 创建爱你的节点
     */
    static public DataDstWriterNode create(Object _descriptor, JAVA_TYPE _type, String pkgName) {
        return new DataDstWriterNode(_descriptor, _type, pkgName);
    }
}
