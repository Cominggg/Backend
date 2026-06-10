ALTER TABLE artist DROP COLUMN created_at;
ALTER TABLE artist DROP COLUMN updated_at;

ALTER TABLE artist_alias DROP COLUMN created_at;

ALTER TABLE concert DROP COLUMN created_at;
ALTER TABLE concert DROP COLUMN updated_at;

ALTER TABLE concert_artist DROP COLUMN created_at;

ALTER TABLE concert_artist_candidate DROP COLUMN created_at;
