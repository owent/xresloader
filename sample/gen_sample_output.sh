#!/bin/sh

cd "$(dirname $0)";
SCRIPT_DIR="$PWD";

XRESLOADER="$(ls -t ../target/xresloader-*-jar-with-dependencies.jar 2>/dev/null | head -n 1)";
if [ -z "$XRESLOADER" ] || [ ! -e "$XRESLOADER" ]; then
    XRESLOADER="$(ls -t ../target/xresloader-*.jar 2>/dev/null | head -n 1)";
fi

if [ -z "$XRESLOADER" ] || [ ! -e "$XRESLOADER" ]; then
    echo "xresloader not found.";
    exit 0;
fi

echo "Using xresloader=$XRESLOADER";

for proto_dir in proto_v2 proto_v3; do
    echo "Generate sample data for $proto_dir, one per cmd";
    XLSX_FILE="$(ls *.xlsx | grep -v 'grep' | grep -v "\\~\\$")";
    find "$proto_dir" -iname "UnreaImportSettings.json" -exec rm -fv "{}" ";" ;
    java -client -jar "$XRESLOADER" -t lua -p protobuf -o $proto_dir -f $proto_dir/kind.pb --pretty 2 -c kind_const.lua ;
    java -client -jar "$XRESLOADER" -t lua -p protobuf -o $proto_dir -f $proto_dir/kind.pb --pretty 2 --lua-module ProtoEnums.Kind -c kind_const_module.lua ;
    java -client -jar "$XRESLOADER" -t bin -p protobuf -o $proto_dir -f $proto_dir/kind.pb -s $XLSX_FILE -m scheme_kind -a 1.0.0.0;
    java -client -jar "$XRESLOADER" -t lua -p protobuf -o $proto_dir -f $proto_dir/kind.pb --pretty 4 -s $XLSX_FILE -m scheme_kind -n "/(?i)\.bin\$/\.lua/" --data-version 1.0.0.0 ;
    java -client -jar "$XRESLOADER" -t lua -p protobuf -o $proto_dir -f $proto_dir/kind.pb --lua-module ProtoData.Kind -s $XLSX_FILE -m scheme_kind -n "/(?i)\.bin\$/_module\.lua/" --data-version 1.0.0.0 ;
    echo "Generate sample data for $proto_dir, batchmode using --stdin";
    CMDS='
        -t lua -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' --pretty 2 -i kind.desc.lua
        -t json -p protobuf -o '$proto_dir'    -f '$proto_dir/kind.pb' --pretty 2 -i kind.desc.json
        -t json -p protobuf -o '$proto_dir'    -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_kind -n "/(?i)\.bin$/\.json/"
        -t xml -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_kind -n "/(?i)\.bin$/\.xml/"
        -t msgpack -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_kind -n "/(?i)\.bin$/\.msgpack.bin/"
        -t js -p protobuf -o '$proto_dir'      -f '$proto_dir/kind.pb' --pretty 2 -s '$XLSX_FILE' -m scheme_kind -n "/(?i)\.bin$/\.js/" --javascript-global sample.xresloader 
        -t js -p protobuf -o '$proto_dir'      -f '$proto_dir/kind.pb' --pretty 2 -m DataSource='$XLSX_FILE'|kind|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=role_cfg -m OutputFile=role_cfg.n.js -m KeyRow=2 -m KeyCase=lower -m KeyWordSplit=_ -m "KeyWordRegex=[A-Z_\$ \t]|[_\$ \t]|[a-zA-Z_\$]" --javascript-export nodejs 
        -t js -p protobuf -o '$proto_dir'      -f '$proto_dir/kind.pb' --pretty 2 -s '$XLSX_FILE' -m scheme_kind -n "/(?i)\.bin$/\.amd\.js/" --javascript-export amd 
        -t lua -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' --pretty 2 -m DataSource='$XLSX_FILE'|arr_in_arr|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=arr_in_arr_cfg -m OutputFile=arr_in_arr_cfg.lua -m KeyRow=2 -o '$proto_dir'
        -t bin -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' -m DataSource='$XLSX_FILE'|arr_in_arr|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=arr_in_arr_cfg -m OutputFile=arr_in_arr_cfg.bin -m KeyRow=2 -o '$proto_dir'
        -t json -p protobuf -o '$proto_dir'    -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_upgrade -n "/(?i)\.bin$/\.json/"
        -t lua -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_upgrade -n "/(?i)\.bin$/\.lua/"
        -t lua -o '$proto_dir'     -f '$proto_dir/kind.pb' --pretty 2 -i kind_option.lua
        -t lua -o '$proto_dir'     -f '$proto_dir/kind.pb' --pretty 2 --lua-module ProtoOptions.Kind -i kind_option.mod.lua
        -t js -o '$proto_dir'      -f '$proto_dir/kind.pb' --pretty 2 -i kind_option.js --javascript-export nodejs
        -t bin -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' --pretty 2 -m DataSource='$XLSX_FILE'|test_oneof|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=event_cfg -m OutputFile=event_cfg.bin -m KeyRow=2 -o '$proto_dir'
        -t lua -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' --pretty 2 -m DataSource='$XLSX_FILE'|test_oneof|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=event_cfg -m OutputFile=event_cfg.lua -m KeyRow=2 -o '$proto_dir'
        -t ue-csv -o '$proto_dir/csv' -f '$proto_dir/kind.pb' -c KindConst.csv  -m UeCfg-RecursiveMode=false
        -t ue-json -o '$proto_dir/json' -f '$proto_dir/kind.pb' -c KindConst.json
        -t ue-csv -o '$proto_dir/csv' -f '$proto_dir/kind.pb' -m DataSource='$XLSX_FILE'|arr_in_arr|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=arr_in_arr_cfg -m OutputFile=ArrInArrCfg.csv -m KeyRow=2 -m UeCfg-CodeOutput=|Public/Config|Private/Config  -m UeCfg-RecursiveMode=false
        -t ue-json -o '$proto_dir/json' -f '$proto_dir/kind.pb' -m DataSource='$XLSX_FILE'|arr_in_arr|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=arr_in_arr_cfg -m OutputFile=ArrInArrCfg.json -m KeyRow=2 -m UeCfg-CodeOutput=|Public/Config|Private/Config
        -t ue-csv -o '$proto_dir/csv' -f '$proto_dir/kind.pb' -m DataSource='$XLSX_FILE'|upgrade_10001|3,1 -m DataSource='$XLSX_FILE'|upgrade_10002|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=role_upgrade_cfg -m OutputFile=RoleUpgradeCfg.csv -m KeyRow=2 -m UeCfg-CodeOutput=|Public/Config|Private/Config -m UeCfg-RecursiveMode=false
        -t ue-json -o '$proto_dir/json' -f '$proto_dir/kind.pb' -m DataSource='$XLSX_FILE'|upgrade_10001|3,1 -m DataSource='$XLSX_FILE'|upgrade_10002|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=role_upgrade_cfg -m OutputFile=RoleUpgradeCfg.json -m KeyRow=2 -m UeCfg-CodeOutput=|Public/Config|Private/Config
        -t ue-csv -o '$proto_dir/csv' -f '$proto_dir/kind.pb' -m DataSource='$XLSX_FILE'|arr_in_arr|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=arr_in_arr_cfg -m OutputFile=ArrInArrCfgRec.csv -m KeyRow=2 -m UeCfg-CodeOutput=|Public/ConfigRec|Private/ConfigRec
        -t ue-csv -o '$proto_dir/csv' -f '$proto_dir/kind.pb' -m DataSource='$XLSX_FILE'|upgrade_10001|3,1 -m DataSource='$XLSX_FILE'|upgrade_10002|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=role_upgrade_cfg -m OutputFile=RoleUpgradeCfgRec.csv -m KeyRow=2 -m UeCfg-CodeOutput=|Public/ConfigRec|Private/ConfigRec
        -t ue-json -o '$proto_dir/json' -f '$proto_dir/kind.pb' -m DataSource='$XLSX_FILE'|test_oneof|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=event_cfg -m OutputFile=EventCfg.json -m KeyRow=2 -m UeCfg-CodeOutput=|Public/Config|Private/Config
        -t ue-csv -o '$proto_dir/csv' -f '$proto_dir/kind.pb' -m DataSource='$XLSX_FILE'|test_oneof|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=event_cfg -m OutputFile=EventCfg.csv -m KeyRow=2 -m UeCfg-CodeOutput=|Public/Config|Private/Config -m UeCfg-RecursiveMode=false
        -t ue-csv -o '$proto_dir/csv' -f '$proto_dir/kind.pb' -m DataSource='$XLSX_FILE'|test_oneof|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=event_cfg -m OutputFile=EventCfgRec.csv -m KeyRow=2 -m UeCfg-CodeOutput=|Public/ConfigRec|Private/ConfigRec
    ';
    echo "Run with --stdin: $CMDS";
    echo "$CMDS" | java -client -jar "$XRESLOADER" --stdin;
done
