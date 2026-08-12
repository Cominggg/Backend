// k6 실행 시 -e PROFILE=smoke|load|stress 로 부하 강도를 선택한다. 기본값은 load.

const STAGES = {
  smoke: {
    vus: 1,
    iterations: 1,
  },
  load: {
    stages: [
      { duration: '30s', target: 20 },
      { duration: '1m', target: 20 },
      { duration: '30s', target: 0 },
    ],
  },
  stress: {
    stages: [
      { duration: '30s', target: 50 },
      { duration: '1m', target: 50 },
      { duration: '30s', target: 150 },
      { duration: '2m', target: 150 },
      { duration: '30s', target: 0 },
    ],
  },
};

const THRESHOLDS = {
  smoke: {
    http_req_failed: ['rate<0.01'],
  },
  load: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
  // stress는 통과/실패가 목적이 아니라 몇 VU에서 무너지는지 관찰하는 것이 목적이라 임계값을 두지 않는다.
  stress: {},
};

export function getOptions() {
  const profile = __ENV.PROFILE || 'load';
  if (!STAGES[profile]) {
    throw new Error(`알 수 없는 PROFILE: ${profile} (smoke | load | stress 중 하나)`);
  }

  return {
    ...STAGES[profile],
    thresholds: THRESHOLDS[profile],
  };
}
