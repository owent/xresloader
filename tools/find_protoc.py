#!/usr/bin/python
# -*- coding: utf-8 -*-

import sys
import os
import stat

protoc_exec = None
def find_protoc():
    global protoc_exec
    if protoc_exec is not None:
        return protoc_exec
    script_dir = os.path.dirname(os.path.realpath(__file__))
    if sys.platform[0:5].lower() == "linux":
        protoc_exec = os.path.join(script_dir, 'linux_x86_64', 'protoc')
    elif sys.platform[0:6].lower() == "darwin":
        protoc_exec = os.path.join(script_dir, 'macos_x86_64', 'protoc')
    else:
        protoc_exec = os.path.join(script_dir, 'windows_x86_64', 'protoc.exe')
    os.chmod(protoc_exec, stat.S_IRWXU + stat.S_IRWXG + stat.S_IRWXO)
    return protoc_exec

""" run as a executable """
if __name__ == "__main__":
    print(find_protoc())
