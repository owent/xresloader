-- this file is generated by xresloader, please don't edit it.

return {
  [1] = {
    count = 7,
    data_source = {
      {
        count = 5,
        file = "资源转换示例.xlsx",
        sheet = "test_oneof",
      },
    },
    data_ver = "1.0.0.0",
    description = "Test event_cfg with oneof fields",
    hash_code = "sha256:f54ea4a17140ed6e75a9136bff6c0053251bb5a4cd3954d905cf5bac8fcce998",
    xres_ver = "2.15.1",
  },
  [2] = "event_cfg",
  event_cfg = {
    {
      id = 50001,
      item = {
        item_count = 1512,
        item_id = 1511,
        nested_note = "啦啦啦",
      },
      process = 1,
      rule = {
        nested_note = "内嵌one of文本",
        rule_id = 511,
        rule_param = 512,
      },
      specify_field = {
        nested_enum_type = 10001,
      },
      test_arr = {
        {
          nested_note = "内嵌one of文本",
          rule_id = 711,
          rule_param = 712,
        },
      },
    },
    {
      id = 50002,
      process = 2,
      rule = {
        nested_enum_type = 10001,
        rule_id = 521,
        rule_param = 522,
      },
      specify_field = {
        nested_enum_type = 10001,
      },
      test_arr = {
        {
          nested_enum_type = 10001,
          rule_id = 721,
          rule_param = 722,
        },
        {
          nested_enum_type = 10101,
          rule_id = 821,
          rule_param = 822,
        },
        {
          nested_note = "数组嵌套one of",
          rule_id = 921,
          rule_param = 822,
        },
      },
      user_exp = 1473624235014786048,
    },
    {
      enum_type = 10001,
      id = 50003,
      process = 3,
      rule = {
        nested_enum_type = 10101,
        rule_id = 531,
        rule_param = 532,
      },
      specify_field = {
        nested_enum_type = 10101,
      },
    },
    {
      id = 50004,
      note = "",
      process = 4,
      rule = {
        rule_id = 541,
        rule_param = 542,
      },
    },
    {
      id = 50004,
      note = "",
      process = 6,
      rule = {
        rule_id = 561,
        rule_param = 562,
      },
    },
  },
}