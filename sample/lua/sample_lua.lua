--region sample_lua.lua
--Author : OWenT
--Date   : 2015/01/17
--启动载入项
-- 需要追加框架目录和数据根目录到package.path, 如:
-- package.path=package.path .. ';/cygdrive/d/projs/github/xresloader/sample/?.lua;/cygdrive/d/projs/github/OWenT-s-Utils/src/?.lua'

local loader = require('utils.loader')
local class = loader.load('utils.class')
local cfg = loader.load('data.conf_manager')

-- 搜索模式为%s,如果设置为data.%s，则cfg:load('role_cfg') 会使用 require('data.role_cfg')
cfg:set_path_rule('%s')

-- role_cfg
cfg:load('role_cfg', function(k, v)
    return v.id or k
end)


vardump(cfg:get('role_cfg'):get(10002))

-- 设置为只读
cfg:set_readonly()


local kind = cfg:get('role_cfg'):get(10002)

vardump(kind)
print(string.format('kind id=%d, name=%s, dep_test.name=%s', kind.id, kind.name, kind.dep_test.name))
