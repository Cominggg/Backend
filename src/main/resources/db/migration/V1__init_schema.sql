CREATE TABLE "user" (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  provider varchar(20) NOT NULL,
  provider_id varchar(255) NOT NULL,
  nickname varchar(50) NOT NULL,
  profile_image_url text,
  role varchar(20) NOT NULL,
  status varchar(20) NOT NULL,
  created_at timestamp,
  updated_at timestamp
);

CREATE TABLE artist (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  mbid varchar(36) NOT NULL UNIQUE,
  name varchar(255) NOT NULL,
  sort_name varchar(255),
  debut_date date,
  is_coming boolean NOT NULL DEFAULT false,
  created_at timestamp,
  updated_at timestamp
);

CREATE TABLE artist_alias (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  artist_id bigint NOT NULL REFERENCES artist(id),
  name varchar(255) NOT NULL,
  locale varchar(10),
  is_learned boolean NOT NULL DEFAULT false,
  created_at timestamp,
  UNIQUE (artist_id, name)
);

CREATE TABLE artist_url (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  artist_id bigint NOT NULL REFERENCES artist(id),
  type varchar(100) NOT NULL,
  url text NOT NULL,
  UNIQUE (artist_id, url)
);

CREATE TABLE user_follow_artist (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES "user"(id),
  artist_id bigint NOT NULL REFERENCES artist(id),
  created_at timestamp
);

CREATE TABLE concert (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  kopis_id varchar(50) NOT NULL UNIQUE,
  title varchar(500) NOT NULL,
  "cast" text,
  start_date date NOT NULL,
  end_date date NOT NULL,
  venue_name varchar(255) NOT NULL,
  venue_address varchar(500),
  poster_url text,
  price text,
  status varchar(20) NOT NULL,
  view_count bigint NOT NULL DEFAULT 0,
  kopis_update_date date NOT NULL,
  created_at timestamp,
  updated_at timestamp
);

CREATE TABLE concert_booking_link (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  concert_id bigint NOT NULL REFERENCES concert(id),
  name varchar(100) NOT NULL,
  url text NOT NULL,
  UNIQUE (concert_id, url)
);

CREATE TABLE concert_artist (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  concert_id bigint NOT NULL REFERENCES concert(id),
  artist_id bigint NOT NULL REFERENCES artist(id),
  confidence varchar(10) NOT NULL,
  matched_by varchar(20) NOT NULL,
  created_at timestamp,
  UNIQUE (concert_id, artist_id)
);

CREATE TABLE user_concert_calendar (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES "user"(id),
  concert_id bigint NOT NULL REFERENCES concert(id),
  created_at timestamp
);

CREATE TABLE setlist (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  concert_id bigint NOT NULL REFERENCES concert(id),
  setlist_fm_id varchar(50) NOT NULL UNIQUE,
  collected_at timestamp NOT NULL
);

CREATE TABLE setlist_track (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  setlist_id bigint NOT NULL REFERENCES setlist(id),
  position int NOT NULL,
  song_name varchar(255) NOT NULL,
  info text
);

CREATE TABLE release_group (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  mbid varchar(36) NOT NULL UNIQUE,
  artist_id bigint NOT NULL REFERENCES artist(id),
  title varchar(500) NOT NULL,
  type varchar(20),
  first_release_date date,
  cover_url text,
  label varchar(255),
  created_at timestamp,
  updated_at timestamp
);

CREATE TABLE track (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  release_group_id bigint NOT NULL REFERENCES release_group(id),
  mbid varchar(36) NOT NULL UNIQUE,
  title varchar(500) NOT NULL,
  position int NOT NULL,
  length_ms int
);

CREATE TABLE inquiry (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES "user"(id),
  type varchar(20) NOT NULL,
  concert_id bigint REFERENCES concert(id),
  artist_id bigint REFERENCES artist(id),
  title varchar(255) NOT NULL,
  content text NOT NULL,
  status varchar(20) NOT NULL,
  admin_note text,
  created_at timestamp,
  updated_at timestamp
);
