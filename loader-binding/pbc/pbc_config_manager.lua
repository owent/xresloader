--region data.pbc_config_manager.lua
--Author : OWenT
--Date   : 2015/01/17
--启动载入项

local class = require('utils.class')
local loader = require('utils.loader')

local conf_set = class.register('data.pbc_config_data_set')

-- 必须保证pbc已经载入
local pbc = protobuf

function conf_set:get_by_table(key)
    if not self or not key then
        return nil
    end
    local len = #key
    local i = 1
    local ret = self.__data

    while ret and i <= len do
        ret = ret[key[i]]
        i = i + 1
    end

    return ret
end

function conf_set:get(...)
    return self:get_by_table({...})
end

function conf_set:get_all()
    return self.__data
end

function conf_set:set_by_table(args)
    if not self or not args or #args < 2 then
        return nil
    end

    local i = 1
    local ret = self.__data
    local len = #args - 2
    while i <= len do
        if not ret[args[i]] then
            ret[args[i]] = {}
        end
        ret = ret[args[i]]
        i = i + 1
    end

    ret[args[len + 1]] = args[len + 2]
    return args[len + 2]
end

function conf_set:set(...)
    return self:set_by_table({...})
end


local pbc_config_manager = class.register('data.pbc_config_manager', class.singleton)

pbc_config_manager.__path_rule = '%s'
pbc_config_manager.__list_path = 'data.conf_list'
pbc_config_manager.__data = {}

function pbc_config_manager:set_path_rule(rule)
    self.__path_rule = rule
end

function pbc_config_manager:set_list(l)
    self.__list_path = l
end

function pbc_config_manager:load_datablocks(path, data_blocks, data_collector_fn, kv_fn, cfg_set_name)
    cfg_set_name = cfg_set_name or path
    pbc_config_manager.__data[cfg_set_name] = pbc_config_manager.__data[cfg_set_name] or conf_set.new({__data = {}})
    local cfg = pbc_config_manager.__data[cfg_set_name]

    path = string.format(self.__path_rule, tostring(path))

    kv_fn = kv_fn or function(k, v)
        return k
    end

    for ck, cv in ipairs(data_blocks.data_block) do
        -- log_debug('config content %s', string.gsub(cv, '.', function(x) return string.format('%02X', string.byte(x)) end) )
        local rv, error_text = pbc.decode(path, cv)

        if false == rv then
            log_error('decode config item failed, path=%s: %s', tostring(path), error_text)
            return false
        end

        -- pbc 解包扩展
        if 'function' == type(pbc.extract) then
            pbc.extract(rv)
        end

        local rk = { kv_fn(ck, rv) }
        data_collector_fn(cfg, rk, rv)
    end

    return true
end

function pbc_config_manager:load_buffer_kv(path, buffers, kv_fn, cfg_set_name)
    local msg, error_text = pbc.decode("com.owent.xresloader.pb.xresloader_datablocks", buffers)
    if false == msg then
        log_error('decode buffer failed, path=%s: %s', tostring(path), error_text)
        return false
    end

    return self:load_datablocks(path, msg,
        function(cfg, rk, rv)
            if cfg:get_by_table(rk) then
                for i = 1, #rk, 1 do
                    if 0 ~= rk[i] and "" ~= rk[i] and nil ~= rk[i] then
                        log_warn('config [%s] already has key %s, old record will be covered', path, table.concat(rk, ', '))
                        break
                    end
                end
            end

            table.insert(rk, rv)
            cfg:set_by_table(rk)
        end, kv_fn, cfg_set_name
    )
end

function pbc_config_manager:load_buffer_kl(path, buffers, kv_fn, cfg_set_name)
    local msg, error_text = pbc.decode("com.owent.xresloader.pb.xresloader_datablocks", buffers)
    if false == msg then
        log_error('decode buffer failed, path=%s: %s', tostring(path), error_text)
        return false
    end

    return self:load_datablocks(path, msg,
        function(cfg, rk, rv)
            local ls = cfg:get_by_table(rk)
            if ls then
                table.insert(ls, rv)
            else
                ls = { rv }
                table.insert(rk, ls)
                cfg:set_by_table(rk)
            end
        end, kv_fn, cfg_set_name
    )
end

function pbc_config_manager:set_readonly()
    self.__data = class.set_readonly(self.__data)
end

function pbc_config_manager:get(type_name)
    return self.__data[type_name] or nil
end

function pbc_config_manager:reload()
    self.__data = {}

    loader.remove(self.__list_path)
    loader.load(self.__list_path)
end

return pbc_config_manager
