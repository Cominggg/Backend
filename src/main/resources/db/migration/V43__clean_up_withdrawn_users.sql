-- 기존 탈퇴 회원을 새 탈퇴 정책(개인정보 파기, 재로그인 시 새 계정)에 맞춘다.
-- 탈퇴 시각은 기록이 없어 마지막 수정 시각으로 대신한다.
DELETE FROM rating
WHERE user_id IN (SELECT id FROM "user" WHERE status = 'INACTIVE');

UPDATE "user"
SET provider_id      = 'withdrawn:' || id,
    email            = NULL,
    nickname         = NULL,
    birth_year       = NULL,
    agreed_marketing = NULL,
    withdrawn_at     = COALESCE(withdrawn_at, updated_at)
WHERE status = 'INACTIVE'
  AND provider_id NOT LIKE 'withdrawn:%';
