import http from 'k6/http';
import { check, sleep } from 'k6';
import { getOptions } from '../lib/profiles.js';

export const options = getOptions();

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
// run.sh가 시딩 직후 팔로워 50,000명이 몰린 더미 아티스트('[LOADTEST] Artist 1')의 id를 넘겨준다.
// countByArtistId가 artist_id 인덱스 없이 풀스캔이라, 상세조회 비용에 팔로워 테이블 크기가 직결된다.
const HOT_ARTIST_ID = __ENV.HOT_ARTIST_ID;

export default function () {
  let artistId = HOT_ARTIST_ID;

  if (!artistId) {
    const listRes = http.get(`${BASE_URL}/api/artists?page=0&size=1`);
    check(listRes, { 'list status is 200': (r) => r.status === 200 });
    const artists = JSON.parse(listRes.body).content;
    artistId = artists && artists.length > 0 ? artists[0].id : null;
  }

  if (artistId) {
    const detailRes = http.get(`${BASE_URL}/api/artists/${artistId}`);
    check(detailRes, { 'detail status is 200': (r) => r.status === 200 });

    const releasesRes = http.get(`${BASE_URL}/api/artists/${artistId}/releases`);
    check(releasesRes, { 'releases status is 200': (r) => r.status === 200 });

    const concertsRes = http.get(`${BASE_URL}/api/artists/${artistId}/concerts`);
    check(concertsRes, { 'concerts status is 200': (r) => r.status === 200 });
  }

  sleep(1);
}
