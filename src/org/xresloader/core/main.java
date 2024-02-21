/**
 *
 */
package org.xresloader.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.xresloader.core.data.dst.DataDstImpl;
import org.xresloader.core.data.dst.DataDstJavascript;
import org.xresloader.core.data.dst.DataDstJson;
import org.xresloader.core.data.dst.DataDstLua;
import org.xresloader.core.data.dst.DataDstMsgPack;
import org.xresloader.core.data.dst.DataDstPb;
import org.xresloader.core.data.dst.DataDstUECsv;
import org.xresloader.core.data.dst.DataDstUEJson;
import org.xresloader.core.data.dst.DataDstXml;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.err.InitializeException;
import org.xresloader.core.data.src.DataSrcExcel;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.scheme.SchemeConf;

/**
 * @author owentou
 */
public class main {

    static private String endl = "\n";

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
            case JAVASCRIPT:
                outDesc = new DataDstJavascript();
                outDesc = outDesc.init() ? outDesc : null;
                break;
            case UECSV:
                outDesc = new DataDstUECsv();
                outDesc = outDesc.init() ? outDesc : null;
                break;
            case UEJSON:
                outDesc = new DataDstUEJson();
                outDesc = outDesc.init() ? outDesc : null;
                break;
            default:
                ProgramOptions.getLoger().error("Output type \"%s\" invalid",
                        ProgramOptions.getInstance().outType.toString());
                break;
        }

        return outDesc;
    }

    private static int print_proto_data() {
        // 1. 协议描述文件
        DataDstImpl protoDesc = null;
        switch (ProgramOptions.getInstance().protocol) {
            case PROTOBUF:
                protoDesc = new DataDstPb();
                break;
            default:
                ProgramOptions.getLoger().error("Protocol type \"%s\" invalid",
                        ProgramOptions.getInstance().protocol.toString());
                break;
        }

        if (null == protoDesc)
            return 1;

        DataDstImpl outDesc = get_out_desc(protoDesc);
        if (null == outDesc)
            return 1;

        HashMap<String, Object> dump_data = null;
        String dump_name = null;

        switch (ProgramOptions.getInstance().protoDumpType) {
            case CONST:
                dump_data = protoDesc.buildConst();
                dump_name = "const";
                break;
            case DESCRIPTOR:
            case OPTIONS:
                dump_data = protoDesc.buildOptions(ProgramOptions.getInstance().protoDumpType);
                dump_name = "option";
                break;
            default:
                break;
        }

        if (null == dump_data) {
            ProgramOptions.getLoger().error("Protocol description \"%s\" initialize and build %s values failed",
                    ProgramOptions.getInstance().protocol.toString(), dump_name);
            return 1;
        }

        try {
            String filePath = SchemeConf.getInstance().getOutputFileAbsPath();
            File fout = new File(filePath);
            File dout = fout.getParentFile();
            if (!dout.exists()) {
                dout.mkdirs();
            }

            OutputStream fos = new FileOutputStream(filePath, false);
            byte[] data = outDesc.dumpConst(dump_data);

            if (null != data) {
                fos.write(data);
            } else {
                ProgramOptions.getLoger().error("Write %s data to file \"%s\" failed, output type invalid.", dump_name,
                        ProgramOptions.getInstance().protoDumpFile);

                fos.close();
                return 1;
            }
            fos.close();
        } catch (java.io.IOException e) {
            ProgramOptions.getLoger().error("Write data to file \"%s\" failed%s  > %s",
                    ProgramOptions.getInstance().protoDumpFile, endl, e.getMessage());
            return 1;
        } catch (ConvException e) {
            ProgramOptions.getLoger().error("Write data to file \"%s\" failed%s  > %s",
                    ProgramOptions.getInstance().protoDumpFile, endl, e.getMessage());
            return 1;
        }

        ProgramOptions.getLoger().info("Write %s data to \"%s\" success.(charset: %s)", dump_name,
                ProgramOptions.getInstance().protoDumpFile, SchemeConf.getInstance().getKey().getEncoding());

        return 0;
    }

    private static int build_group(String[] args) throws InitializeException {
        int ret = ProgramOptions.getInstance().init(args);
        if (ret < 0) {
            return 1;
        } else if (ret > 0) {
            return 0;
        }

        SchemeConf.getInstance().reset();

        // 特殊流程，常量打印
        if (false == ProgramOptions.getInstance().protoDumpFile.isEmpty()) {
            return print_proto_data();
        }

        ret = SchemeConf.getInstance().initScheme();
        if (ret < 0) {
            return 1;
        }

        if (ProgramOptions.getInstance().dataSourceMetas == null) {
            return 1;
        }

        // 读入数据表 & 协议编译
        int failed_count = 0;
        boolean continue_next_scheme = true;
        for (int i = 0; continue_next_scheme && i < ProgramOptions.getInstance().dataSourceMetas.length; ++i) {
            String sn = ProgramOptions.getInstance().dataSourceMetas[i];

            // 0. 清理
            SchemeConf.getInstance().getMacroSource().clear();
            SchemeConf.getInstance().getDataSource().clear();

            // 1. 描述信息
            if (false == SchemeConf.getInstance().getScheme().load_scheme(sn)) {
                sn = String.join(" ", ProgramOptions.getInstance().dataSourceMetas);
                ProgramOptions.getLoger().error("Convert from \"%s\" failed", sn);
                ++failed_count;
                continue;
            }

            // 命令行模式下dataSourceType为默认值，也就是BIN
            continue_next_scheme = SchemeConf.getInstance().getScheme().isSupportMultipleScheme();

            // 重新组织sn
            StringBuilder descBuilder = new StringBuilder();
            for (SchemeConf.DataInfo di : SchemeConf.getInstance().getDataSource()) {
                if (descBuilder.length() > 0) {
                    descBuilder.append(',');
                }

                if (!di.file_path.isEmpty()) {
                    descBuilder.append(di.file_path);
                    descBuilder.append('|');
                }

                descBuilder.append(di.table_name);
            }
            sn = descBuilder.toString();

            // 2. 数据工作簿
            Class<?> ds_clazz = DataSrcExcel.class;
            DataSrcImpl ds = DataSrcImpl.create(ds_clazz);
            if (null == ds) {
                ProgramOptions.getLoger().error("Create data source class \"%s\" failed", ds_clazz.getName());
                ++failed_count;
                continue;
            }

            ret = ds.init();
            if (ret < 0) {
                ProgramOptions.getLoger().error("Initialize data source class \"%s\" failed", ds_clazz.getName());
                ++failed_count;
                continue;
            }

            // 3. 协议描述文件
            DataDstImpl protoDesc = null;
            switch (ProgramOptions.getInstance().protocol) {
                case PROTOBUF:
                    protoDesc = new DataDstPb();
                    break;
                default:
                    ProgramOptions.getLoger().error("Protocol type \"%s\" invalid",
                            ProgramOptions.getInstance().protocol.toString());
                    ++failed_count;
                    break;
            }

            if (null == protoDesc) {
                continue;
            }

            ProgramOptions.getLoger().trace("Convert from \"%s\" to \"%s\" started (protocol=%s) ...", sn,
                    SchemeConf.getInstance().getOutputFile(), SchemeConf.getInstance().getProtoName());

            if (false == protoDesc.init()) {
                ProgramOptions.getLoger().error("Protocol description \"%s\" initialize failed: %s ",
                        ProgramOptions.getInstance().protocol.toString(), protoDesc.getLastErrorMessage());
                ++failed_count;
                continue;
            }

            // 4. 输出类型
            DataDstImpl outDesc = get_out_desc(protoDesc);
            if (null == outDesc) {
                continue;
            }

            try {
                String filePath = SchemeConf.getInstance().getOutputFileAbsPath();
                File fout = new File(filePath);
                File dout = fout.getParentFile();
                if (!dout.exists()) {
                    dout.mkdirs();
                }

                OutputStream fos = new FileOutputStream(filePath, false);
                byte[] data = outDesc.build(protoDesc);

                if (null != data) {
                    fos.write(data);
                }

                fos.close();
            } catch (ConvException e) {
                String fileName = ds.getCurrentFileName();
                String tableName = ds.getCurrentTableName();

                if (!fileName.isEmpty() && !tableName.isEmpty() && ds.hasCurrentRow()) {
                    ProgramOptions.getLoger().error(
                            "Convert data failed.%s  > %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > %s",
                            endl,
                            String.join(" ", args), endl, fileName, tableName,
                            ds.getCurrentRowNum() + 1, ds.getLastColomnNum() + 1,
                            ExcelEngine.getColumnName(ds.getLastColomnNum() + 1), endl, e.getMessage());
                } else if (!fileName.isEmpty() && !tableName.isEmpty()) {
                    ProgramOptions.getLoger().error(
                            "Convert data failed.%s  > %s%s  > File: %s, Table: %s%s  > %s", endl,
                            String.join(" ", args), endl, fileName, tableName,
                            endl, e.getMessage());
                } else if (!fileName.isEmpty()) {
                    ProgramOptions.getLoger().error(
                            "Convert data failed.%s  > %s%s  > File: %s%s  > %s", endl,
                            String.join(" ", args), endl, fileName,
                            endl, e.getMessage());
                } else {
                    ProgramOptions.getLoger().error(
                            "Convert data failed.%s  > %s%s  > %s", endl,
                            String.join(" ", args),
                            endl, e.getMessage());
                }

                ++failed_count;
                continue;
            } catch (java.io.IOException e) {
                ProgramOptions.getLoger().error("Write data to file \"%s\" failed%s  > %s",
                        SchemeConf.getInstance().getOutputFile(), endl, e.getMessage());
                ++failed_count;
                continue;
            }

            ProgramOptions.getLoger().info("Convert from \"%s\" to \"%s\" success.(charset: %s, %d excel row(s))", sn,
                    SchemeConf.getInstance().getOutputFile(), SchemeConf.getInstance().getKey().getEncoding(),
                    ds.getRecordNumber());
        }

        return failed_count;
    }

    // 因为所有的参数除了特定名字外都是文件或目录的路径，而跨平台的路径是不能包含双引号的，所以为了简单起见，就不需要“的转义功能了
    private static Pattern pick_args_rule = Pattern.compile("('[^']*')|(\"[^\"]*\")|(\\S+)",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static String[] pick_stdin_args(Scanner jin) {
        String[] ret = null;

        if (jin.hasNextLine()) {
            Matcher rem = pick_args_rule.matcher(jin.nextLine());

            LinkedList<String> res = new LinkedList<String>();
            while (rem.find()) {
                String item = rem.group();
                if (item.charAt(0) == '"' && item.charAt(item.length() - 1) == '"') {
                    if (item.length() > 2) {
                        item = item.substring(1, item.length() - 1);
                    } else {
                        item = "";
                    }
                    // 如果需要转义功能的话
                } else if (item.charAt(0) == '\'' && item.charAt(item.length() - 1) == '\'') {
                    if (item.length() > 2) {
                        item = item.substring(1, item.length() - 1);
                    } else {
                        item = "";
                    }
                }

                if (item.length() > 0) {
                    res.add(item);
                }
            }

            ret = new String[res.size()];
            for (int i = 0; !res.isEmpty(); ++i, res.removeFirst()) {
                ret[i] = res.getFirst();
            }
        }

        return ret;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        endl = ProgramOptions.getEndl();

        // 全局设置
        ExcelEngine.setMaxByteArraySize(Integer.MAX_VALUE);
        ZipSecureFile.setMinInflateRatio(0);

        // 先尝试根据传入参数转表
        int ret_code = 1;
        try {
            ret_code = build_group(args);
        } catch (InitializeException e) {
            ProgramOptions.getLoger().error("Initlize failed.%s%s> %s", e.getMessage(), endl, String.join(" ", args));
        } catch (Exception e) {
            ProgramOptions.getLoger().error("%s", e.getMessage());
            for (StackTraceElement frame : e.getStackTrace()) {
                ProgramOptions.getLoger().error("\t%s", frame.toString());
            }

            ProgramOptions.getLoger().error("Panic!, it's probably a BUG, please report to %s, current version: %s",
                    ProgramOptions.getReportUrl(), ProgramOptions.getInstance().getVersion());
        }

        // 再尝试使用标准输入来批量转表
        if (ProgramOptions.getInstance().enableStdin) {
            String[] stdin_args = null;
            Scanner jin = new Scanner(System.in);
            while (null != (stdin_args = pick_stdin_args(jin))) {
                if (stdin_args.length > 0) {
                    try {
                        ret_code += build_group(stdin_args);
                    } catch (InitializeException e) {
                        ++ret_code;
                        ProgramOptions.getLoger().error("Initlize failed.%s%s> %s", e.getMessage(), endl,
                                String.join(" ", stdin_args));
                    } catch (Exception e) {
                        ++ret_code;
                        ProgramOptions.getLoger().error("%s", e.getMessage());
                        for (StackTraceElement frame : e.getStackTrace()) {
                            ProgramOptions.getLoger().error("\t%s", frame.toString());
                        }

                        ProgramOptions.getLoger().error(
                                "Panic!, it's probably a BUG, please report to %s, current version: %s",
                                ProgramOptions.getReportUrl(), ProgramOptions.getInstance().getVersion());
                    }
                }
            }
        }

        // 退出码为失败的任务个数，用以外部捕获转换失败
        if (ret_code > 255) {
            ret_code = 255;
        }
        System.exit(ret_code);
    }

}
