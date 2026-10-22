#!/usr/bin/env python3
"""Replay xresconv-cli previews in one JVM; isolate outputs and compare every byte.

The CLI's --test expansion is the source of truth for includes, classes, and order.
GUI hooks are deliberately not executed. Each measured run starts a fresh JVM;
filesystem caches are warm after the unmeasured original-JAR reference run.
"""
import argparse
import ctypes
import hashlib
import json
import os
from pathlib import Path
import random
import re
import shutil
import statistics
import subprocess
import threading
import time
import xml.etree.ElementTree as ET

TOKENS = re.compile(r"('[^']*')|(\"[^\"]*\")|(\S+)")
ANSI = re.compile(r"\x1b\[[0-9;]*m")


def tokenize(line):
    return [m[0][1:-1] if m[0][0] in "\"'" else m[0] for m in TOKENS.finditer(line)]


def quote(value):
    if any(c in value for c in "\r\n\0"):
        raise ValueError("stdin argument contains a line break or NUL")
    for delimiter in ['"', "'"]:
        if delimiter not in value:
            return delimiter + value + delimiter
    raise ValueError("stdin argument cannot be represented")


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def snapshot(root):
    return {p.relative_to(root).as_posix(): digest(p)
            for p in sorted(root.rglob("*")) if p.is_file()}


def process_metrics(process):
    if os.name != "nt":
        return {}
    from ctypes import wintypes

    class Counters(ctypes.Structure):
        _fields_ = [("cb", wintypes.DWORD), ("faults", wintypes.DWORD)] + [
            (name, ctypes.c_size_t) for name in ["peak_ws", "ws", "peak_paged", "paged",
                                               "peak_nonpaged", "nonpaged", "pagefile", "peak_pagefile"]]
    counters = Counters()
    counters.cb = ctypes.sizeof(counters)
    handle = wintypes.HANDLE(int(process._handle))
    ctypes.windll.psapi.GetProcessMemoryInfo(handle, ctypes.byref(counters), counters.cb)
    times = [ctypes.c_uint64() for _ in range(4)]
    ctypes.windll.kernel32.GetProcessTimes(handle, *(ctypes.byref(t) for t in times))
    return {"peak_working_set_bytes": counters.peak_ws,
            "cpu_seconds": (times[2].value + times[3].value) / 10_000_000}


def prepare(args, root):
    result = subprocess.run([str(args.cli.resolve()), "--test", "--parallelism", "1", str(args.xml.resolve())],
                            capture_output=True, check=True, encoding="utf-8")
    preview = ANSI.sub("", result.stdout)
    (root / "preview.txt").write_text(preview, encoding="utf-8")
    cwd = Path(next(line.split("dir: ", 1)[1] for line in preview.splitlines()
                    if line.startswith("[NOTICE] start to run conv cmds on dir: ")))
    launch = tokenize(next(line for line in preview.splitlines() if line.startswith('"')))
    java_options = launch[1:launch.index("-jar")]
    java_options = [v for v in java_options if not v.startswith(("-Xmx", "-Xms"))]
    java_options += ["-Xms64m", f"-Xmx{args.heap_mb}m"]
    output = root / "output"
    commands, destinations = [], {}
    input_roots = {cwd}
    for line in preview.splitlines():
        if not line.strip().startswith("> "):
            continue
        row = tokenize(line.strip()[2:])
        value_options = {"--validator-rules", "--ignore-field-tags", "--pretty", "-f", "-d", "-p", "-o",
                         "-a", "-t", "-n", "-m", "--data-src-dir", "--output-dir"}
        flag_options = {"--transpose-data-source", "--list-keep-empty", "--list-strip-empty-tail",
                        "--list-strip-all-empty"}
        index = 0
        while index < len(row):
            option = row[index]
            if option in flag_options:
                index += 1
                continue
            if option not in value_options or index + 1 == len(row):
                raise ValueError(f"Review IO and callback behavior before benchmarking option {option}")
            value = row[index + 1]
            if option == "-m" and value.split("=", 1)[0] not in {"DataSource", "ProtoName", "OutputFile", "KeyRow", "MacroSource"}:
                raise ValueError("Review additional scheme IO before benchmarking this configuration")
            if option == "-t" and value not in {"bin", "json"}:
                raise ValueError("This benchmark currently isolates bin/json outputs only")
            index += 2
        for i, value in enumerate(row[:-1]):
            if value in ("-d", "--data-src-dir"):
                input_roots.add((cwd / row[i + 1]).resolve())
            if value in ("-o", "--output-dir"):
                original = row[i + 1]
                destinations.setdefault(original, f"destination-{len(destinations)}")
                row[i + 1] = str(output / destinations[original])
            if value == "-m" and row[i + 1].startswith("OutputFile="):
                filename = row[i + 1].split("=", 1)[1]
                if Path(filename).is_absolute() or ".." in Path(filename).parts:
                    raise ValueError("OutputFile escapes the isolated directory")
        if "-o" not in row and "--output-dir" not in row:
            raise ValueError("Every benchmark command must specify its output directory")
        commands.append(" ".join(quote(v) for v in row))
    payload = ("\n".join(commands) + "\n").encode("utf-8")
    (root / "commands.txt").write_bytes(payload)
    inputs = {str(p): snapshot(p) for p in sorted(input_roots)}
    (root / "inputs-before.json").write_text(json.dumps(inputs, indent=2), encoding="utf-8")
    return cwd, java_options, output, payload, inputs, destinations


