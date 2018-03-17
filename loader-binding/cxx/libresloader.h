#ifndef LIBRESLOADER_RESLOADER_H
#define LIBRESLOADER_RESLOADER_H

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
#include <type_traits>
#include <map>

#if defined(__cplusplus) && __cplusplus >= 201103L
    #include <unordered_map>
    #include <tuple>
#endif

#include <google/protobuf/stubs/common.h>

#if GOOGLE_PROTOBUF_VERSION < 3000000
#include "pb_header.pb.h"
#else
#include "pb_header_v3.pb.h"
#endif


namespace xresloader {
    namespace details {
#if defined(__cplusplus) && __cplusplus >= 201103L

        // Implementing some of the C++14 features in C++11
        template <size_t... I> class index_sequence {};

        template <size_t N, size_t... I>
        struct make_index_sequence
            : public make_index_sequence<N-1, N-1, I...>
        {};
        template <size_t... I>
        struct make_index_sequence<0, I...>
            : public index_sequence<I...>
        {};


        // hash dector
        template<typename Ty, typename... TTP>
        struct conf_hash_dector;


        template<typename... TTP>
        struct conf_hash_dector<std::tuple<TTP...> > {

            typedef std::tuple<TTP...> tpl_t;

            struct hash_fn {

                template<size_t I>
                static size_t make_ele_hash(size_t &res, const tpl_t& tpl) {
                    std::hash<typename std::tuple_element<I, tpl_t>::type> h;
                    res ^= h(std::get<I>(tpl));
                    return res;
                }

                template<size_t... I>
                static void make_tpl_hash(size_t &res, const tpl_t& tpl, index_sequence<I...>) {
                    std::make_tuple(make_ele_hash<I>(res, tpl)...);
                }

                size_t operator()(const tpl_t& tpl) const {
                    size_t ret = 0;
                    make_tpl_hash(ret, tpl, make_index_sequence<sizeof...(TTP)>());
                    return ret;
                }
            };

            typedef hash_fn type;
        };

        template<typename Ty, typename... TTP>
        struct conf_hash_dector {
            typedef std::hash<Ty> type;
        };

#endif

        /**
         * @brief 配置集合管理器基类
         */
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
            conf_manager_base(): data_count_(0) {}
            virtual ~conf_manager_base() {}

            virtual bool filter(const key_type& key, value_type val) = 0;

            virtual void on_load() {};
            virtual void on_loaded() {};

            inline size_t get_header_count() const { return data_count_; }

        private:
            bool build() {
                // TODO 检查校验码
                xresloader_version_ = root_.mutable_header()->xres_ver();
                data_version_ = root_.mutable_header()->data_ver();
                data_count_ = root_.mutable_header()->count();

                on_load();

                int len = root_.data_block_size();
                for (int i = 0; i < len; ++i) {
                    const std::string& data_block = root_.data_block(i);
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
            size_t data_count_;
            std::list<filter_func_type> filter_list_;
        };


        template<typename... TKey>
        class conf_manager_index_key_base {
        public:
            typedef  std::function<size_t(const std::tuple<TKey...>&)> index_key_fn_t;

        public:
            void set_index_key_handle(index_key_fn_t fn) { fn_ = fn; }

            const index_key_fn_t& get_index_key_handle() const { return fn_; }
            index_key_fn_t& get_index_key_handle() { return fn_; }

        private:
            index_key_fn_t fn_;
        };

        template<typename... TKey>
        class conf_manager_index_key_auto : public conf_manager_index_key_base<TKey...> {
        public:
            conf_manager_index_key_auto() {
                conf_manager_index_key_base<TKey...>::set_index_key_handle(unwrapper);
            }
        private:
            static size_t unwrapper(const std::tuple<TKey...>& tpl) {
                return static_cast<size_t>(std::get<0>(tpl));
            }
        };
    }

    /**
     * @brief key-value 型配置集合管理器
     * @note c++11完全支持得模式下会使用hash map来管理数据
     */
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
#if defined(__cplusplus) && __cplusplus >= 201103L
        typedef std::unordered_map<key_type, value_type, typename details::conf_hash_dector<key_type>::type > data_map_t;
#else
        typedef std::map<key_type, value_type> data_map_t;
#endif
        data_map_t data_;
    };

