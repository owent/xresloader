#!/usr/bin/python
# -*- coding: utf-8 -*-  

import os
import string

work_dir = os.getcwd();
script_dir = os.path.dirname(os.path.realpath(__file__))
os.chdir(script_dir)
os.chdir(os.path.join('..', '..'));

project_dir = os.getcwd();
proto_dir = '"' + os.path.join(project_dir, 'header') + '"';

proto_file = '"' + os.path.join(project_dir, 'header', 'pb_header_v3.proto') + '"';

os.chdir(work_dir);

cmd = ' '.join(['protoc', '-I', proto_dir, '--cpp_out=lite:{0}'.format(script_dir), proto_file])
print(cmd)
os.system(cmd)
