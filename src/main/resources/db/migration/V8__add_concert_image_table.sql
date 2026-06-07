CREATE TABLE concert_image (
  id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  concert_id bigint NOT NULL REFERENCES concert(id),
  url        text   NOT NULL,
  position   int    NOT NULL DEFAULT 0
);
