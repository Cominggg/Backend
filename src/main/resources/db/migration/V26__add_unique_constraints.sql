ALTER TABLE "user" ADD CONSTRAINT uq_user_provider_provider_id UNIQUE (provider, provider_id);

ALTER TABLE user_follow_artist ADD CONSTRAINT uq_user_follow_artist_user_id_artist_id UNIQUE (user_id, artist_id);

ALTER TABLE user_concert_calendar ADD CONSTRAINT uq_user_concert_calendar_user_id_concert_id UNIQUE (user_id, concert_id);
