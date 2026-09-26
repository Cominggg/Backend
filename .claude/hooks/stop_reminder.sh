#!/bin/sh
# Stop: 커밋되지 않은 Java 변경이 있을 때만 커밋 전 워크플로우를 안내한다.
cd "$CLAUDE_PROJECT_DIR" 2>/dev/null || exit 0
[ -n "$(git status --porcelain -- '*.java')" ] || exit 0
echo '{"systemMessage": "⚠️ 커밋되지 않은 Java 변경 있음 — write-tests → ./gradlew test → /be-review (auth 변경 시 + security-reviewer) → /simplify → /commit → /pr"}'