    /**
     * @brief key-list 型配置集合管理器
     * @note c++11完全支持得模式下会使用hash map来管理数据
     */
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
                typename data_map_t::iterator iter = data_.begin();
                for (; data_.end() != iter; ++iter) {
                    std::stable_sort(iter->second.begin(), iter->second.end(), sort_func_);
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

    protected:
#if defined(__cplusplus) && __cplusplus >= 201103L
        typedef std::unordered_map<key_type, list_type, typename details::conf_hash_dector<key_type>::type> data_map_t;
#else
        typedef std::map<key_type, list_type> data_map_t;
#endif
        data_map_t data_;
        sort_func_type sort_func_;
    };

    /**
     * @brief index-value 型配置集合管理器
     */
    template<typename TItem, typename... TKey>
    class conf_manager_iv :
        public details::conf_manager_base<TItem, TKey...>,
        public std::conditional<
            1 == sizeof...(TKey) && std::is_integral<
                typename std::tuple_element<0, std::tuple<TKey...> >::type
            >::value,
            details::conf_manager_index_key_auto<TKey...>,
            details::conf_manager_index_key_base<TKey...>
        >::type {
    public:
        typedef details::conf_manager_base<TItem, TKey...> base_type;
        typedef typename std::conditional<
            1 == sizeof...(TKey) && std::is_integral<
                typename std::tuple_element<0, std::tuple<TKey...> >::type
            >::value,
            details::conf_manager_index_key_auto<TKey...>,
            details::conf_manager_index_key_base<TKey...>
        >::type index_base_type;

        typedef typename base_type::key_type key_type;
        typedef typename base_type::value_type value_type;
        typedef typename base_type::proto_type proto_type;

        typedef typename base_type::func_type func_type;
        typedef typename base_type::filter_func_type filter_func_type;

    protected:
        virtual bool filter(const key_type& key, value_type val) {
            size_t index = index_base_type::get_index_key_handle()(key);

            if (data_.size() <= index) {
                data_.resize(index + 1);
            }
            if (data_[index]) {
                std::cerr << "[WARN] key "<< index<< " appear more than once will be covered" << std::endl;
            }

            data_[index] = val;
            return true;
        }

        virtual void on_load() {
            reserve(base_type::get_header_count());
        };

    public:

        void clear() {
            data_.clear();
        }

        size_t size() const {
            return data_.size();
        }

        void reserve(size_t s) {
            data_.reserve(s);
        }

        size_t capacity() const {
            return data_.capacity();
        }

        value_type get(key_type k) const {
            size_t index = index_base_type::get_index_key_handle()(k);
            if (index >= data_.size()) {
                return value_type();
            }

            return data_[index];
        }

        value_type get(TKey... keys) const {
            return get(std::forward_as_tuple(keys...));
        }

        void foreach(std::function<void (const value_type&)> fn) const {
            for (size_t i = 0; i < data_.size(); ++ i) {
                fn(data_[i]);
            }
        }

    private:
        typedef std::vector<value_type> data_vec_t;
        data_vec_t data_;
    };

    /**
     * @brief key-index-list 型配置集合管理器
     * @note c++11完全支持得模式下会使用hash map来管理数据
     */
    template<typename TItem, typename... TKey>
    class conf_manager_kil : public conf_manager_kl<TItem, TKey...> {
    public:
        typedef conf_manager_kl<TItem, TKey...> base_type;
       
        typedef typename base_type::key_type key_type;
        typedef typename base_type::value_type value_type;
        typedef typename base_type::list_type list_type;
        typedef typename base_type::proto_type proto_type;

        typedef typename base_type::func_type func_type;
        typedef typename base_type::filter_func_type filter_func_type;
        typedef typename base_type::sort_func_type sort_func_type;
        typedef std::function<size_t(const value_type&)> index_key_fn_t;;

    protected:
        virtual bool filter(const key_type& key, value_type val) {
            list_type& ls = base_type::data_[key];
            size_t index = index_fn_(val);
            if (ls.size() <= index) {
                ls.resize(index + 1);
            }

            if (ls[index]) {
                std::cerr << "[WARN] key with index="<< index<< " appear more than once will be covered" << std::endl;
            }

            ls[index] = val;
            return true;
        }

    public:
        inline void set_index_handle(index_key_fn_t fn) { index_fn_ = fn; }

    private:
        index_key_fn_t index_fn_;
    };
}

#endif
