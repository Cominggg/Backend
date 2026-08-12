import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// local 프로파일 전용 DevAuthController(/api/dev/login)로 토큰을 발급받는다.
export function devLogin(nickname = 'k6-load-test', role = 'USER') {
  const res = http.post(
    `${BASE_URL}/api/dev/login`,
    JSON.stringify({ nickname, role }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, { 'dev login status is 200': (r) => r.status === 200 });
  return { accessToken: JSON.parse(res.body).accessToken };
}
