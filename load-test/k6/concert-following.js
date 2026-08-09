import http from 'k6/http';
import { check, sleep } from 'k6';
import { getOptions } from './lib/profiles.js';

export const options = getOptions();

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// local 프로파일 전용 DevAuthController(/api/dev/login)로 토큰을 발급받는다.
export function setup() {
  const res = http.post(
    `${BASE_URL}/api/dev/login`,
    JSON.stringify({ nickname: 'k6-load-test', role: 'USER' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, { 'dev login status is 200': (r) => r.status === 200 });
  return { accessToken: JSON.parse(res.body).accessToken };
}

export default function (data) {
  const res = http.get(`${BASE_URL}/api/concerts/following`, {
    headers: { Authorization: `Bearer ${data.accessToken}` },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
