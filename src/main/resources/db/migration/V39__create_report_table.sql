CREATE TABLE report (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  reporter_id bigint NOT NULL,
  target_type varchar(20) NOT NULL,
  target_id bigint NOT NULL,
  reason varchar(20) NOT NULL,
  detail text,
  status varchar(20) NOT NULL,
  admin_note text,
  created_at timestamp,
  updated_at timestamp,
  UNIQUE (reporter_id, target_type, target_id)
);

CREATE INDEX idx_report_status ON report (status);
CREATE INDEX idx_report_target ON report (target_type, target_id);
