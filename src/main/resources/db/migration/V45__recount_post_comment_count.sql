-- commentCount를 삭제되지 않은 댓글·답글 수로 바꾼다. 지금까지는 소프트 삭제 때 줄지 않아 실제보다 많다.
UPDATE post p
SET comment_count = (SELECT COUNT(*) FROM comment c WHERE c.post_id = p.id AND c.is_deleted = false);
