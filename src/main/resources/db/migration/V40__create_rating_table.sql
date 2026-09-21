CREATE TABLE rating (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id bigint NOT NULL,
  target_type varchar(20) NOT NULL,
  target_id bigint NOT NULL,
  score numeric(2,1) NOT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_rating_target ON rating (target_type, target_id);
