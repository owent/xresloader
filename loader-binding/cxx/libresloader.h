#pragma once

#include <cstddef>
#include <iostream>
#include <fstream>
#include <tuple>
#include <vector>
#include <list>
#include <memory>
#include <functional>
#include <algorithm>
#include <string>
#include <map>

#include "pb_header.pb.h"

namespace xresloader {

    namespace details {
        template<typename TItem, typename... TKey>
        class conf_manager_base {
        public:
            typedef std::tuple<TKey...> key_type;
            typedef std::shared_ptr<TItem> value_type;
            typedef com::owent::xresloader::pb::xresloader_datablocks proto_type;

            typedef std::function<key_type(value_type)> func_type;
            typedef std::function<bool(const value_type&)> filter_func_type;

        public:
            inline const proto_type& get_root() const { return root_; }

            void set_key_handle(func_type fn) { func_ = fn; };

            bool load(const void* buff, size_t len) {
                bool res = root_.ParseFromArray(buff, static_cast<int>(len));
                if (false == res) {
                    std::cerr << "parse config data block failed." << std::endl <<
                        root_.InitializationErrorString() << std::endl;
                    return res;
                }

                return build();
            }

            bool load_file(const std::string& file_path) {
                std::fstream fin;
                fin.open(file_path.c_str(), std::ios::in | std::ios::binary);
                if (false == fin.is_open()) {
                    std::cerr << "open file " << file_path << "failed " << std::endl;
                    return false;
                }

                fin.seekg(0, std::ios::end);
                size_t len = static_cast<size_t>(fin.tellg());

                fin.seekg(0, std::ios::beg);

                char* buffer = new char[len];
                fin.read(buffer, len);

                bool ret = load(buffer, len);

                delete[] buffer;

                return ret;
            }

            const std::string& get_data_version() const {
                return data_version_;
            }

            const std::string& get_xresloader_version() const {
                return xresloader_version_;
            }

            void add_filter(filter_func_type fn) {
                if (fn) {
                    filter_list_.push_back(fn);
                }
            }

        protected:
            virtual ~conf_manager_base() {}

            virtual bool filter(const key_type& key, value_type val) = 0;

            virtual void on_loaded() {};

        private:
            bool build() {
                // TODO 检查校验码
                xresloader_version_ = root_.mutable_header()->xres_ver();
                data_version_ = root_.mutable_header()->data_ver();


                int len = root_.data_block_size();
                for (int i = 0; i < len; ++i) {
                    const std::string data_block = root_.data_block(i);
                    value_type p = value_type(new TItem());
                    if (false == p->ParseFromArray(data_block.data(), static_cast<int>(data_block.size()))) {
                        std::cerr << "parse config data block failed." << std::endl <<
                            p->InitializationErrorString() << std::endl;

                        continue;
                    }

                    bool is_pass = false;
                    for (filter_func_type& filter_fn : filter_list_) {
                        if (false == filter_fn(p)) {
                            is_pass = true;
                            break;
                        }
                    }

                    // 过滤器
                    if (is_pass) {
                        continue;
                    }

                    key_type key;
                    if (func_) {
                        key = func_(p);
                    }

                    filter(key, p);
                }

                on_loaded();
                root_.Clear();
                return true;
            }

        private:
            proto_type root_;
            func_type func_;
            std::string xresloader_version_;
            std::string data_version_;
            std::list<filter_func_type> filter_list_;
        };

    }

    template<typename TItem, typename... TKey>
    class conf_manager_kv : public details::conf_manager_base<TItem, TKey...> {
    public:
        typedef details::conf_manager_base<TItem, TKey...> base_type;
        typedef typename base_type::key_type key_type;
        typedef typename base_type::value_type value_type;
        typedef typename base_type::proto_type proto_type;

        typedef typename base_type::func_type func_type;
        typedef typename base_type::filter_func_type filter_func_type;

    protected:
        virtual bool filter(const key_type& key, value_type val) {
            if (data_.end() != data_.find(key)) {
                std::cerr << "[WARN] key appear more than once will be covered" << std::endl;
            }

            data_[key] = val;
            return true;
        }

    public:

        void clear() {
            data_.clear();
        }

        size_t size() const {
            return data_.size();
        }

        value_type get(key_type k) const {
            auto iter = data_.find(k);
            if (data_.end() == iter) {
                return value_type();
            }

            return iter->second;
        }

        value_type get(TKey... keys) const {
            return get(std::forward_as_tuple(keys...));
        }

        void foreach(std::function<void (const value_type&)> fn) const {
            for (auto iter = data_.begin(); iter != data_.end(); ++ iter) {
                fn(iter->second);
            }
        }

    private:
        std::map<key_type, value_type> data_;
    };

    template<typename TItem, typename... TKey>
    class conf_manager_kl : public details::conf_manager_base<TItem, TKey...> {
    public:
        typedef details::conf_manager_base<TItem, TKey...> base_type;
        typedef typename base_type::key_type key_type;
        typedef typename base_type::value_type value_type;
        typedef typename std::vector<value_type> list_type;
        typedef typename base_type::proto_type proto_type;

        typedef typename base_type::func_type func_type;
        typedef typename base_type::filter_func_type filter_func_type;
        typedef std::function<bool(const value_type&, const value_type&)> sort_func_type;

    protected:
        virtual bool filter(const key_type& key, value_type val) {
            data_[key].push_back(val);
            return true;
        }

        virtual void on_loaded() {
            if (sort_func_) {
                typename std::map<key_type, list_type>::iterator iter = data_.begin();
                for (; data_.end() != iter; ++iter) {
                    std::sort(iter->second.begin(), iter->second.end(), sort_func_);
                }
            }
        };

    public:

        void clear() {
            data_.clear();
        }

        size_t size() const {
            return data_.size();
        }

        const list_type* get_list(key_type k) const {
            auto iter = data_.find(k);
            if (data_.end() == iter) {
                return nullptr;
            }

            return &iter->second;
        }

        const list_type* get_list(TKey... keys) const {
            return get_list(std::forward_as_tuple(keys...));
        }

        value_type get(key_type k, size_t index) const {
            const list_type* ls = get_list(k);
            if (nullptr == ls) {
                return value_type();
            }

            if (index >= ls->size()) {
                return value_type();
            }

            return (*ls)[index];
        }

        value_type get(TKey... keys, size_t index) const {
            return get(std::forward_as_tuple(keys...), index);
        }

        void foreach(std::function<void (const value_type&)> fn) const {
            for (auto iter = data_.begin(); iter != data_.end(); ++iter) {
                for (auto item = iter->second.begin(); item != iter->second.end(); ++item) {
                    fn(*item);
                }
            }
        }

        void set_sort_rule(sort_func_type fn) {
            sort_func_ = fn;
        }
    private:
        std::map<key_type, list_type> data_;
        sort_func_type sort_func_;
    };
}
