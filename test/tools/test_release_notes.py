import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tools"))
import release_notes


class ReleaseNotesTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.repository = Path(self.temporary.name)
        self.git("init", "--quiet")
        self.git("config", "user.name", "Release fixture")
        self.git("config", "user.email", "release@example.invalid")
        self.git("config", "commit.gpgSign", "false")
        self.git("config", "tag.gpgSign", "false")
        self.git("commit", "--quiet", "--allow-empty", "-m", "Build commit")
        self.commit = self.git("rev-parse", "HEAD").strip().decode("ascii")

    def git(self, *arguments, input=None):
        return subprocess.run(["git", *arguments], cwd=self.repository, input=input, check=True,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout

    def tag(self, name="v1.0", message="Release notes\n", target=None):
        path = self.repository / "message.txt"
        path.write_bytes(message.encode("utf-8"))
        self.git("tag", "--annotate", "--cleanup=verbatim", "--file", str(path),
                 name, target or self.commit)

    def test_preserves_complete_multiline_utf8_and_literal_shell_text(self):
        message = "## 更新\n\n- 并发转换\n- `$(echo literal)` $HOME ${{ example }}\n\n"
        self.tag(message=message)
        self.assertEqual(message, release_notes.tag_message("v1.0", self.commit, self.repository))

    def test_lightweight_tag_is_rejected_instead_of_using_commit_message(self):
        self.git("tag", "v1.0")
        with self.assertRaisesRegex(ValueError, "annotated tag"):
            release_notes.tag_message("v1.0", self.commit, self.repository)

    def test_empty_annotated_tag_is_rejected(self):
        self.tag(message=" \n\n")
        with self.assertRaisesRegex(ValueError, "must not be empty"):
            release_notes.tag_message("v1.0", self.commit, self.repository)

    def test_tag_must_resolve_to_build_commit(self):
        self.tag()
        self.git("commit", "--quiet", "--allow-empty", "-m", "Another commit")
        another = self.git("rev-parse", "HEAD").strip().decode("ascii")
        with self.assertRaisesRegex(ValueError, "build commit"):
            release_notes.tag_message("v1.0", another, self.repository)

    def test_nested_tag_uses_outer_message(self):
        self.tag("inner", "Inner annotation\n")
        self.tag(message="Outer annotation\n", target="inner")
        self.assertEqual("Outer annotation\n", release_notes.tag_message("v1.0", self.commit, self.repository))

    def test_signed_tag_does_not_include_signature_in_release_body(self):
        self.tag()
        original = self.git("cat-file", "tag", "refs/tags/v1.0")
        signed = original + b"-----BEGIN PGP SIGNATURE-----\nfixture\n-----END PGP SIGNATURE-----\n"
        tag_object = self.git("mktag", input=signed).strip().decode("ascii")
        self.git("update-ref", "refs/tags/v1.0", tag_object)
        self.assertEqual("Release notes\n", release_notes.tag_message("v1.0", self.commit, self.repository))

    def test_release_verification_requires_matching_tag_and_complete_body(self):
        message = "## Changes\n\nExact body\n"
        release_notes.verify_release({"tag_name": "v1.0", "body": message}, "v1.0", message)
        for release in [{"tag_name": "other", "body": message},
                        {"tag_name": "v1.0", "body": message.rstrip()},
                        {"tag_name": "v1.0", "body": "Generated notes"}]:
            with self.assertRaises(ValueError):
                release_notes.verify_release(release, "v1.0", message)

    def test_cli_writes_and_verifies_exact_body(self):
        message = "## CHANGELOG\n\n- 发布测试\n"
        self.tag(message=message)
        script = Path(release_notes.__file__).resolve()
        output = self.repository / "notes" / "body.md"
        command = [sys.executable, str(script), "--tag", "v1.0", "--commit", self.commit]
        subprocess.run([*command, "--output", str(output)], cwd=self.repository, check=True,
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        self.assertEqual(message.encode("utf-8"), output.read_bytes())
        release = self.repository / "release.json"
        release.write_text(json.dumps({"tag_name": "v1.0", "body": message}), encoding="utf-8")
        subprocess.run([*command, "--verify-release", str(release)], cwd=self.repository, check=True,
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE)


if __name__ == "__main__":
    unittest.main()
