CREATE TABLE comment (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  post_id bigint NOT NULL,
  user_id bigint NOT NULL,
  parent_comment_id bigint,
  content text NOT NULL,
  like_count bigint NOT NULL DEFAULT 0,
  is_deleted boolean NOT NULL DEFAULT false,
  created_at timestamp
);

CREATE INDEX idx_comment_post_id ON comment (post_id);
CREATE INDEX idx_comment_parent_comment_id ON comment (parent_comment_id);

CREATE TABLE comment_like (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id bigint NOT NULL,
  comment_id bigint NOT NULL,
  created_at timestamp,
  UNIQUE (user_id, comment_id)
);

ALTER TABLE post ADD COLUMN comment_count bigint NOT NULL DEFAULT 0;
