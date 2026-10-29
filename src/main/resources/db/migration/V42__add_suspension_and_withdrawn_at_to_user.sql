-- status가 SUSPENDED일 때 suspended_until이 NULL이면 영구 정지
ALTER TABLE "user"
    ADD COLUMN suspended_until   TIMESTAMP   NULL,
    ADD COLUMN suspension_reason VARCHAR(20) NULL,
    ADD COLUMN suspension_detail TEXT        NULL,
    ADD COLUMN withdrawn_at      TIMESTAMP   NULL;
