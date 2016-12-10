#!/usr/bin/python
# -*- coding: utf-8 -*-

import os, sys
import string
import glob


work_dir = os.getcwd();
script_dir = os.path.dirname(os.path.realpath(__file__))
os.chdir(script_dir)
os.chdir(os.path.join('..'));

project_dir = os.getcwd();
proto_dir = os.path.join(script_dir, 'proto_v2');

proto_file = []
for item in glob.glob(os.path.join(proto_dir, '*.proto')):
    proto_file.append('"' + item + '"');

os.chdir(work_dir);
os.system('/usr/bin/python "{0}"'.format(os.path.join(project_dir, 'loader-binding', 'cxx', 'gen_protocol.py')))
cpp_out_dir = os.path.join(script_dir, 'cxx');

proto_src_dir = '{0}/v2'.format(cpp_out_dir)
if not os.path.exists(proto_src_dir):
    os.mkdir(proto_src_dir)
params = ['protoc', '-I', proto_dir, '-o', os.path.join(proto_dir, 'kind.pb'), '--cpp_out={0}'.format(proto_src_dir)]
params.extend(proto_file)
cmd = ' '.join(params)
print(cmd)
os.system(cmd)

