local const_res = {
  game_const_config = {
    EN_GCC_SCREEN_WIDTH = 1136,
    EN_GCC_SCREEN_HEIGHT = 640,
    EN_GCC_CAMERA_OFFSET = 268,
    EN_GCC_FORMULAR_TYPE_MASK = 10,
    EN_GCC_LEVEL_LIMIT = 999,
    EN_GCC_RESOURCE_MAX_LIMIT = 9999999,
    EN_GCC_SOLDIER_TYPE_MASK = 100,
    EN_GCC_PERCENT_BASE = 10000,
    EN_GCC_RANDOM_RANGE_UNIT = 10,
    EN_GCC_ACTIVITY_TYPE_MASK = 1000,
  },
}

for k, v in pairs(const_res) do
  _G[k] = v
end

return const_res
