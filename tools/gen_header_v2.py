#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import re
import string
import glob
import sys
from subprocess import Popen

work_dir = os.getcwd()
script_dir = os.path.dirname(os.path.realpath(__file__))
os.chdir(script_dir)
sys.path.append(script_dir)
os.chdir(os.path.join('..'))

project_dir = os.getcwd()
proto_dir = os.path.join(project_dir, 'third_party', 'xresloader-protocol',
                         'core')
proto_file = os.path.join(proto_dir, 'pb_header.proto')
extension_proto_file = glob.glob(
    os.path.join(proto_dir, 'extensions', 'v2', '*.proto'))

os.chdir(work_dir)

java_out_dir = os.path.join(project_dir, 'header')
pb_out_file = os.path.join(project_dir, 'header', 'pb_header.pb')

from find_protoc import find_protoc

common_args = [
    "-I",
    os.path.join(proto_dir, 'extensions', 'v2'), "-I",
    os.path.join(proto_dir, '..', 'common'), "-I",
    os.path.join(proto_dir)
]

# java 文件为非LITE版本
proto_file_data = open(proto_file).read()
proto_file_no_lite_data = re.sub(
    '\s*option\s*optimize_for\s*=\s*LITE_RUNTIME\s*;',
    '// option optimize_for = LITE_RUNTIME;', proto_file_data)
open(proto_file, 'wb').write(proto_file_no_lite_data.encode('utf-8'))

print('[PROCESS] generate java source ... ')
exec_args = [find_protoc(), java_out_dir]
exec_args.extend(common_args)
exec_args.extend([proto_file])
exec_args.extend(extension_proto_file)
Popen(exec_args, shell=False).wait()
print('[PROCESS] generate java source done.')

# pb 文件为LITE版本
open(proto_file, 'wb').write(proto_file_data.encode('utf-8'))

print('[PROCESS] generate proto pb file ... ')
exec_args = [find_protoc(), '-o', pb_out_file]
exec_args.extend(common_args)
exec_args.extend([proto_file])
Popen(exec_args, shell=False).wait()
print('[PROCESS] generate proto pb file done.')
