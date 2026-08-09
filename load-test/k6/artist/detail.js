import http from 'k6/http';
import { check, sleep } from 'k6';
import { getOptions } from '../lib/profiles.js';

export const options = getOptions();

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const listRes = http.get(`${BASE_URL}/api/artists?page=0&size=1`);
  check(listRes, { 'list status is 200': (r) => r.status === 200 });

  const artists = JSON.parse(listRes.body).content;
  if (artists && artists.length > 0) {
    const artistId = artists[0].id;

    const detailRes = http.get(`${BASE_URL}/api/artists/${artistId}`);
    check(detailRes, { 'detail status is 200': (r) => r.status === 200 });

    const releasesRes = http.get(`${BASE_URL}/api/artists/${artistId}/releases`);
    check(releasesRes, { 'releases status is 200': (r) => r.status === 200 });

    const concertsRes = http.get(`${BASE_URL}/api/artists/${artistId}/concerts`);
    check(concertsRes, { 'concerts status is 200': (r) => r.status === 200 });
  }

  sleep(1);
}
