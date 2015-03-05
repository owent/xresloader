#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import re
import string


work_dir = os.getcwd();
script_dir = os.path.dirname(os.path.realpath(__file__))
os.chdir(script_dir)
os.chdir(os.path.join('..'));

project_dir = os.getcwd();
proto_dir = os.path.join(project_dir, 'header');
proto_file = os.path.join(proto_dir, 'pb_header.proto');

os.chdir(work_dir);

java_out_dir = proto_dir;
pb_out_file = os.path.join(proto_dir, 'pb_header.pb');

# java 文件为非LITE版本
proto_file_data = open(proto_file).read()
proto_file_no_lite_data = re.sub('\s*option\s*optimize_for\s*=\s*LITE_RUNTIME\s*;','// option optimize_for = LITE_RUNTIME;', proto_file_data)
open(proto_file, 'wb').write(proto_file_no_lite_data)

params = ['protoc', '-I', '"' + proto_dir + '"' , '--java_out',  '"' + java_out_dir + '"', '"' + proto_file + '"']
cmd = string.join(params, ' ')
print('[PROCESS] generate java source ... ')
print(cmd)
os.system(cmd)
print('[PROCESS] generate java source done.')

# pb 文件为LITE版本
open(proto_file, 'wb').write(proto_file_data)

params = ['protoc', '-I', '"' + proto_dir + '"' , '-o',  '"' + pb_out_file + '"', '"' + proto_file + '"']
cmd = string.join(params, ' ')
print('[PROCESS] generate proto pb file ... ')
print(cmd)
os.system(cmd)
print('[PROCESS] generate proto pb file done.')

