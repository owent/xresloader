/**
 *
 */
package com.owent.xresloader;

import com.owent.xresloader.data.dst.*;
import com.owent.xresloader.data.src.DataSrcExcel;
import com.owent.xresloader.data.src.DataSrcImpl;
import com.owent.xresloader.engine.IdentifyEngine;
import com.owent.xresloader.scheme.SchemeConf;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
/**
 * @author owentou
 */
public class main {

    private static DataDstImpl get_out_desc(DataDstImpl proto_desc) {
        DataDstImpl outDesc = null;
        switch (ProgramOptions.getInstance().outType) {
            case BIN:
                outDesc = proto_desc;
                break;
            case LUA:
                outDesc = new DataDstLua();
                outDesc = outDesc.init() ? outDesc : null;
                break;
            case MSGPACK:
                outDesc = new DataDstMsgPack();
                outDesc = outDesc.init() ? outDesc : null;
                break;
            case JSON:
                outDesc = new DataDstJson();
                outDesc = outDesc.init() ? outDesc : null;
                break;
            case XML:
                outDesc = new DataDstXml();
                outDesc = outDesc.init() ? outDesc : null;
                break;
            default:
                System.err.println(String.format("[ERROR] output type \"%s\" invalid", ProgramOptions.getInstance().outType.toString()));
                break;
        }

        return outDesc;
    }

    private static int print_const_data() {
        // 1. 协议描述文件
        DataDstImpl protoDesc = null;
        switch (ProgramOptions.getInstance().protocol) {
            case PROTOBUF:
                protoDesc = new DataDstPb();
                break;
            default:
                System.err.println(String.format("[ERROR] protocol type \"%s\" invalid", ProgramOptions.getInstance().protocol.toString()));
                break;
        }

        if (null == protoDesc)
            return 1;

        DataDstImpl outDesc = get_out_desc(protoDesc);
        if (null == outDesc)
            return 1;

        HashMap<String, Object> enum_data = protoDesc.buildConst();

        if (null == enum_data) {
            System.err.println(String.format("[ERROR] protocol desc \"%s\" init and build const values failed", ProgramOptions.getInstance().protocol.toString()));
            return 1;
        }

        try {
            String filePath = ProgramOptions.getInstance().constPrint;
            if(!IdentifyEngine.isAbsPath(filePath))
                filePath = ProgramOptions.getInstance().outputDirectory + '/' + filePath;
            OutputStream fos = new FileOutputStream(filePath, false);
            byte[] data = outDesc.dumpConst(enum_data);

            if (null != data) {
                fos.write(data);
            } else {
                System.err.println(String.format("[ERROR] write const data to file \"%s\" failed, output type invalid.", ProgramOptions.getInstance().constPrint));
                return 1;
            }
        } catch (java.io.IOException e) {
            System.err.println(String.format("[ERROR] write data to file \"%s\" failed", ProgramOptions.getInstance().constPrint));
            return 1;
        }

        System.out.println(String.format(
            "[INFO] write const data to \"%s\" success.(charset: %s)",
            ProgramOptions.getInstance().constPrint,
            SchemeConf.getInstance().getKey().getEncoding()
        ));

        return 0;
    }

