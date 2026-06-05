-- 기존 (artist_id, url) 유니크 제약 제거
ALTER TABLE artist_url DROP CONSTRAINT artist_url_artist_id_url_key;

-- 중복 제거: (artist_id, type) 기준으로 id가 가장 작은 것만 남김
DELETE FROM artist_url
WHERE id NOT IN (
    SELECT MIN(id)
    FROM artist_url
    GROUP BY artist_id, type
);

-- (artist_id, type) 유니크 제약 추가
ALTER TABLE artist_url ADD CONSTRAINT uq_artist_url_type UNIQUE (artist_id, type);
