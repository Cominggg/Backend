CREATE TABLE user_policy_agreement (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id bigint NOT NULL,
  policy_id bigint NOT NULL,
  agreed_at timestamp NOT NULL,
  UNIQUE (user_id, policy_id)
);

INSERT INTO policy_document (type, version, effective_date, change_summary, detail_url, requires_reconsent, created_at)
VALUES
  ('TERMS', 'legacy', '2020-01-01', '시스템 마이그레이션 이전 동의 이력 보존용 레코드입니다.', '', false, now()),
  ('PRIVACY', 'legacy', '2020-01-01', '시스템 마이그레이션 이전 동의 이력 보존용 레코드입니다.', '', false, now());

INSERT INTO user_policy_agreement (user_id, policy_id, agreed_at)
SELECT id, (SELECT id FROM policy_document WHERE type = 'TERMS' AND version = 'legacy'), agreed_at
FROM "user"
WHERE agreed_terms = true AND agreed_at IS NOT NULL;

INSERT INTO user_policy_agreement (user_id, policy_id, agreed_at)
SELECT id, (SELECT id FROM policy_document WHERE type = 'PRIVACY' AND version = 'legacy'), agreed_at
FROM "user"
WHERE agreed_privacy = true AND agreed_at IS NOT NULL;
