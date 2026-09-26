"""PreToolUse: 시크릿 파일 접근과 커밋된 Flyway 마이그레이션 수정을 차단한다.

Bash는 명령을 토큰으로 나눠 각 토큰의 파일명을 검사한다. 따옴표로 묶인 문장(공백 포함 토큰)과
heredoc 본문은 제외하므로 커밋 메시지·PR 본문의 언급은 걸리지 않는다.
문자열 조립 같은 의도적 우회까지 막지는 않는다 — 실수 방지용이다.
"""
import json
import os
import re
import shlex
import subprocess
import sys

SECRET_NAME = re.compile(r"^(\.env(\..+)?|application-(local|secret).*|credentials(\..+)?|.*\.secrets?)$")
SECRET_ALLOWED = {".env.example"}
HEREDOC = re.compile(r"<<-?\s*(['\"]?)(\w+)\1.*?\n(.*?)^\s*\2\s*$", re.S | re.M)
REDIRECT_PREFIX = re.compile(r"^[0-9&]*[<>|]+")


def deny(reason):
    print(json.dumps({"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "deny",
        "permissionDecisionReason": reason,
    }}, ensure_ascii=False))
    sys.exit(0)


def is_secret(path):
    name = os.path.basename(path.rstrip("/"))
    return bool(SECRET_NAME.match(name)) and name not in SECRET_ALLOWED


def bash_targets(command):
    command = HEREDOC.sub("", command)
    lexer = shlex.shlex(command, posix=True, punctuation_chars=";&|<>()")
    lexer.whitespace_split = True
    try:
        tokens = list(lexer)
    except ValueError:
        tokens = command.split()
    # 공백이 든 토큰은 따옴표로 묶인 문장이므로 제외, `--file=.env`는 `=` 뒤만 본다
    tokens = [t for t in tokens if not re.search(r"\s", t)]
    return [REDIRECT_PREFIX.sub("", t).rsplit("=", 1)[-1] for t in tokens]


data = json.load(sys.stdin)
tool = data.get("tool_name", "")
tool_input = data.get("tool_input", {})
path = tool_input.get("file_path", "")
name = os.path.basename(path)

if tool == "Bash":
    targets = bash_targets(tool_input.get("command", ""))
elif tool == "Grep":
    targets = [tool_input.get("path", "")]
else:
    targets = [path]
hits = [t for t in targets if t and is_secret(t)]
if hits:
    deny(f"시크릿 파일 접근 차단: {', '.join(hits)} — 필요한 내용은 사용자에게 직접 수정을 요청한다")

if tool in ("Write", "Edit") and "/db/migration/" in path and name.startswith("V"):
    tracked = subprocess.run(
        ["git", "ls-files", "--error-unmatch", path],
        cwd=data.get("cwd") or ".", capture_output=True,
    ).returncode == 0
    if tracked:
        deny(f"커밋된 Flyway 마이그레이션 수정 차단: {path} — 변경은 새 V{{n}}__*.sql 파일로 추가한다")
