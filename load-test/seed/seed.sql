-- 부하테스트 전용 더미 데이터.
-- artist.name / concert.kopis_id / release_group.title 에 [LOADTEST] 마커를 붙여 실 데이터와 구분한다.
-- cleanup.sql로 전량 삭제 가능. Flyway 마이그레이션에는 포함하지 않는다.
--
-- 사용법: psql -h localhost -d coming -v test_user_id={dev-login으로 발급받은 유저 id} -f seed.sql

BEGIN;

-- 1. 더미 아티스트 50,000건
-- ArtistRepository의 이름 검색(findByNameOrAliasContainingIgnoreCase)이 LIKE '%...%'(양쪽 와일드카드)라
-- 인덱스를 타지 못하고 항상 풀스캔이다. 이 비용은 행 수에 선형 비례하므로,
-- 검색 부하가 실제로 체감되는 지점을 보려면 절대 건수를 크게 잡아야 한다.
INSERT INTO artist (mbid, name, sort_name, is_coming)
SELECT
    gen_random_uuid()::text,
    '[LOADTEST] Artist ' || i,
    'LOADTEST Artist ' || i,
    (i % 10 = 0)
FROM generate_series(1, 50000) AS i;

-- 2. 더미 공연 50,000건 (concert.search도 동일하게 풀스캔 기반 검색)
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
FROM generate_series(1, 50000) AS i;

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

-- 4. 디스코그래피 30,000건 (ReleaseGroupRepository.searchReleases도 LIKE 기반 풀스캔 + 서브쿼리 조인)
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
FROM generate_series(1, 30000) AS i, loadtest_artists;

-- 5. 팔로우 200건 (concert-following.js / artist-detail 팔로우 흐름 테스트용 — dev-login으로 만든 테스트 유저 기준)
INSERT INTO user_follow_artist (user_id, artist_id, created_at)
SELECT :test_user_id, id, now()
FROM artist
WHERE name LIKE '[LOADTEST]%'
ORDER BY random()
LIMIT 200;

-- 6. 팔로우 관계 대량 스큐 시뮬레이션 (인기 아티스트에 팔로워가 몰리는 실제 패턴 재현)
-- user_follow_artist는 user_id FK 제약이 V2 마이그레이션에서 제거되어 실유저 행 없이도 삽입 가능하다.
-- ArtistService.getArtist()가 상세조회마다 countByArtistId를 호출하는데 artist_id에 인덱스가 없어
-- 테이블 전체 크기가 이 쿼리 비용을 직접 좌우한다. 가짜 user_id는 실 유저 시퀀스와 겹치지 않도록
-- 900000000 이상 오프셋을 사용한다 (FK 제약이 없어 실존하지 않아도 무방).

-- 6-1. 인기 아티스트 1건('[LOADTEST] Artist 1')에 팔로워 50,000명 집중
INSERT INTO user_follow_artist (user_id, artist_id, created_at)
SELECT 900000000 + i, (SELECT id FROM artist WHERE name = '[LOADTEST] Artist 1'), now()
FROM generate_series(1, 50000) AS i;

-- 6-2. 나머지 아티스트에 팔로우 150,000건 무작위 분산 (테이블 전체 크기 확보용)
WITH loadtest_artists AS (
    SELECT array_agg(id) AS ids FROM artist WHERE name LIKE '[LOADTEST]%'
)
INSERT INTO user_follow_artist (user_id, artist_id, created_at)
SELECT
    900050000 + i,
    loadtest_artists.ids[1 + floor(random() * array_length(loadtest_artists.ids, 1))::int],
    now()
FROM generate_series(1, 150000) AS i, loadtest_artists;

COMMIT;
