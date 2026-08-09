import http from 'k6/http';
import { check, sleep } from 'k6';
import { getOptions } from '../lib/profiles.js';
import { devLogin } from '../lib/auth.js';

export const options = getOptions();

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  return devLogin();
}

// VU마다 다른 페이지의 공연을 골라 캘린더 등록→해제를 짝지어 실행한다 (동시 등록 충돌 최소화).
export default function (data) {
  const headers = { Authorization: `Bearer ${data.accessToken}` };
  const listRes = http.get(`${BASE_URL}/api/concerts?page=${__VU - 1}&size=20`, { headers });
  check(listRes, { 'list status is 200': (r) => r.status === 200 });

  const concertId = JSON.parse(listRes.body).content[0].id;

  const addRes = http.post(`${BASE_URL}/api/calendar/${concertId}`, null, { headers });
  check(addRes, { 'add status is 200': (r) => r.status === 200 });

  const removeRes = http.del(`${BASE_URL}/api/calendar/${concertId}`, null, { headers });
  check(removeRes, { 'remove status is 200': (r) => r.status === 200 });

  sleep(1);
}
