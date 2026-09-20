CREATE TABLE notice (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id bigint NOT NULL,
  title varchar(255) NOT NULL,
  content text NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamp,
  updated_at timestamp
);

CREATE INDEX idx_notice_active_created_at ON notice (active, created_at);
