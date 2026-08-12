import http from 'k6/http';
import { check, sleep } from 'k6';
import { getOptions } from '../lib/profiles.js';

export const options = getOptions();

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const listRes = http.get(`${BASE_URL}/api/releases?page=0&size=1`);
  check(listRes, { 'list status is 200': (r) => r.status === 200 });

  const releaseId = JSON.parse(listRes.body).content[0].id;
  const detailRes = http.get(`${BASE_URL}/api/releases/${releaseId}`);

  check(detailRes, {
    'detail status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
