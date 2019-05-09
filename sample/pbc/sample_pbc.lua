--region sample_pbc.lua
--Author : owent
--Date   : 2016/04/15
--启动载入项
-- 需要追加框架目录、数据根目和pbc的lua文件录到package.path, 如:
-- package.path=package.path .. ';../?.lua;/c/workspace/projs/github/owent-utils/lua/src/?.lua'
-- package.path=package.path .. ';/c/workspace/projs/github/owent-contrib/pbc/binding/lua/?.lua'
-- 还需要追加pbc的动态库所在目录到package.cpath, 如:
-- package.cpath=package.cpath .. ';/c/workspace/projs/github/owent-contrib/pbc/binding/lua/?.so'

local loader = require('utils.loader')
local class = loader.load('utils.class')
local cfg = loader.load('data.pbc_config_manager')

-- 加载pbc的lua文件，可以换成自己的路径
loader.load('protobuf')
local pbc = protobuf

-- 搜索模式为%s,如果设置为data.%s，则cfg:load('role_cfg') 会使用 require('data.role_cfg')
cfg:set_path_rule('%s')
-- 下面这个接口是在执行reload的时候会重新require的路径，可以在这个文件里重新读取配置
-- cfg:set_list('data.conf_list') -- cfg:reload() 会在清空配置数据后执行require('data.conf_list')


-- 读取文件数据
local cfg_file = io.open('../role_cfg.bin')
local cfg_buffer = cfg_file:read('*a')
cfg_file:close()

-- role_cfg
cfg:load_buffer_kv('role_cfg', cfg_buffer, function(k, v)
    return v.id or k
end)

-- 别名
cfg:load_buffer_kv('role_cfg', cfg_buffer, function(k, v)
    return v.id or k
end, 'alias_name')

-- 别名和非别名的数据一样的
vardump(cfg:get('role_cfg'):get(10002))
vardump(cfg:get('alias_name'):get(10002))

-- 设置为只读
cfg:set_readonly()


local kind = cfg:get('role_cfg'):get(10002)

vardump(kind)
print(string.format('kind id=%d, name=%s, dep_test.name=%s', kind.id, kind.name, kind.dep_test.name))
