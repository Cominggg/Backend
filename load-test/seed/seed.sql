-- 부하테스트 전용 더미 데이터.
-- artist.name / concert.kopis_id / release_group.title 에 [LOADTEST] 마커를 붙여 실 데이터와 구분한다.
-- cleanup.sql로 전량 삭제 가능. Flyway 마이그레이션에는 포함하지 않는다.
--
-- 사용법: psql -h localhost -d coming -v test_user_id={dev-login으로 발급받은 유저 id} -f seed.sql

BEGIN;

-- 1. 더미 아티스트 2,000건
INSERT INTO artist (mbid, name, sort_name, is_coming)
SELECT
    gen_random_uuid()::text,
    '[LOADTEST] Artist ' || i,
    'LOADTEST Artist ' || i,
    (i % 10 = 0)
FROM generate_series(1, 2000) AS i;

-- 2. 더미 공연 5,000건
INSERT INTO concert (kopis_id, title, start_date, end_date, venue_name, status, view_count, kopis_update_date)
SELECT
    'LOADTEST-' || i,
    '[LOADTEST] Concert ' || i,
    CURRENT_DATE + (i % 180),
    CURRENT_DATE + (i % 180) + 1,
    'LOADTEST Venue ' || (i % 50),
    (ARRAY['UPCOMING', 'ONGOING', 'ENDED'])[1 + (i % 3)],
    (i * 7) % 1000,
    CURRENT_DATE
FROM generate_series(1, 5000) AS i;

-- 3. 공연-아티스트 조인 (무작위 매칭, 충돌 시 무시하므로 최종 건수는 8,000보다 약간 적을 수 있음)
WITH loadtest_artists AS (
    SELECT array_agg(id) AS ids FROM artist WHERE name LIKE '[LOADTEST]%'
), loadtest_concerts AS (
    SELECT array_agg(id) AS ids FROM concert WHERE kopis_id LIKE 'LOADTEST-%'
)
INSERT INTO concert_artist (concert_id, artist_id)
SELECT
    loadtest_concerts.ids[1 + floor(random() * array_length(loadtest_concerts.ids, 1))::int],
    loadtest_artists.ids[1 + floor(random() * array_length(loadtest_artists.ids, 1))::int]
FROM generate_series(1, 8000), loadtest_artists, loadtest_concerts
ON CONFLICT (concert_id, artist_id) DO NOTHING;

-- 4. 디스코그래피 3,000건
WITH loadtest_artists AS (
    SELECT array_agg(id) AS ids FROM artist WHERE name LIKE '[LOADTEST]%'
)
INSERT INTO release_group (mbid, artist_id, title, type, first_release_date)
SELECT
    gen_random_uuid()::text,
    loadtest_artists.ids[1 + floor(random() * array_length(loadtest_artists.ids, 1))::int],
    '[LOADTEST] Release ' || i,
    (ARRAY['ALBUM', 'SINGLE', 'EP'])[1 + (i % 3)],
    CURRENT_DATE - (i % 1000)
FROM generate_series(1, 3000) AS i, loadtest_artists;

-- 5. 팔로우 200건 (concert-following.js / artist-detail 팔로우 흐름 테스트용 — dev-login으로 만든 테스트 유저 기준)
INSERT INTO user_follow_artist (user_id, artist_id, created_at)
SELECT :test_user_id, id, now()
FROM artist
WHERE name LIKE '[LOADTEST]%'
ORDER BY random()
LIMIT 200;

COMMIT;
