#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import re
import string
import glob
import sys
import sysconfig

if sys.version_info[0] == 2:
    def CmdArgsGetParser(usage):
        reload(sys)
        sys.setdefaultencoding('utf-8')
        from optparse import OptionParser
        return OptionParser('usage: %prog ' + usage)
    
    def CmdArgsAddOption(parser, *args, **kwargs):
        parser.add_option(*args, **kwargs)

    def CmdArgsParse(parser):
        return parser.parse_args()

else:
    def CmdArgsGetParser(usage):
        import argparse
        ret = argparse.ArgumentParser(usage="%(prog)s " + usage)
        ret.add_argument('REMAINDER', nargs=argparse.REMAINDER, help='task names')
        return ret

    def CmdArgsAddOption(parser, *args, **kwargs):
        parser.add_argument(*args, **kwargs)

    def CmdArgsParse(parser):
        ret = parser.parse_args()
        return (ret, ret.REMAINDER)

def add_package_prefix_paths(packag_paths):
    """See https://docs.python.org/3/install/#how-installation-works"""
    append_paths = []
    for path in packag_paths:
        add_package_bin_path = os.path.join(path, "bin")
        if os.path.exists(add_package_bin_path):
            if sys.platform.lower() == "win32":
                os.environ[
                    "PATH"] = add_package_bin_path + ";" + os.environ["PATH"]
            else:
                os.environ[
                    "PATH"] = add_package_bin_path + ":" + os.environ["PATH"]

        add_package_lib_path = os.path.join(
            path,
            "lib",
            "python{0}".format(sysconfig.get_python_version()),
            "site-packages",
        )
        if os.path.exists(add_package_lib_path):
            append_paths.append(add_package_lib_path)

        add_package_lib64_path = os.path.join(
            path,
            "lib64",
            "python{0}".format(sysconfig.get_python_version()),
            "site-packages",
        )
        if os.path.exists(add_package_lib64_path):
            append_paths.append(add_package_lib64_path)

        add_package_lib_path_for_win = os.path.join(path, "Lib",
                                                    "site-packages")
        if os.path.exists(add_package_lib_path_for_win):
            append_paths.append(add_package_lib_path_for_win)
    append_paths.extend(sys.path)
    sys.path = append_paths

def main():
    from google.protobuf import descriptor_pb2 as pb2
    from google.protobuf import message_factory as _message_factory
    from google.protobuf.text_format import MessageToString 

    script_dir = os.path.dirname(os.path.realpath(__file__))
    default_header_pb_file = os.path.realpath(os.path.join(script_dir, '..', 'header', 'pb_header_v3.pb'))

    usage = '[options...] <pb file> <binary file>'
    parser = CmdArgsGetParser(usage)
    CmdArgsAddOption(parser,
        "-v",
        "--version",
        action="store_true",
        help="show version and exit",
        dest="version",
        default=False)
    CmdArgsAddOption(parser,
        "--as_one_line",
        action="store_true",
        help="set one line per data_block for output",
        dest="as_one_line",
        default=False)
    CmdArgsAddOption(parser,
        "--header",
        action="store",
        help="set xresloader header pb file(default: {0})".format(os.path.relpath(default_header_pb_file, os.getcwd())),
        dest="header_pb_file",
        default=default_header_pb_file)
    CmdArgsAddOption(parser,
        "--add_path",
        action="append",
        help="Append python search PATH(It can be used to set pure python protobuf implementation)",
        dest="add_path",
        default=[])
    CmdArgsAddOption(parser,
        "--add_package_prefix",
        action="append",
        help="Append python search PATH(It can be used to set pure python protobuf implementation)",
        dest="add_package_prefix",
        default=[])

    (options, left_args) = CmdArgsParse(parser)
    if options.version:
        print('1.0.0')
        return 0

    def print_help_msg(err_code):
        parser.print_help()
        exit(err_code)

    if len(left_args) < 2:
        print_help_msg(1)


    header_pb_fds = pb2.FileDescriptorSet.FromString(open(options.header_pb_file, 'rb').read())
    real_pb_fds = pb2.FileDescriptorSet.FromString(open(left_args[0], 'rb').read())
    protobuf_fds = pb2.FileDescriptorSet.FromString(open(os.path.join(script_dir, 'extensions.pb'), 'rb').read())

    header_message_desc = _message_factory.GetMessages([x for x in header_pb_fds.file])
    pb_fds_header_clazz = header_message_desc["org.xresloader.pb.xresloader_datablocks"]

    header_inst = pb_fds_header_clazz.FromString(open(left_args[1], 'rb').read())

    if options.add_path:
        prepend_paths = [x for x in options.add_path]
        prepend_paths.extend(sys.path)
        sys.path = prepend_paths
    add_package_prefix_paths(options.add_package_prefix)

    print('==================================================================')
    print(MessageToString(header_inst.header, as_utf8=True, as_one_line=options.as_one_line, use_short_repeated_primitives=True))

    real_file_descs = [x for x in real_pb_fds.file]
    real_file_descs.extend([x for x in header_pb_fds.file])
    real_file_descs.extend([x for x in protobuf_fds.file])
    real_message_desc = _message_factory.GetMessages(real_file_descs)
    if header_inst.data_message_type not in real_message_desc:
        print('------------------------------------------------------------------')
        print('data_message_type {0} not found in {1}'.format(header_inst.data_message_type, open(left_args[0])))
        exit(0)

    real_inst = real_message_desc[header_inst.data_message_type]
    line_index = 0

    if options.as_one_line:
        print('------------------------------------------------------------------')

    for data_block in header_inst.data_block:
        message_inst = real_inst.FromString(data_block)
        line_index = line_index + 1
        if not options.as_one_line:
            print('# {0:<5} ----------------------------------------------------------'.format(line_index))
        decode_as_utf8 = False
        try:
            # see https://googleapis.dev/python/protobuf/latest/google/protobuf/text_format.html for detail
            if options.as_one_line:
                print('$ {0:<5}: {1}'.format(line_index, MessageToString(message_inst, as_utf8=True, as_one_line=options.as_one_line, use_short_repeated_primitives=True, print_unknown_fields=True)))
            else:
                print(MessageToString(message_inst, as_utf8=True, as_one_line=options.as_one_line, use_short_repeated_primitives=True, print_unknown_fields=True))
            decode_as_utf8 = True
        except:
            pass

        if decode_as_utf8:
            continue

        try:
            if options.as_one_line:
                print('$ {0:<5}: {1}'.format(line_index, MessageToString(message_inst, as_utf8=True, as_one_line=options.as_one_line, use_short_repeated_primitives=True, print_unknown_fields=True)))
            else:
                print(MessageToString(message_inst, as_utf8=True, as_one_line=options.as_one_line, use_short_repeated_primitives=True, print_unknown_fields=True))
        except:
            pass

if __name__ == '__main__':
    exit(main())
