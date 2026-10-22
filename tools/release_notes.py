#!/usr/bin/env python3
"""Use an annotated tag's message as the complete GitHub Release body."""

import argparse
import json
from pathlib import Path
import re
import subprocess
import sys


def git(*arguments, repository="."):
    return subprocess.run(["git", *arguments], cwd=repository, check=True,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout


def tag_message(tag, commit, repository="."):
    if not re.fullmatch(r"[0-9a-fA-F]{40}|[0-9a-fA-F]{64}", commit):
        raise ValueError("The build commit must be a full Git object ID")
    reference = "refs/tags/" + tag
    git("check-ref-format", reference, repository=repository)
    tag_object = git("rev-parse", "--verify", reference, repository=repository).strip().decode("ascii")
    if git("cat-file", "-t", tag_object, repository=repository).strip() != b"tag":
        raise ValueError("Release notes require an annotated tag, not a lightweight tag")
    target = git("rev-parse", tag_object + "^{commit}", repository=repository).strip().decode("ascii")
    if target.lower() != commit.lower():
        raise ValueError("The release tag does not point to the build commit")
    contents = git("cat-file", "tag", tag_object, repository=repository)
    _, separator, message = contents.partition(b"\n\n")
    if not separator:
        raise ValueError("The annotated tag has no message")
    # Git identifies signatures independently of the annotation text.
    signature = git("for-each-ref", "--format=%(contents:signature)", reference,
                    repository=repository).rstrip(b"\n")
    if git("rev-parse", "--verify", reference, repository=repository).strip().decode("ascii") != tag_object:
        raise ValueError("The release tag changed while its message was being read")
    if signature:
        start = message.rfind(signature)
        if start < 0 or message[start + len(signature):].strip():
            raise ValueError("Cannot separate the tag signature from its message")
        message = message[:start]
    text = message.decode("utf-8")
    if not text.strip():
        raise ValueError("The annotated tag message must not be empty")
    return text


def verify_release(release, tag, message):
    if release.get("tag_name") != tag:
        raise ValueError("The uploaded Release belongs to a different tag")
    if release.get("body") != message:
        raise ValueError("The uploaded Release body differs from the annotated tag message")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--commit", required=True)
    operation = parser.add_mutually_exclusive_group(required=True)
    operation.add_argument("--output", type=Path)
    operation.add_argument("--verify-release", type=Path, help="GitHub Release JSON returned by gh api")
    args = parser.parse_args()
    message = tag_message(args.tag, args.commit)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_bytes(message.encode("utf-8"))
    else:
        verify_release(json.loads(args.verify_release.read_text(encoding="utf-8")), args.tag, message)
        print("Release body matches the annotated tag message")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        detail = error.stderr.decode("utf-8", errors="replace").strip() if isinstance(
            error, subprocess.CalledProcessError) else str(error)
        print(f"Release notes failed: {detail}", file=sys.stderr)
        sys.exit(1)
