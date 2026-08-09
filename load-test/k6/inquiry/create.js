import http from 'k6/http';
import { check, sleep } from 'k6';
import { getOptions } from '../lib/profiles.js';
import { devLogin } from '../lib/auth.js';

export const options = getOptions();

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  return devLogin();
}

// FEEDBACK은 targetId 없이 등록 가능하고 중복(PENDING) 체크 대상이 아니라 반복 생성에 안전하다.
// title에 [LOADTEST] 마커를 붙여 cleanup.sql에서 정리한다.
export default function (data) {
  const res = http.post(
    `${BASE_URL}/api/inquiries`,
    JSON.stringify({
      type: 'FEEDBACK',
      title: '[LOADTEST] 부하테스트 문의',
      content: 'k6 부하테스트로 생성된 더미 문의입니다.',
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${data.accessToken}`,
      },
    }
  );

  check(res, {
    'status is 201': (r) => r.status === 201,
  });

  sleep(1);
}
