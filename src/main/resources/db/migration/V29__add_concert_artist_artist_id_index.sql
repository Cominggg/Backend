-- flyway:executeInTransaction=false
-- CONCURRENTLY는 다른 세션의 트랜잭션이 끝날 때까지 무기한 대기할 수 있다.
-- 무한 대기로 배포/마이그레이션이 멈추지 않도록 상한을 둔다.
SET statement_timeout = '300s';
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_concert_artist_artist_id ON concert_artist (artist_id);
