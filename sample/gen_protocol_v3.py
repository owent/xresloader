#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import string
import glob
import sys
from subprocess import Popen

work_dir = os.getcwd()
script_dir = os.path.dirname(os.path.realpath(__file__))
os.chdir(script_dir)
os.chdir(os.path.join('..'))
project_dir = os.getcwd()
proto_dir = os.path.join(script_dir, 'proto_v3')

header_dir = os.path.realpath(os.path.join(project_dir, 'header'))
tools_dir = os.path.realpath(os.path.join(project_dir, 'tools'))

sys.path.append(tools_dir)
from find_protoc import find_protoc

common_args = [
    "-I", proto_dir,
    "-I", os.path.join(header_dir, 'extensions', 'v3'),
    "-I", os.path.join(header_dir, 'extensions')
]

proto_file = glob.glob(os.path.join(proto_dir, '*.proto'))
proto_file.append(os.path.join(header_dir, 'extensions', 'google', 'protobuf', 'descriptor.proto'))
proto_file.extend(glob.glob(os.path.join(header_dir, 'extensions', 'v3', '*.proto')))

os.chdir(work_dir)

Popen(
    [
        sys.executable,
        os.path.join(project_dir, 'loader-binding', 'cxx', 'gen_protocol_v3.py')
    ],
    stdin=None,
    stdout=None,
    stderr=None,
    shell=False).wait()
cpp_out_dir = os.path.join(script_dir, 'cxx')

proto_src_dir = os.path.join(cpp_out_dir, 'v3')
if not os.path.exists(proto_src_dir):
    os.mkdir(proto_src_dir)

exec_args = [find_protoc(), '-o', os.path.join(proto_dir, 'kind.pb'), '--cpp_out', proto_src_dir]
exec_args.extend(common_args)
exec_args.extend(proto_file)

Popen(exec_args,
    stdin=None,
    stdout=None,
    stderr=None,
    shell=False).wait()

print('Run "{0}" done'.format('" "'.join(exec_args)))
