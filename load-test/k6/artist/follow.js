import http from 'k6/http';
import { check, sleep } from 'k6';
import { getOptions } from '../lib/profiles.js';
import { devLogin } from '../lib/auth.js';

export const options = getOptions();

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  return devLogin();
}

// VU마다 다른 페이지의 아티스트를 골라 팔로우→언팔로우를 짝지어 실행한다 (동시 팔로우 충돌 최소화).
export default function (data) {
  const headers = { Authorization: `Bearer ${data.accessToken}` };
  const listRes = http.get(`${BASE_URL}/api/artists?page=${__VU - 1}&size=25`, { headers });
  check(listRes, { 'list status is 200': (r) => r.status === 200 });

  const artistId = JSON.parse(listRes.body).content[0].id;

  const followRes = http.post(`${BASE_URL}/api/artists/${artistId}/follow`, null, { headers });
  check(followRes, { 'follow status is 200': (r) => r.status === 200 });

  const unfollowRes = http.del(`${BASE_URL}/api/artists/${artistId}/follow`, null, { headers });
  check(unfollowRes, { 'unfollow status is 200': (r) => r.status === 200 });

  sleep(1);
}
