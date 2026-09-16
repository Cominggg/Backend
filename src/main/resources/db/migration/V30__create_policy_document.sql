CREATE TABLE policy_document (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  type varchar(20) NOT NULL,
  version varchar(50) NOT NULL,
  effective_date date NOT NULL,
  change_summary text NOT NULL,
  detail_url varchar(500) NOT NULL,
  requires_reconsent boolean NOT NULL DEFAULT false,
  created_at timestamp,
  UNIQUE (type, version)
);
