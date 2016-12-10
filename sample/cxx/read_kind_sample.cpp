#include <cstdio>
#include <iostream>
#include <fstream>

#include "kind.pb.h"
#include "libresloader.h"

int main(int argc, char* argv[]) {

    const char* file_path = "../role_cfg.bin";
    if (argc > 1) {
        file_path = argv[1];
    } else {
        printf("usage: %s <path to role_cfg.bin>\n");
        return 1;
    }

    // key - value
    do {
        typedef xresloader::conf_manager_kv<role_cfg, uint32_t> kind_upg_cfg_t;
        kind_upg_cfg_t upg_mgr;
        upg_mgr.set_key_handle([](kind_upg_cfg_t::value_type p) {
            return std::make_tuple<uint32_t>(p->id());
        });

        upg_mgr.load_file(file_path);

        kind_upg_cfg_t::value_type data1 = upg_mgr.get(10002);
        if (NULL == data1) {
            std::cerr<< "role id: 10002 not found, load file "<< file_path<< " failed."<< std::endl;
            break;
        }

        if (data1->has_dep_test()) {
            printf("role id: %u, unlock level: %u, name %s, dep_test.name: %s\n", data1->id(), data1->unlock_level(), data1->name().c_str(), data1->dep_test().name().c_str());
        } else {
            printf("role id: %u, unlock level: %u, name %s, dep_test.name: NONE\n", data1->id(), data1->unlock_level(), data1->name().c_str());
        }
        printf("%s\n", data1->DebugString().c_str());
    } while(false);

    // key - list
    do {
        typedef xresloader::conf_manager_kl<role_cfg, uint32_t> kind_upg_cfg_t;
        kind_upg_cfg_t upg_mgr;
        upg_mgr.set_key_handle([](kind_upg_cfg_t::value_type p) {
            return std::make_tuple<uint32_t>(p->id());
        });

        upg_mgr.load_file(file_path);

        kind_upg_cfg_t::value_type data1 = upg_mgr.get(10003, 0);
        if (NULL == data1) {
            std::cerr<< "role id: 10003 not found, load file "<< file_path<< " failed."<< std::endl;
            break;
        }
        
        if (data1->has_dep_test()) {
            printf("role id: %u, unlock level: %u, name %s, dep_test.name: %s\n", data1->id(), data1->unlock_level(), data1->name().c_str(), data1->dep_test().name().c_str());
        } else {
            printf("role id: %u, unlock level: %u, name %s, dep_test.name: NONE\n", data1->id(), data1->unlock_level(), data1->name().c_str());
        }

        printf("%s\n", data1->DebugString().c_str());
    } while(false);

    return 0;
}