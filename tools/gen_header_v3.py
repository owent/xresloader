#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import re
import string
import glob
import sys
from subprocess import Popen

work_dir = os.getcwd()
script_dir = os.path.dirname(os.path.realpath(__file__))
sys.path.append(script_dir)
os.chdir(script_dir)
os.chdir(os.path.join('..'))

project_dir = os.getcwd()
proto_dir = os.path.join(project_dir, 'header')
proto_file = os.path.join(proto_dir, 'pb_header_v3.proto')
extension_proto_file = glob.glob(os.path.join(proto_dir, 'extensions', 'v3', '*.proto'))

os.chdir(work_dir)

java_out_dir = proto_dir
pb_out_file = os.path.join(proto_dir, 'pb_header_v3.pb')

from find_protoc import find_protoc

common_args = [
    "-I", os.path.join(proto_dir, 'extensions', 'v3'),
    "-I", os.path.join(proto_dir, 'extensions'),
    "-I", os.path.join(proto_dir)
]

# java 文件为非LITE版本
print('[PROCESS] generate java source ... ')
Popen(
    [
        find_protoc(), *common_args, 
        '--java_out', java_out_dir,
        proto_file, *extension_proto_file
    ],
    cwd=os.path.join(proto_dir, 'extensions'),
    shell=False).wait()
print('[PROCESS] generate java source done.')

# pb 文件为LITE版本
print('[PROCESS] generate proto pb file ... ')
Popen(
    [
        find_protoc(), *common_args, 
        '-o', pb_out_file,
        proto_file
    ],
    shell=False).wait()
print('[PROCESS] generate proto pb file done.')
