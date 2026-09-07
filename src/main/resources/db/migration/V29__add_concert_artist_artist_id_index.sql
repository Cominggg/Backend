-- flyway:executeInTransaction=false
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_concert_artist_artist_id ON concert_artist (artist_id);
