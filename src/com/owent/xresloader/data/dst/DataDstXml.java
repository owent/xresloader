package com.owent.xresloader.data.dst;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.scheme.SchemeConf;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.*;

/**
 * Created by owentou on 2014/10/10.
 */
public class DataDstXml extends DataDstJava {
    @Override
    public boolean init() {
        return true;
    }

    @Override
    public final byte[] build(DataDstWriterNode desc) {
        // pretty print
        OutputFormat of = null;
        if (ProgramOptions.getInstance().prettyIndent <= 0) {
            of = OutputFormat.createCompactFormat();
        } else {
            of = OutputFormat.createPrettyPrint();
            of.setIndentSize(ProgramOptions.getInstance().prettyIndent);
        }

        // build data
        DataDstObject data_obj = build_data(desc);

        // build xml tree
        Document doc = DocumentHelper.createDocument();
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null != encoding && false == encoding.isEmpty()) {
            doc.setXMLEncoding(encoding);
            of.setEncoding(encoding);
        }

        doc.setRootElement(DocumentHelper.createElement("root"));

        Element header = DocumentHelper.createElement("header");
        Element body = DocumentHelper.createElement("body");

        writeData(header, data_obj.header, header.getName());

        // body
        for(Map.Entry<String, List<Object> > item: data_obj.body.entrySet()) {
            for(Object obj: item.getValue()) {
                Element xml_item = DocumentHelper.createElement(item.getKey());

                writeData(xml_item, obj, item.getKey());

                body.add(xml_item);
            }
        }

        writeData(body, data_obj.body, body.getName());

        doc.getRootElement().add(header);
        doc.getRootElement().add(body);

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            XMLWriter writer = new XMLWriter(bos, of);
            writer.write(doc);

            return bos.toByteArray();
        } catch(Exception e) {
            System.err.println(String.format("[ERROR] write xml failed, %s", e.getMessage()));
            return null;
        }
    }

    @Override
    public final DataDstWriterNode compile() {
        System.err.println("[ERROR] lua can not be protocol description.");
        return null;
    }

    private void writeData(Element sb, Object data, String wrapper_name) {
        // null
        if (null == data) {
            return;
        }

        // 数字
        // 枚举值已被转为Java Long，会在这里执行
        if (data instanceof Number) {
            sb.addText(data.toString());
            return;
        }

        // 布尔
        if (data instanceof Boolean) {
            sb.addText(((Boolean) data) ? "true" : "false");
            return;
        }

        // 字符串&二进制
        if (data instanceof String) {
            sb.addText(data.toString());
            return;
        }

        // 列表
        if (data instanceof List) {
            Element list_ele = DocumentHelper.createElement(wrapper_name);

            List<Object> ls = (List<Object>)data;

            for(Object obj: ls) {
                Element item = DocumentHelper.createElement("item");
                DocumentHelper.createAttribute(item, "for", wrapper_name);

                writeData(item, obj, wrapper_name);
                list_ele.add(item);
            }

            sb.add(list_ele);
            return;
        }

        // Hashmap
        if (data instanceof Map) {
            Map<String, Object> mp = (Map<String, Object>)data;

            for(Map.Entry<String, Object> item: mp.entrySet()) {
                Element xml_item = DocumentHelper.createElement(item.getKey());
                writeData(xml_item, item.getValue(), item.getKey());

                sb.add(xml_item);
            }

            return;
        }

        System.out.println(String.format("[ERROR] %s not support.", data.toString()));
    }

    /**
     * 转储常量数据
     * @return 常量数据,不支持的时候返回空
     */
    public final byte[] dumpConst(HashMap<String, Object> data) {
        // pretty print
        OutputFormat of = null;
        if (ProgramOptions.getInstance().prettyIndent <= 0) {
            of= OutputFormat.createCompactFormat();
        } else {
            of = OutputFormat.createPrettyPrint();
            of.setIndentSize(ProgramOptions.getInstance().prettyIndent);
        }

        // build xml tree
        Document doc = DocumentHelper.createDocument();
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null != encoding && false == encoding.isEmpty()) {
            doc.setXMLEncoding(encoding);
            of.setEncoding(encoding);
        }

        doc.setRootElement(DocumentHelper.createElement("const"));
        writeData(doc.getRootElement(), data, "");

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            XMLWriter writer = new XMLWriter(bos, of);
            writer.write(doc);

            return bos.toByteArray();
        } catch(Exception e) {
            System.err.println(String.format("[ERROR] write xml failed, %s", e.getMessage()));
            return null;
        }
    };
}