def run_once(args, root, prepared, label, jar, degree, reference=None):
    cwd, java_options, output, payload, _, _ = prepared
    # Delete only our marked, resolved output directory beneath this run's workspace.
    if output.resolve().parent != root.resolve() or not (root / ".benchmark-root").is_file():
        raise ValueError("Unsafe benchmark output directory")
    if output.exists():
        shutil.rmtree(output)
    output.mkdir()
    config = ET.parse(Path(__file__).resolve().parents[1] / "src/org/xresloader/core/resource/log4j2.xml")
    config.find("./Properties/Property[@name='filename']").text = str(root / f"{label}.java.log")
    config_path = root / "log4j2.xml"
    config.write(config_path, encoding="utf-8", xml_declaration=True)
    command = [args.java, *java_options, f"-Dlog4j2.configurationFile={config_path.as_uri()}",
               "-jar", str(jar.resolve()), "--stdin", "--data-version", "parallelism-benchmark"]
    if degree is not None and degree != "default":
        command += ["--parallelism", str(degree)]
    env = dict(os.environ)
    for key in ["JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"]:
        env.pop(key, None)
    started = time.perf_counter()
    with (root / f"{label}.stdout.log").open("wb") as stdout, (root / f"{label}.stderr.log").open("wb") as stderr:
        process = subprocess.Popen(command, cwd=cwd, stdin=subprocess.PIPE, stdout=stdout, stderr=stderr, env=env)
        failure = []
        def communicate():
            try:
                process.communicate(payload, timeout=args.timeout)
            except BaseException as error:
                failure.append(str(error))
                process.kill()
                process.communicate()
        feeder = threading.Thread(target=communicate)
        feeder.start()
        metrics = {}
        while feeder.is_alive():
            sample = process_metrics(process)
            metrics = {k: max(metrics.get(k, 0), v) for k, v in sample.items()}
            feeder.join(0.1)
        elapsed = time.perf_counter() - started
        metrics.update({k: max(metrics.get(k, 0), v) for k, v in process_metrics(process).items()})
    files = snapshot(output)
    messages = ANSI.sub("", (root / f"{label}.stderr.log").read_text(encoding="utf-8", errors="replace"))
    # Multi-line diagnostics can interleave; compare complete output bytes and error/warning headers separately.
    headers = sorted(line for line in messages.splitlines() if line.startswith(("[ERROR", "[WARN", "[FATAL")))
    result = {"label": label, "parallelism": degree, "seconds": elapsed, **metrics,
              "exit_code": process.returncode, "errors": failure, "files": files,
              "diagnostic_headers": headers, "jar_sha256": digest(jar)}
    if reference is not None:
        result["matches_reference"] = (files == reference["files"] and process.returncode == reference["exit_code"]
                                       and headers == reference["diagnostic_headers"] and not failure)
        result["different_files"] = sorted(k for k in files.keys() | reference["files"].keys()
                                            if files.get(k) != reference["files"].get(k))
    (root / f"{label}.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(json.dumps({k: v for k, v in result.items() if k not in ["files", "diagnostic_headers", "different_files"]}
                     | {"file_count": len(files), "diagnostics": len(headers),
                        "differences": len(result.get("different_files", []))}), flush=True)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cli", type=Path, required=True)
    parser.add_argument("--xml", type=Path, required=True)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--reference-jar", type=Path, required=True)
    parser.add_argument("--work-dir", type=Path, required=True)
    parser.add_argument("--java", default="java")
    parser.add_argument("--heap-mb", type=int, default=2048)
    parser.add_argument("--degrees", nargs="+", default=["1", "2", "4", "8", "16", "auto"])
    parser.add_argument("--repeats", type=int, default=3)
    parser.add_argument("--compare-reference-performance", action="store_true",
                        help="Interleave an old-JAR serial run into every measured round")
    parser.add_argument("--timeout", type=int, default=600)
    args = parser.parse_args()
    if args.repeats < 1 or args.heap_mb < 1 or args.timeout < 1:
        parser.error("repeats, heap-mb and timeout must be positive")
    for degree in args.degrees:
        if degree not in ("auto", "default") and (not degree.isdecimal() or int(degree) < 1):
            parser.error("degrees must be positive integers, auto, or default")
    root = args.work_dir.resolve()
    if root.exists() and any(root.iterdir()):
        raise ValueError("Use a new work directory to preserve prior evidence")
    root.mkdir(parents=True, exist_ok=True)
    (root / ".benchmark-root").touch()
    prepared = prepare(args, root)
    reference = run_once(args, root, prepared, "reference", args.reference_jar, None)
    results = []
    randomizer = random.Random(20261022)
    measured_degrees = args.degrees + ([None] if args.compare_reference_performance else [])
    for repeat in range(args.repeats):
        degrees = measured_degrees.copy()
        randomizer.shuffle(degrees)
        for degree in degrees:
            results.append(run_once(args, root, prepared, f"r{repeat + 1}-p{degree}",
                                    args.jar if degree is not None else args.reference_jar, degree, reference))
    inputs_after = {p: snapshot(Path(p)) for p in prepared[4]}
    report = {"xml": str(args.xml.resolve()), "tasks": len(prepared[3].splitlines()),
              "heap_mb": args.heap_mb, "output_directories": prepared[5],
              "inputs_unchanged": inputs_after == prepared[4], "reference": reference, "runs": results,
              "summary": {str(p): {"median_seconds": statistics.median(r["seconds"] for r in results if r["parallelism"] == p),
                                    "seconds": [r["seconds"] for r in results if r["parallelism"] == p]}
                          for p in measured_degrees}}
    (root / "report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps({k: report[k] for k in ["tasks", "inputs_unchanged", "summary"]}), flush=True)
    if not report["inputs_unchanged"] or not all(r["matches_reference"] for r in results):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
