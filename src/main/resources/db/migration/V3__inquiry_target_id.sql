ALTER TABLE inquiry ADD COLUMN target_id bigint;
UPDATE inquiry SET target_id = COALESCE(concert_id, artist_id);
ALTER TABLE inquiry ALTER COLUMN target_id SET NOT NULL;
ALTER TABLE inquiry DROP COLUMN concert_id;
ALTER TABLE inquiry DROP COLUMN artist_id;
