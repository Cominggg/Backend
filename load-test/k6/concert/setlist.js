import http from 'k6/http';
import { check, sleep } from 'k6';
import { getOptions } from '../lib/profiles.js';

export const options = getOptions();

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const listRes = http.get(`${BASE_URL}/api/concerts?page=0&size=1`);
  check(listRes, { 'list status is 200': (r) => r.status === 200 });

  const concertId = JSON.parse(listRes.body).content[0].id;
  const setlistRes = http.get(`${BASE_URL}/api/concerts/${concertId}/setlist`);

  check(setlistRes, {
    'setlist status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
