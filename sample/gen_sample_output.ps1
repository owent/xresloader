$PSDefaultParameterValues['*:Encoding'] = 'UTF-8'

$OutputEncoding = [System.Text.UTF8Encoding]::new()

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Definition

Set-Location $SCRIPT_DIR

$XRESLOADER = Get-ChildItem -Path "../target" -Filter "xresloader-*-jar-with-dependencies.jar" -Name | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if (-Not $XRESLOADER) {
    $XRESLOADER = Get-ChildItem -Path "../target" -Filter "*.jar" -Name -Exclude "original-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
}


$XRESLOADER = "../target/" + $XRESLOADER

if (!(Test-Path $XRESLOADER)) {
    Write-Output "xresloader not found."
    exit 0;
}

Write-Output "Using xresloader=$XRESLOADER"

foreach ($proto_dir in "proto_v2", "proto_v3") {
    Write-Output "Generate sample data for $proto_dir, one per cmd";
    $XLSX_FILE = Get-ChildItem -Name -Filter "*.xlsx" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    & java -client -jar "$XRESLOADER" -t lua -p protobuf -o "$proto_dir" -f "$proto_dir/kind.pb" --pretty 2 -c kind_const.lua ;
    & java -client -jar "$XRESLOADER" -t lua -p protobuf -o "$proto_dir" -f "$proto_dir/kind.pb" --pretty 2 --lua-module ProtoEnums.Kind -c kind_const_module.lua ;
    & java -client -jar "$XRESLOADER" -t bin -p protobuf -o "$proto_dir" -f "$proto_dir/kind.pb" -s "$XLSX_FILE" -m scheme_kind -a 1.0.0.0 ;
    & java -client -jar "$XRESLOADER" -t lua -p protobuf -o "$proto_dir" -f "$proto_dir/kind.pb" --pretty 4 -s "$XLSX_FILE" -m scheme_kind -n '/(?i)\.bin$/\.lua/' --data-version 1.0.0.0 ;
    & java -client -jar "$XRESLOADER" -t lua -p protobuf -o "$proto_dir" -f "$proto_dir/kind.pb" --lua-module ProtoData.Kind -s "$XLSX_FILE" -m scheme_kind -n '/(?i)\.bin$/_module\.lua/' --data-version 1.0.0.0 ;
    Write-Output "Generate sample data for $proto_dir, batchmode using --stdin";
    $TASK_LINES = @(
        "-t lua -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' --pretty 2 -i kind.desc.lua",
        "-t json -p protobuf -o '$proto_dir'    -f '$proto_dir/kind.pb' --pretty 2 -r kind.desc.json",
        "-t json -p protobuf -o '$proto_dir'    -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_kind -n '/(?i)\.bin$/\.json/'",
        "-t xml -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_kind -n '/(?i)\.bin$/\.xml/'",
        "-t msgpack -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_kind -n '/(?i)\.bin$/\.msgpack.bin/'",
        "-t js -p protobuf -o '$proto_dir'      -f '$proto_dir/kind.pb' --pretty 2 -s '$XLSX_FILE' -m scheme_kind -n '/(?i)\.bin$/\.js/' --javascript-global sample.xresloader",
        "-t js -p protobuf -o '$proto_dir'      -f '$proto_dir/kind.pb' --pretty 2 -m 'DataSource=$XLSX_FILE|kind|3,1' -m 'MacroSource=$XLSX_FILE|macro|2,1' -m ProtoName=role_cfg -m OutputFile=role_cfg.n.js -m KeyRow=2 -m KeyCase=lower -m KeyWordSplit=_ -m 'KeyWordRegex=[A-Z_\$ \t\r\n]|[_\$ \t\r\n]|[a-zA-Z_\$]' --javascript-export nodejs",
        "-t js -p protobuf -o '$proto_dir'      -f '$proto_dir/kind.pb' --pretty 2 -s '$XLSX_FILE' -m scheme_kind -n '/(?i)\.bin$/\.amd\.js/' --javascript-export amd",
        "-t lua -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' --pretty 2 -m 'DataSource=$XLSX_FILE|arr_in_arr|3,1' -m 'MacroSource=$XLSX_FILE|macro|2,1' -m ProtoName=arr_in_arr_cfg -m OutputFile=arr_in_arr_cfg.lua -m KeyRow=2",
        "-t bin -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' -m 'DataSource=$XLSX_FILE|arr_in_arr|3,1' -m 'MacroSource=$XLSX_FILE|macro|2,1' -m ProtoName=arr_in_arr_cfg -m OutputFile=arr_in_arr_cfg.bin -m KeyRow=2",
        "-t xml -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' --pretty 2 -m 'DataSource=$XLSX_FILE|arr_in_arr|3,1' -m 'MacroSource=$XLSX_FILE|macro|2,1' -m ProtoName=arr_in_arr_cfg -m OutputFile=arr_in_arr_cfg.xml -m KeyRow=2",
        "-t bin -p protobuf -o '$proto_dir'    -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_upgrade --disable-excel-formular",
        "-t json -p protobuf -o '$proto_dir'    -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_upgrade -n '/(?i)\.bin$/\.json/' --pretty 2 --disable-excel-formular",
        "-t lua -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_upgrade -n '/(?i)\.bin$/\.lua/' --pretty 2 --disable-excel-formular",
        "-t lua -o '$proto_dir' -f '$proto_dir/kind.pb' --pretty 2 -i kind_option.lua",
        "-t lua -o '$proto_dir' -f '$proto_dir/kind.pb' --pretty 2 --lua-module ProtoOptions.Kind -i kind_option.mod.lua",
        "-t js -o '$proto_dir' -f '$proto_dir/kind.pb' --pretty 2 -i kind_option.js --javascript-export nodejs",
        "-t bin -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' --pretty 2 -m 'DataSource=$XLSX_FILE|test_oneof|3,1' -m 'MacroSource=$XLSX_FILE|macro|2,1' -m ProtoName=event_cfg -m OutputFile=event_cfg.bin -m KeyRow=2",
        "-t lua -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' --pretty 2 -m 'DataSource=$XLSX_FILE|test_oneof|3,1' -m 'MacroSource=$XLSX_FILE|macro|2,1' -m ProtoName=event_cfg -m OutputFile=event_cfg.lua -m KeyRow=2",
        "-t ue-json -o '$proto_dir/json' -f '$proto_dir/kind.pb' -c KindConst.json",
        "-t ue-json -o '$proto_dir/json' -f '$proto_dir/kind.pb' -m 'DataSource=$XLSX_FILE|arr_in_arr|3,1' -m 'MacroSource=$XLSX_FILE|macro|2,1' -m ProtoName=arr_in_arr_cfg -m OutputFile=ArrInArrCfg.json -m KeyRow=2 -m UeCfg-CodeOutput=|Public/ConfigRec|Private/ConfigRec",
        "-t ue-json -o '$proto_dir/json' -f '$proto_dir/kind.pb' -m 'DataSource=$XLSX_FILE|upgrade_10001|3,1' -m 'DataSource=$XLSX_FILE|upgrade_10002|3,1' -m 'MacroSource=$XLSX_FILE|macro|2,1' -m ProtoName=role_upgrade_cfg -m OutputFile=RoleUpgradeCfg.json -m KeyRow=2 -m UeCfg-CodeOutput=|Public/ConfigRec|Private/ConfigRec",
        "-t ue-csv -o '$proto_dir/csv' -f '$proto_dir/kind.pb' -m 'DataSource=$XLSX_FILE|arr_in_arr|3,1' -m 'MacroSource=$XLSX_FILE|macro|2,1' -m ProtoName=arr_in_arr_cfg -m OutputFile=ArrInArrCfgRec.csv -m KeyRow=2 -m UeCfg-CodeOutput=|Public/ConfigRec|Private/ConfigRec",
        "-t ue-csv -o '$proto_dir/csv' -f '$proto_dir/kind.pb' -m 'DataSource=$XLSX_FILE|upgrade_10001|3,1' -m 'DataSource=$XLSX_FILE|upgrade_10002|3,1' -m 'MacroSource=$XLSX_FILE|macro|2,1' -m ProtoName=role_upgrade_cfg -m OutputFile=RoleUpgradeCfgRec.csv -m KeyRow=2 -m UeCfg-CodeOutput=|Public/ConfigRec|Private/ConfigRec",
        "-t ue-json -o '$proto_dir/json' -f '$proto_dir/kind.pb' -m 'DataSource=$XLSX_FILE|test_oneof|3,1' -m 'MacroSource=$XLSX_FILE|macro|2,1' -m ProtoName=event_cfg -m OutputFile=EventCfg.json -m KeyRow=2 -m UeCfg-CodeOutput=|Public/ConfigRec|Private/ConfigRec",
        "-t ue-csv -o '$proto_dir/csv' -f '$proto_dir/kind.pb' -m 'DataSource=$XLSX_FILE|test_oneof|3,1' -m 'MacroSource=$XLSX_FILE|macro|2,1' -m ProtoName=event_cfg -m OutputFile=EventCfgRec.csv -m KeyRow=2 -m UeCfg-CodeOutput=|Public/ConfigRec|Private/ConfigRec",
        "-t lua -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' --pretty 2 -m 'DataSource=$XLSX_FILE|keep_or_strip_empty_list|3,1' -m ProtoName=keep_or_strip_empty_list_cfg -m OutputFile=keep_empty_list_cfg.lua -m KeyRow=2 --list-keep-empty",
        "-t ue-json -o '$proto_dir/json' -f '$proto_dir/kind.pb' -m 'DataSource=$XLSX_FILE|keep_or_strip_empty_list|3,1' -m ProtoName=keep_or_strip_empty_list_cfg -m OutputFile=KeepEmptyListCfg.json -m KeyRow=2 -m UeCfg-CodeOutput=|Public/ConfigRec|Private/ConfigRec --list-keep-empty",
        "-t ue-csv -o '$proto_dir/csv' -f '$proto_dir/kind.pb' -m 'DataSource=$XLSX_FILE|keep_or_strip_empty_list|3,1' -m ProtoName=keep_or_strip_empty_list_cfg -m OutputFile=KeepEmptyListCfg.csv -m KeyRow=2 -m UeCfg-CodeOutput=|Public/ConfigRec|Private/ConfigRec --list-keep-empty",
        "-t bin -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' -m 'DataSource=$XLSX_FILE|keep_or_strip_empty_list|3,1' -m ProtoName=keep_or_strip_empty_list_cfg -m OutputFile=keep_empty_list_cfg.bin -m KeyRow=2 --list-keep-empty",
        "-t lua -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' --pretty 2 -m 'DataSource=$XLSX_FILE|keep_or_strip_empty_list|3,1' -m ProtoName=keep_or_strip_empty_list_cfg -m OutputFile=strip_list_tail_cfg.lua -m KeyRow=2 --list-strip-empty-tail",
        "-t ue-json -o '$proto_dir/json' -f '$proto_dir/kind.pb' -m 'DataSource=$XLSX_FILE|keep_or_strip_empty_list|3,1' -m ProtoName=keep_or_strip_empty_list_cfg -m OutputFile=StripListTailCfg.json -m KeyRow=2 -m UeCfg-CodeOutput=|Public/ConfigRec|Private/ConfigRec --list-strip-empty-tail",
        "-t ue-csv -o '$proto_dir/csv' -f '$proto_dir/kind.pb' -m 'DataSource=$XLSX_FILE|keep_or_strip_empty_list|3,1' -m ProtoName=keep_or_strip_empty_list_cfg -m OutputFile=StripListTailCfg.csv -m KeyRow=2 -m UeCfg-CodeOutput=|Public/ConfigRec|Private/ConfigRec --list-strip-empty-tail",
        "-t bin -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' -m 'DataSource=$XLSX_FILE|keep_or_strip_empty_list|3,1' -m ProtoName=keep_or_strip_empty_list_cfg -m OutputFile=strip_list_tail_cfg.bin -m KeyRow=2 --list-strip-empty-tail"
    )
    Write-Output "Write-Output '$(Join-String -Separator `n -InputObject $TASK_LINES)' | java `"-Dfile.encoding=UTF-8`" `"-Dlog4j.appender.console.encoding=UTF-8`" -client -jar `"$XRESLOADER`" --stdin --data-version 1.0.0.0"
    Join-String -Separator "`n" -InputObject $TASK_LINES | java "-Dfile.encoding=UTF-8" "-Dlog4j.appender.console.encoding=UTF-8" -client -jar "$XRESLOADER" --stdin --data-version 1.0.0.0
}
