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
$TRANS_DIRS = @(
    "proto_v2"
    "proto_v3"
)
foreach ($proto_dir in $TRANS_DIRS) {
    Write-Output "Generate sample data for $proto_dir, one per cmd";
    $XLSX_FILE = "资源转换示例-大文件.xlsx"
    Write-Output "Generate sample data for $proto_dir, batchmode using --stdin";
    $DATA_SOURCES = @(
        "-m 'DataSource=$XLSX_FILE|process_by_script1|2,1'"
        "-m 'DataSource=$XLSX_FILE|process_by_script2|2,1'"
        "-m 'DataSource=$XLSX_FILE|process_by_script3|2,1'"
        "-m 'DataSource=$XLSX_FILE|process_by_script4|2,1'"
        "-m 'DataSource=$XLSX_FILE|process_by_script5|2,1'"
        "-m 'DataSource=$XLSX_FILE|process_by_script6|2,1'"
        "-m 'DataSource=$XLSX_FILE|process_by_script7|2,1'"
        "-m 'DataSource=$XLSX_FILE|process_by_script8|2,1'"
    )
    $TASK_LINES = @(

        # 回调脚本参与转表
        " -t json -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' -m CallbackScript=cb_script.js $($DATA_SOURCES -join ' ') -m ProtoName=large_file_test -m OutputFile=large_file_test.json -m KeyRow=1 --pretty 2 --list-strip-empty-tail"
        " -t bin  -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' -m CallbackScript=cb_script.js $($DATA_SOURCES -join ' ') -m ProtoName=large_file_test -m OutputFile=large_file_test.bin  -m KeyRow=1 --list-strip-empty-tail"
        " -t lua  -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' -m CallbackScript=cb_script.js $($DATA_SOURCES -join ' ') -m ProtoName=large_file_test -m OutputFile=large_file_test.lua  -m KeyRow=1 --pretty 2 --list-strip-empty-tail"

        # 去掉回调，空回调耗时与之相差不大
        # " -t bin  -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' $($data_sources -join " ") -m protoname=large_file_test -m outputfile=large_file_test.bin -m keyrow=1 --list-strip-empty-tail"
        # " -t json -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' $($data_sources -join " ") -m protoname=large_file_test -m outputfile=large_file_test.json -m keyrow=1 --pretty 2 --list-strip-empty-tail"
        # " -t lua  -p protobuf -o '$proto_dir' -f '$proto_dir/kind.pb' $($data_sources -join " ") -m protoname=large_file_test -m outputfile=large_file_test.lua -m keyrow=1 --pretty 2 --list-strip-empty-tail"
    )
    Write-Output "Write-Output '$($TASK_LINES -join "`n")' | java `"-Dfile.encoding=UTF-8`" `"-Dlog4j.appender.console.encoding=UTF-8`" -client -jar `"$XRESLOADER`" --stdin --data-version 1.0.0.0"
    Measure-Command {$TASK_LINES -join "`n" | java "-Dfile.encoding=UTF-8" "-Dlog4j.appender.console.encoding=UTF-8" -client -jar "$XRESLOADER" --stdin --data-version 1.0.0.0}

}
