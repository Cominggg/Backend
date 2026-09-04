CREATE INDEX idx_user_follow_artist_artist_id ON user_follow_artist (artist_id);

CREATE INDEX idx_release_group_artist_id ON release_group (artist_id);

CREATE INDEX idx_concert_date_range ON concert (start_date, end_date, status);

CREATE INDEX idx_setlist_concert_id ON setlist (concert_id);

CREATE INDEX idx_setlist_track_setlist_id ON setlist_track (setlist_id);

CREATE INDEX idx_track_release_group_id ON track (release_group_id);

CREATE INDEX idx_inquiry_user_id ON inquiry (user_id);

CREATE INDEX idx_inquiry_target_id ON inquiry (target_id);
