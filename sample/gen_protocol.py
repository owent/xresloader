#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import string
import glob


work_dir = os.getcwd();
script_dir = os.path.dirname(os.path.realpath(__file__))
os.chdir(script_dir)
os.chdir(os.path.join('..'));

project_dir = os.getcwd();
proto_dir = '"' + script_dir + '"';

proto_file = []
for item in glob.glob(os.path.join(script_dir, '*.proto')):
    proto_file.append('"' + item + '"');

os.chdir(work_dir);

cpp_out_dir = '"' + os.path.join(script_dir, 'cxx') + '"';

params = ['protoc', '-I', proto_dir, '-o', os.path.join(proto_dir, 'kind.pb'), '--cpp_out', cpp_out_dir]
params.extend(proto_file)
cmd = string.join(params, ' ')
print(cmd)
os.system(cmd)

