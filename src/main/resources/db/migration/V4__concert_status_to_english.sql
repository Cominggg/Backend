UPDATE concert SET status = 'UPCOMING'  WHERE status = '공연예정';
UPDATE concert SET status = 'ONGOING'   WHERE status = '공연중';
UPDATE concert SET status = 'ENDED'     WHERE status = '공연완료';
UPDATE concert SET status = 'CANCELLED' WHERE status = '공연취소';
