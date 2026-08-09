import http from 'k6/http';
import { check, sleep } from 'k6';
import { getOptions } from '../lib/profiles.js';

export const options = getOptions();

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const res = http.get(`${BASE_URL}/api/concerts?page=0&size=20`);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'has content field': (r) => JSON.parse(r.body).content !== undefined,
  });

  sleep(1);
}
