UPDATE concert SET status = 'UPCOMING'  WHERE status = '공연예정';
UPDATE concert SET status = 'ONGOING'   WHERE status = '공연중';
UPDATE concert SET status = 'ENDED'     WHERE status = '공연완료';
UPDATE concert SET status = 'CANCELLED' WHERE status = '공연취소';

UPDATE concert_status_log SET before_status = 'UPCOMING'  WHERE before_status = '공연예정';
UPDATE concert_status_log SET before_status = 'ONGOING'   WHERE before_status = '공연중';
UPDATE concert_status_log SET before_status = 'ENDED'     WHERE before_status = '공연완료';
UPDATE concert_status_log SET before_status = 'CANCELLED' WHERE before_status = '공연취소';

UPDATE concert_status_log SET after_status = 'UPCOMING'   WHERE after_status = '공연예정';
UPDATE concert_status_log SET after_status = 'ONGOING'    WHERE after_status = '공연중';
UPDATE concert_status_log SET after_status = 'ENDED'      WHERE after_status = '공연완료';
UPDATE concert_status_log SET after_status = 'CANCELLED'  WHERE after_status = '공연취소';
