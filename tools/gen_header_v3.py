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
proto_file = os.path.join(proto_dir, 'pb_header_v3.proto');

os.chdir(work_dir);

java_out_dir = proto_dir;
pb_out_file = os.path.join(proto_dir, 'pb_header_v3.pb');

# java 文件为非LITE版本
params = ['protoc', '-I', '"' + proto_dir + '"' , '--java_out',  '"' + java_out_dir + '"', '"' + proto_file + '"']
cmd = ' '.join(params)
print('[PROCESS] generate java source ... ')
print(cmd)
os.system(cmd)
print('[PROCESS] generate java source done.')

# pb 文件为LITE版本
params = ['protoc', '-I', '"' + proto_dir + '"' , '-o',  '"' + pb_out_file + '"', '"' + proto_file + '"']
cmd = ' '.join(params)
print('[PROCESS] generate proto pb file ... ')
print(cmd)
os.system(cmd)
print('[PROCESS] generate proto pb file done.')

