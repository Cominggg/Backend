TRUNCATE TABLE
  concert_status_log,
  setlist_track,
  setlist,
  concert_booking_link,
  concert_artist,
  user_concert_calendar,
  user_follow_artist,
  track,
  release_group,
  artist_alias,
  artist_url,
  inquiry,
  concert,
  artist,
  "user"
RESTART IDENTITY CASCADE;

ALTER TABLE artist DROP COLUMN debut_date;
ALTER TABLE artist_alias DROP COLUMN is_learned;
ALTER TABLE release_group DROP COLUMN created_at;
ALTER TABLE release_group DROP COLUMN updated_at;
