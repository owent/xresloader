#include <cstdio>
#include <iostream>
#include <fstream>

#include "kind.pb.h"
#include "pb_header.pb.h"


using com::owent::xresloader::pb::xresloader_header;
using com::owent::xresloader::pb::xresloader_datablocks;

int main(int argc, char* argv[]) {
    std::fstream f;
    f.open(argv[1], std::ios::in | std::ios::binary);
    if (!f.is_open()) {
        std::cerr << "open file" << argv[1] << "] failed" << std::endl;
        return 0;
    }

    xresloader_datablocks blocks;
    if (false == blocks.ParseFromIstream(&f)) {
        std::cerr << "Parse data blocks failed: " << blocks.InitializationErrorString() << std::endl;
        return 0;
    }
    const xresloader_header& header = blocks.header();
    std::cout << header.DebugString() << std::endl;

    int count = blocks.data_block_size();
    role_cfg* roles = new role_cfg [count];
    if (header.count() != count) {
        std::cerr << "Parse data blocks count not match.(expect: " << header.count()<< ", real: "<< count<< ")"<< std::endl;
    }

    for (int i = 0; i < count; ++i) {
        if (false == roles[i].ParseFromString(blocks.data_block(i))) {
            std::cerr << "Parse role_cfg["<< i<< "] failed: "<< roles[i].InitializationErrorString() << std::endl;
            break;
        }
        
        std::cout << roles[i].DebugString() << std::endl;
    }


    delete [] roles;
    return 0;
}