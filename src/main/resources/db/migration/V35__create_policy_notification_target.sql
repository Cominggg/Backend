CREATE TABLE policy_notification_target (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  policy_id bigint NOT NULL,
  user_id bigint NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'PENDING',
  sent_at timestamp,
  retry_count integer NOT NULL DEFAULT 0,
  created_at timestamp,
  UNIQUE (policy_id, user_id)
);
