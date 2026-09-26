"""PreToolUse: 시크릿 파일 접근과 커밋된 Flyway 마이그레이션 수정을 차단한다."""
import json
import os
import re
import subprocess
import sys

SECRET_NAME = re.compile(r"^(\.env(\..+)?|application-(local|secret).*|credentials(\..+)?|.*\.secrets?)$")
SECRET_ALLOWED = {".env.example"}


def deny(reason):
    print(json.dumps({"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "deny",
        "permissionDecisionReason": reason,
    }}, ensure_ascii=False))
    sys.exit(0)


data = json.load(sys.stdin)
tool = data.get("tool_name", "")
path = data.get("tool_input", {}).get("file_path", "")
name = os.path.basename(path)

if SECRET_NAME.match(name) and name not in SECRET_ALLOWED:
    deny(f"시크릿 파일 접근 차단: {path} — 필요한 내용은 사용자에게 직접 수정을 요청한다")

if tool in ("Write", "Edit") and "/db/migration/" in path and name.startswith("V"):
    tracked = subprocess.run(
        ["git", "ls-files", "--error-unmatch", path],
        cwd=data.get("cwd") or ".", capture_output=True,
    ).returncode == 0
    if tracked:
        deny(f"커밋된 Flyway 마이그레이션 수정 차단: {path} — 변경은 새 V{{n}}__*.sql 파일로 추가한다")
