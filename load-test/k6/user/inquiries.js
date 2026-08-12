import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { getOptions } from '../lib/profiles.js';
import { devLogin } from '../lib/auth.js';

export const options = getOptions();

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  return devLogin();
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.accessToken}` };

  group('내 문의 목록', () => {
    const res = http.get(`${BASE_URL}/api/me/inquiries`, { headers });
    check(res, { 'list status is 200': (r) => r.status === 200 });
  });

  group('PENDING 문의 존재 여부', () => {
    const res = http.get(`${BASE_URL}/api/me/inquiries/exists?type=CONCERT&targetId=1`, { headers });
    check(res, { 'exists status is 200': (r) => r.status === 200 });
  });

  sleep(1);
}
