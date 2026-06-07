CREATE TABLE concert_artist_candidate (
    id         BIGSERIAL PRIMARY KEY,
    concert_id BIGINT NOT NULL REFERENCES concert (id),
    artist_id  BIGINT NOT NULL REFERENCES artist (id),
    matched_by TEXT   NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (concert_id, artist_id)
);
