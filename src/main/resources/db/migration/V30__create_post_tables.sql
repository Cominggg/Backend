CREATE TABLE post (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id bigint NOT NULL,
  category varchar(20) NOT NULL,
  title varchar(255) NOT NULL,
  content jsonb NOT NULL,
  content_text text NOT NULL,
  recommend_count bigint NOT NULL DEFAULT 0,
  view_count bigint NOT NULL DEFAULT 0,
  created_at timestamp,
  updated_at timestamp
);

CREATE INDEX idx_post_user_id ON post (user_id);
CREATE INDEX idx_post_category ON post (category);

CREATE TABLE post_entity_tag (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  post_id bigint NOT NULL,
  entity_type varchar(20) NOT NULL,
  entity_id bigint NOT NULL,
  created_at timestamp
);

CREATE INDEX idx_post_entity_tag_post_id ON post_entity_tag (post_id);
CREATE INDEX idx_post_entity_tag_entity_type_entity_id ON post_entity_tag (entity_type, entity_id);

CREATE TABLE post_recommend (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id bigint NOT NULL,
  post_id bigint NOT NULL,
  created_at timestamp,
  UNIQUE (user_id, post_id)
);