    private static int build_group(String[] args) {
        int ret = ProgramOptions.getInstance().init(args);
        if (ret != 0) {
            return 1;
        }

        SchemeConf.getInstance().reset();

        // 特殊流程，常量打印
        if (false == ProgramOptions.getInstance().constPrint.isEmpty()) {
            return print_const_data();
        }

        ret = SchemeConf.getInstance().initScheme();
        if (ret < 0) {
            return 1;
        }

        // 读入数据表 & 协议编译
        int success_count = 0;
        for (int i = 0; i < ProgramOptions.getInstance().dataSourceMetas.length; ++ i) {
            String sn = ProgramOptions.getInstance().dataSourceMetas[i];

            // 0. 清理
            SchemeConf.getInstance().getMacroSource().clear();
            SchemeConf.getInstance().getDataSource().clear();

            // 1. 描述信息
            if (false == SchemeConf.getInstance().getScheme().load_scheme(sn)) {
                System.err.println(String.format("[ERROR] convert scheme \"%s\" failed", sn));
                continue;
            }

            // 2. 数据工作簿
            Class ds_clazz = DataSrcExcel.class;
            DataSrcImpl ds = DataSrcImpl.create(ds_clazz);
            if (null == ds) {
                System.err.println(String.format("[ERROR] create data source class \"%s\" failed", ds_clazz.getName()));
                continue;
            }
            ret = ds.init();
            if (ret < 0) {
                System.err.println(String.format("[ERROR] init data source class \"%s\" failed", ds_clazz.getName()));
                continue;
            }

            // 3. 协议描述文件
            DataDstImpl protoDesc = null;
            switch (ProgramOptions.getInstance().protocol) {
                case PROTOBUF:
                    protoDesc = new DataDstPb();
                    break;
                default:
                    System.err.println(String.format("[ERROR] protocol type \"%s\" invalid", ProgramOptions.getInstance().protocol.toString()));
                    break;
            }

            if (null == protoDesc)
                continue;
            if (false == protoDesc.init()) {
                System.err.println(String.format("[ERROR] protocol desc \"%s\" init failed", ProgramOptions.getInstance().protocol.toString()));
                continue;
            }

            DataDstWriterNode dataDesc = protoDesc.compile();
            if (null == dataDesc) {
                System.err.println(String.format("[ERROR] compile protocol desc \"%s\" failed", ProgramOptions.getInstance().protocol.toString()));
                continue;
            }

            // 4. 输出类型
            DataDstImpl outDesc = get_out_desc(protoDesc);
            if (null == outDesc)
                continue;

            System.out.println(String.format(
                "[INFO] convert scheme \"%s\" to \"%s\" started ...",
                sn,
                SchemeConf.getInstance().getOutputFile(),
                SchemeConf.getInstance().getKey().getEncoding()
            ));

            try {
                String filePath = SchemeConf.getInstance().getOutputFile();
                if(!IdentifyEngine.isAbsPath(filePath))
                    filePath = ProgramOptions.getInstance().outputDirectory + '/' + filePath;
                OutputStream fos = new FileOutputStream(filePath, false);
                byte[] data = outDesc.build(dataDesc);

                if (null != data) {
                    fos.write(data);
                }
            } catch (java.io.IOException e) {
                System.err.println(String.format("[ERROR] write data to file \"%s\" failed", SchemeConf.getInstance().getOutputFile()));
                continue;
            }

            System.out.println(String.format(
                "[INFO] convert scheme \"%s\" to \"%s\" success.(charset: %s)",
                sn,
                SchemeConf.getInstance().getOutputFile(),
                SchemeConf.getInstance().getKey().getEncoding()
            ));
            ++ success_count;
        }

        return ProgramOptions.getInstance().dataSourceMetas.length - success_count;
    }


    // 因为所有的参数除了特定名字外都是文件或目录的路径，而跨平台的路径是不能包含双引号的，所以为了简单起见，就不需要“的转义功能了
    private static Pattern pick_args_rule = Pattern.compile("('[^']*')|(\"[^\"]*\")|(\\S+)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static String[] pick_stdin_args(Scanner jin) {
        String[] ret = null;

        if (jin.hasNextLine()) {
            Matcher rem = pick_args_rule.matcher(jin.nextLine());

            LinkedList<String> res = new LinkedList<String>();
            while(rem.find()) {
                String item = rem.group();
                if (item.charAt(0) == '"' && item.charAt(item.length() - 1) == '"') {
                    item = item.substring(1, item.length() - 1);
                    // 如果需要转义功能的话
                } else if (item.charAt(0) == '\'' && item.charAt(item.length() - 1) == '\'') {
                    item = item.substring(1, item.length() - 1);
                }

                res.add(item);
            }


            ret = new String[res.size()];
            for(int i = 0; !res.isEmpty(); ++ i, res.removeFirst()) {
                ret[i] = res.getFirst();
            }
        }

        return ret;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // 先尝试根据传入参数转表
        int ret_code = build_group(args);

        // 再尝试使用标准输入来批量转表
        if (ProgramOptions.getInstance().enableStdin) {
            String[] stdin_args = null;
            Scanner jin = new Scanner(System.in);
            while (null != (stdin_args = pick_stdin_args(jin))) {
                if (stdin_args.length > 0) {
                    ret_code += build_group(stdin_args);
                }
            }
        }

        // 退出码为失败的任务个数，用以外部捕获转换失败
        System.exit(ret_code);
    }

}
