import http from 'k6/http';
import { check, sleep } from 'k6';
import { getOptions } from '../lib/profiles.js';
import { devLogin } from '../lib/auth.js';

export const options = getOptions();

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  return devLogin();
}

export default function (data) {
  const now = new Date();
  const res = http.get(
    `${BASE_URL}/api/calendar?year=${now.getFullYear()}&month=${now.getMonth() + 1}`,
    { headers: { Authorization: `Bearer ${data.accessToken}` } }
  );

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
