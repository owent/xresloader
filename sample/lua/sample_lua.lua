--region sample_lua.lua
--Author : OWenT
--Date   : 2015/01/17
--启动载入项
-- 需要追加框架目录和数据根目录到package.path, 如:
-- package.path=package.path .. ';../?.lua;/cygdrive/c/workspace/projs/github/owent-utils/lua/src/?.lua'

local loader = require('utils.loader')
local class = loader.load('utils.class')
local cfg = loader.load('data.conf_manager')

-- 搜索模式为%s,如果设置为data.%s，则cfg:load('role_cfg') 会使用 require('data.role_cfg')
-- 比如如果配置协议的package名称是 a.b.c那么这个值应该是cfg:set_path_rule('a.b.c.%s') 
cfg:set_path_rule('%s')

-- 下面这个接口是在执行reload的时候会重新require的路径，可以在这个文件里重新读取配置
-- cfg:set_list('data.conf_list') -- cfg:reload() 会在清空配置数据后执行require('data.conf_list')

-- role_cfg
cfg:load_kv('role_cfg', function(k, v)
    return v.id or k
end)

cfg:load_kv('role_cfg', function(k, v)
    return v.id or k
end, 'alias_name')

-- 别名和非别名的数据一样的
vardump(cfg:get('role_cfg'):get(10002))
vardump(cfg:get('alias_name'):get(10002))

-- 设置为只读
cfg:set_readonly()

local kind = cfg:get('role_cfg'):get(10002)

-- 只读保护之后不会看到原始数据
vardump(kind)

print(string.format('kind id=%d, name=%s, dep_test.name=%s', kind.id, kind.name, kind.dep_test.name))
