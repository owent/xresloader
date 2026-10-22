#!/usr/bin/env python3
"""Compile jcstress actors against a packaged JAR, then exercise that exact JAR."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--work-dir", type=Path, default=Path("build/concurrency-stress-run"))
    parser.add_argument("--java", default="java")
    parser.add_argument("--javac", default="javac")
    parser.add_argument("--maven", default="mvn")
    parser.add_argument("jcstress_args", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[1]
    jar = args.jar.resolve(strict=True)
    work = args.work_dir.resolve()
    if work.exists() and any(work.iterdir()):
        parser.error("Use a new work directory to preserve reports")
    classes = work / "classes"
    classes.mkdir(parents=True)
    classpath_file = work / "dependencies.txt"
    maven = shutil.which(args.maven)
    if maven is None:
        parser.error("Maven executable not found")
    subprocess.run([maven, "-B", "-f", str(repo / "tools/concurrency-stress/pom.xml"),
                    "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath",
                    f"-Dmdep.outputFile={classpath_file}"], check=True)
    dependencies = classpath_file.read_text(encoding="utf-8").strip()
    compile_classpath = os.pathsep.join([str(jar), dependencies])
    sources = sorted((repo / "tools/concurrency-stress/src/main/java").rglob("*.java"))
    subprocess.run([args.javac, "--release", "17", "-encoding", "UTF-8", "-cp", compile_classpath,
                    "-processorpath", dependencies, "-processor", "org.openjdk.jcstress.infra.processors.JCStressTestProcessor",
                    "-d", str(classes), *(str(p) for p in sources)], check=True)
    extra = args.jcstress_args
    if extra[:1] == ["--"]:
        extra = extra[1:]
    command = [args.java, "-Xms32m", "-Xmx256m", "-cp", os.pathsep.join([str(classes), compile_classpath]),
               "org.openjdk.jcstress.Main", "-t", "org.xresloader.core.*Stress", "-r", str(work / "reports")]
    command += extra or ["-m", "quick", "-af", "NONE", "-c", "4", "-hs", "128", "-pth", "false",
                        "-iters", "3", "-time", "200", "-f", "2"]
    with jar.open("rb") as stream:
        metadata = {"jar": str(jar), "sha256": hashlib.file_digest(stream, "sha256").hexdigest(), "command": command}
    (work / "invocation.json").write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    # Result blobs and all fork logs remain inside the explicitly selected work directory.
    subprocess.run(command, cwd=work, check=True)


if __name__ == "__main__":
    main()
