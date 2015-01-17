#pragma once

#include <cstddef>
#include <iostream>
#include <fstream>
#include <tuple>
#include <memory>
#include <string>

#include "pb_header.pb.h"

namespace xresloader {

    template<typename TItem, typename... TKey>
    class conf_manager_kv {
    public:
        typedef std::tuple<TKey...> key_type;
        typedef std::shared_ptr<TItem> value_type;
        typedef com::owent::xresloader::pb::xresloader_datablocks proto_type;

    public:
        inline const proto_type& get_root() const { return root_; }

        void clear() {
            data_.clear();
        }

        bool load(const void* buff, size_t len) {
            root_.clear_data_block();
            root_.clear_header();

            return root_;
        }

        bool load_file(const std::string& file_path) {
            std::fstream fin;
            fin.open(file_path.c_str(), std::ios::in | std::ios::binary);
            if (false == fin.is_open()) {
                return false;
            }

            root_.clear_data_block();
            root_.clear_header();
            if (false == root_.ParseFromIstream(fin)) {
                return false;
            }

            return build();
        }

    private:
        bool build();

    private:
        proto_type root_;
        std::map<key_type, value_type> data_;
    };
}
