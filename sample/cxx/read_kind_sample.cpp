#include <cstdio>
#include <iostream>
#include <fstream>

#include "kind.pb.h"
#include "libresloader.h"

int main(int argc, char* argv[]) {

    // key - value
    {
        typedef xresloader::conf_manager_kv<role_cfg, uint32_t> kind_upg_cfg_t;
        kind_upg_cfg_t upg_mgr;
        upg_mgr.set_key_handle([](kind_upg_cfg_t::value_type p) {
            return std::make_tuple<uint32_t>(p->id());
        });

        upg_mgr.load_file("../role_cfg.bin");

        kind_upg_cfg_t::value_type data1 = upg_mgr.get(10002);
        printf("role id: %u, unlock level: %u, name %s, dep_test.name: %s\n", data1->id(), data1->unlock_level(), data1->name().c_str(), data1->dep_test().name().c_str());
    }


    // key - list
    {
        typedef xresloader::conf_manager_kl<role_cfg, uint32_t> kind_upg_cfg_t;
        kind_upg_cfg_t upg_mgr;
        upg_mgr.set_key_handle([](kind_upg_cfg_t::value_type p) {
            return std::make_tuple<uint32_t>(p->id());
        });

        upg_mgr.load_file("../role_cfg.bin");

        kind_upg_cfg_t::value_type data1 = upg_mgr.get(10003, 0);
        printf("role id: %u, unlock level: %u, name %s, dep_test.name: %s\n", data1->id(), data1->unlock_level(), data1->name().c_str(), data1->dep_test().name().c_str());
    }

    return 0;
}