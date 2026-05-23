CREATE TABLE concert_status_log (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  concert_id bigint NOT NULL REFERENCES concert(id),
  before_status varchar(20) NOT NULL,
  after_status varchar(20) NOT NULL,
  reason text,
  changed_at timestamp NOT NULL
);
