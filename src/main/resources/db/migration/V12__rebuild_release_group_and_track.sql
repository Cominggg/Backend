-- 기존 데이터 전체 삭제 (참조 순서)
TRUNCATE TABLE setlist_track;
TRUNCATE TABLE setlist;
DROP TABLE track;
DROP TABLE release_group;

-- release_group 재생성
CREATE TABLE release_group (
    id                 bigint       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mbid               varchar(36)  UNIQUE,
    spotify_id         varchar(22)  UNIQUE,
    artist_id          bigint       NOT NULL,
    title              varchar(500) NOT NULL,
    type               varchar(20),
    first_release_date date,
    cover_url          text,
    label              varchar(255),
    total_tracks       int
);

-- track 재생성
CREATE TABLE track (
    id               bigint       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    release_group_id bigint       NOT NULL,
    mbid             varchar(36)  UNIQUE,
    spotify_id       varchar(22)  UNIQUE,
    title            varchar(500) NOT NULL,
    position         int          NOT NULL,
    length_ms        int,
    disc_number      int,
    explicit         boolean
);
