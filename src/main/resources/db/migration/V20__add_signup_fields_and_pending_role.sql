ALTER TABLE "user" DROP COLUMN IF EXISTS profile_image_url;

ALTER TABLE "user" ALTER COLUMN nickname DROP NOT NULL;

CREATE UNIQUE INDEX user_nickname_unique ON "user"(nickname) WHERE nickname IS NOT NULL;

ALTER TABLE "user" ADD COLUMN birth_year INTEGER;
ALTER TABLE "user" ADD COLUMN agreed_terms BOOLEAN;
ALTER TABLE "user" ADD COLUMN agreed_privacy BOOLEAN;
ALTER TABLE "user" ADD COLUMN agreed_marketing BOOLEAN;
ALTER TABLE "user" ADD COLUMN agreed_at TIMESTAMP;

UPDATE "user" SET role = 'PENDING' WHERE status = 'ACTIVE';
