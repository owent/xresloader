#!/bin/sh

cd "$(dirname $0)";
SCRIPT_DIR="$PWD";

XRESLOADER="$(ls -t ../target/*.jar | head -n 1)";
if [ -z "$XRESLOADER" ] || [ ! -e "$XRESLOADER" ]; then
    echo "xresloader not found.";
    exit 0;
fi

echo "Using xresloader=$XRESLOADER";

for proto_dir in proto_v2 proto_v3; do
    echo "Generate sample data for $proto_dir, one per cmd";
    XLSX_FILE="$(ls *.xlsx)";
    java -client -jar "$XRESLOADER" -t lua -p protobuf -o $proto_dir -f $proto_dir/kind.pb --pretty 2 -c kind_const.lua ;
    java -client -jar "$XRESLOADER" -t bin -p protobuf -o $proto_dir -f $proto_dir/kind.pb -s $XLSX_FILE -m scheme_kind -a 1.0.0.0;
    java -client -jar "$XRESLOADER" -t lua -p protobuf -o $proto_dir -f $proto_dir/kind.pb --pretty 4 -s $XLSX_FILE -m scheme_kind -n "/(?i)\.bin\$/\.lua/" --data-version 1.0.0.0 ;
    echo "Generate sample data for $proto_dir, batchmode using --stdin";
    echo '
        -t json -p protobuf -o '$proto_dir'    -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_kind -n "/(?i)\.bin$/\.json/"
        -t xml -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_kind -n "/(?i)\.bin$/\.xml/"
        -t msgpack -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_kind -n "/(?i)\.bin$/\.msgpack.bin/"
        -t js -p protobuf -o '$proto_dir'      -f '$proto_dir/kind.pb' --pretty 2 -s '$XLSX_FILE' -m scheme_kind -n "/(?i)\.bin$/\.js/" --javascript-global sample 
        -t js -p protobuf -o '$proto_dir'      -f '$proto_dir/kind.pb' --pretty 2 -m DataSource='$XLSX_FILE'|kind|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=role_cfg -m OutputFile=role_cfg.n.js -m KeyRow=2 -m KeyCase=lower -m KeyWordSplit=_ -m "KeyWordRegex=[A-Z_\$ \t\r\n]|[_\$ \t\r\n]|[a-zA-Z_\$]" --javascript-export nodejs 
        -t js -p protobuf -o '$proto_dir'      -f '$proto_dir/kind.pb' --pretty 2 -s '$XLSX_FILE' -m scheme_kind -n "/(?i)\.bin$/\.amd\.js/" --javascript-export amd 
        -t lua -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' --pretty 2 -m DataSource='$XLSX_FILE'|arr_in_arr|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=arr_in_arr_cfg -m OutputFile=arr_in_arr_cfg.lua -m KeyRow=2 -o proto_v3
        -t bin -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' -m DataSource='$XLSX_FILE'|arr_in_arr|3,1 -m MacroSource='$XLSX_FILE'|macro|2,1 -m ProtoName=arr_in_arr_cfg -m OutputFile=arr_in_arr_cfg.bin -m KeyRow=2 -o proto_v3
        -t json -p protobuf -o '$proto_dir'    -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_upgrade -n "/(?i)\.bin$/\.json/"
        -t lua -p protobuf -o '$proto_dir'     -f '$proto_dir/kind.pb' -s '$XLSX_FILE' -m scheme_upgrade -n "/(?i)\.bin$/\.lua/"
    ' | java -client -jar "$XRESLOADER" --stdin --data-version 1.0.0.0;
done
