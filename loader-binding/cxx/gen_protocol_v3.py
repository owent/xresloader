#!/usr/bin/env python3
# -*- coding: utf-8 -*-  

import os
import string
import sys
from subprocess import Popen

work_dir = os.getcwd()
script_dir = os.path.dirname(os.path.realpath(__file__))
os.chdir(script_dir)
os.chdir(os.path.join('..', '..'))

project_dir = os.getcwd()

sys.path.append(os.path.join(project_dir, 'tools'))

from find_protoc import find_protoc

proto_dir = os.path.join(project_dir, 'header')
proto_file = os.path.join(proto_dir, 'pb_header_v3.proto')

os.chdir(work_dir)

Popen(
    [
        find_protoc(),  
        '-I', proto_dir, 
        '--cpp_out', script_dir,
        proto_file
    ],
    shell=False).wait()

print('Generate header code from {0} to {1} done.'.format(proto_file, script_dir))