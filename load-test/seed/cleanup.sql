-- seed.sql로 넣은 [LOADTEST] 마커 더미 데이터를 전량 삭제한다.
-- 사용법: psql -h localhost -d coming -f cleanup.sql

BEGIN;

DELETE FROM user_follow_artist
WHERE artist_id IN (SELECT id FROM artist WHERE name LIKE '[LOADTEST]%');

DELETE FROM concert_artist
WHERE artist_id IN (SELECT id FROM artist WHERE name LIKE '[LOADTEST]%')
   OR concert_id IN (SELECT id FROM concert WHERE kopis_id LIKE 'LOADTEST-%');

DELETE FROM release_group
WHERE artist_id IN (SELECT id FROM artist WHERE name LIKE '[LOADTEST]%');

DELETE FROM concert WHERE kopis_id LIKE 'LOADTEST-%';

DELETE FROM artist WHERE name LIKE '[LOADTEST]%';

DELETE FROM inquiry WHERE title LIKE '[LOADTEST]%';

DELETE FROM "user" WHERE provider = 'local' AND provider_id = 'k6-load-test';

COMMIT;
