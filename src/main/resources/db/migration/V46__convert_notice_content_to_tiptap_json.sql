-- 공지사항 본문을 평문(text)에서 게시글과 같은 Tiptap JSON(jsonb)으로 바꾼다.
-- 기존 평문은 줄마다 문단 하나로 옮긴다. 빈 줄은 빈 문단(content 없음)이 된다(Tiptap은 빈 text 노드를 허용하지 않는다).
-- ALTER COLUMN ... USING에는 하위 쿼리를 쓸 수 없어 새 컬럼에 채운 뒤 바꿔 끼운다.
ALTER TABLE notice ADD COLUMN content_json jsonb;

UPDATE notice n
SET content_json = jsonb_build_object(
        'type', 'doc',
        'content', (
            SELECT jsonb_agg(
                       CASE
                           WHEN line = '' THEN jsonb_build_object('type', 'paragraph')
                           ELSE jsonb_build_object(
                                   'type', 'paragraph',
                                   'content', jsonb_build_array(jsonb_build_object('type', 'text', 'text', line)))
                       END
                       ORDER BY ord)
            FROM regexp_split_to_table(replace(n.content, E'\r', ''), E'\n') WITH ORDINALITY AS t(line, ord)
        ));

ALTER TABLE notice ALTER COLUMN content_json SET NOT NULL;
ALTER TABLE notice DROP COLUMN content;
ALTER TABLE notice RENAME COLUMN content_json TO content;
