// OrderFlow load test (k6)
//
// दो काम एक साथ चलते हैं:
//   browse   : products की list देखना         (पढ़ने वाला हल्का काम)
//   checkout : order लगाना, फिर तब तक पूछते रहना जब तक workflow उसे COMPLETED न कर दे
//              (यानी पूरा सफ़र: API -> Temporal -> stock -> payment -> invoice -> email)
//
// चलाने से पहले:
//   1. पूरा system चालू हो, और payment-service का chaos बंद हो:  PUT http://localhost:8081/api/chaos {"failureRate":0,"slowRate":0}
//   2. एक ADMIN user हो (register करके database में role ADMIN किया हुआ)
//
// Smoke (सब ठीक है या नहीं, 10 सेकंड में):
//   k6 run -e SMOKE=1 -e ADMIN_EMAIL=admin@example.com -e ADMIN_PASSWORD=password123 k6/order-flow.js
// असली run:
//   k6 run -e ADMIN_EMAIL=admin@example.com -e ADMIN_PASSWORD=password123 k6/order-flow.js
//
// सेटिंग (-e NAAM=value): BASE_URL, USERS, DURATION, BROWSE_RATE, ORDER_RATE, SMOKE

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import exec from 'k6/execution';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL;
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD;
const USERS = parseInt(__ENV.USERS || '20', 10);          // कितने अलग customers
const DURATION = __ENV.DURATION || '1m';
const BROWSE_RATE = parseInt(__ENV.BROWSE_RATE || '50', 10);   // हर सेकंड कितनी बार list देखी जाए
const ORDER_RATE = parseInt(__ENV.ORDER_RATE || '5', 10);      // हर सेकंड कितने नए orders
const SMOKE = __ENV.SMOKE === '1';

const TERMINAL = ['COMPLETED', 'CANCELLED', 'FAILED'];
const JSON_HEADERS = { 'Content-Type': 'application/json' };

// हमारे अपने मापदंड
const ordersCompleted = new Counter('orders_completed');
const ordersNotCompleted = new Counter('orders_not_completed');   // रद्द, फेल, या समय से पूरे नहीं हुए
const orderCompletionMs = new Trend('order_completion_ms', true); // order लगाने से COMPLETED दिखने तक

export const options = SMOKE
  ? {
      scenarios: {
        browse: { executor: 'shared-iterations', vus: 1, iterations: 3, exec: 'browse' },
        checkout: { executor: 'shared-iterations', vus: 1, iterations: 2, exec: 'checkout' },
      },
      thresholds: { checks: ['rate>0.99'] },
      summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    }
  : {
      scenarios: {
        browse: {
          executor: 'constant-arrival-rate',
          rate: BROWSE_RATE, timeUnit: '1s', duration: DURATION,
          preAllocatedVUs: 20, maxVUs: 200, exec: 'browse',
        },
        checkout: {
          executor: 'constant-arrival-rate',
          rate: ORDER_RATE, timeUnit: '1s', duration: DURATION,
          preAllocatedVUs: 30, maxVUs: 300, exec: 'checkout',
        },
      },
      // ये "लक्ष्य" हैं, गारंटी नहीं। laptop पर कोई फेल हो तो उसे छिपाएँ नहीं, असली संख्या README में लिखें।
      thresholds: {
        http_req_failed: ['rate<0.01'],
        'http_req_duration{name:browse_products}': ['p(95)<300'],
        'http_req_duration{name:create_order}': ['p(95)<800'],
        order_completion_ms: ['p(95)<10000'],
        checks: ['rate>0.99'],
      },
      summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    };

function authHeaders(token, extra) {
  return Object.assign({}, JSON_HEADERS, { Authorization: `Bearer ${token}` }, extra || {});
}

function login(email, password) {
  const res = http.post(`${BASE_URL}/api/auth/login`,
    JSON.stringify({ email, password }), { headers: JSON_HEADERS });
  return res.status === 200 ? res.json('data.token') : null;
}

// एक बार चलता है, नापे जाने वाले समय में नहीं गिना जाता (बहुत सारे bcrypt वाले register यहीं निपट जाते हैं)
export function setup() {
  if (!ADMIN_EMAIL || !ADMIN_PASSWORD) {
    exec.test.abort('ADMIN_EMAIL और ADMIN_PASSWORD दीजिए (-e ADMIN_EMAIL=... -e ADMIN_PASSWORD=...)');
  }
  const adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
  if (!adminToken) {
    exec.test.abort('Admin login नहीं हुआ। user register करके database में role ADMIN किया है?');
  }

  const run = Date.now();
  // बहुत बड़ा stock, ताकि load test के दौरान माल ख़त्म न हो और हम stock की नहीं, system की गति नापें
  const product = http.post(`${BASE_URL}/api/admin/products`,
    JSON.stringify({ name: `Load-${run}`, description: 'k6', price: 100, stock: 10000000 }),
    { headers: authHeaders(adminToken) });
  if (product.status !== 201) {
    exec.test.abort(`Product नहीं बना (status ${product.status}). admin का token ADMIN role वाला है?`);
  }

  const tokens = [];
  for (let i = 0; i < USERS; i++) {
    const email = `k6-${run}-${i}@load.test`;
    http.post(`${BASE_URL}/api/auth/register`,
      JSON.stringify({ email, password: 'password123' }), { headers: JSON_HEADERS });
    const token = login(email, 'password123');
    if (!token) exec.test.abort(`Customer ${email} का login नहीं हुआ`);
    tokens.push(token);
  }
  return { productId: product.json('data.id'), tokens };
}

export function browse() {
  const res = http.get(`${BASE_URL}/api/products?page=0&size=10`, { tags: { name: 'browse_products' } });
  check(res, { 'products list 200': (r) => r.status === 200 });
}

export function checkout(data) {
  const token = data.tokens[(exec.vu.idInTest - 1) % data.tokens.length];
  const key = `k6-${exec.vu.idInTest}-${exec.vu.iterationInScenario}-${Date.now()}`;   // हर order की अपनी key
  const started = Date.now();

  const res = http.post(`${BASE_URL}/api/orders`,
    JSON.stringify({ items: [{ productId: data.productId, quantity: 1 }] }),
    { headers: authHeaders(token, { 'Idempotency-Key': key }), tags: { name: 'create_order' } });

  if (!check(res, { 'order created (201)': (r) => r.status === 201 })) {
    ordersNotCompleted.add(1);
    return;
  }
  const orderId = res.json('data.id');

  // workflow पीछे चलता है; नतीजे के लिए पूछते रहो (हर 0.5 सेकंड, अधिकतम 30 सेकंड)
  let status = 'PENDING';
  for (let i = 0; i < 60 && !TERMINAL.includes(status); i++) {
    sleep(0.5);
    const r = http.get(`${BASE_URL}/api/orders/${orderId}`,
      { headers: authHeaders(token), tags: { name: 'get_order' } });
    if (r.status === 200) status = r.json('data.status');
  }

  const ok = check(status, { 'order reached COMPLETED': (s) => s === 'COMPLETED' });
  if (ok) {
    ordersCompleted.add(1);
    orderCompletionMs.add(Date.now() - started);
  } else {
    ordersNotCompleted.add(1);
  }
}

// ---------- अंत में README में चिपकाने लायक तालिका ----------

function val(data, metric, stat) {
  const m = data.metrics[metric];
  return m && m.values && m.values[stat] !== undefined ? m.values[stat] : null;
}
const ms = (v) => (v === null ? 'n/a' : `${v.toFixed(0)} ms`);
const num = (v, d) => (v === null ? 'n/a' : v.toFixed(d));

function markdownSummary(data) {
  const seconds = data.state.testRunDurationMs / 1000;
  const completed = val(data, 'orders_completed', 'count') || 0;
  const notCompleted = val(data, 'orders_not_completed', 'count') || 0;
  const failedRate = val(data, 'http_req_failed', 'rate');
  const lines = [
    '| Metric | Value |',
    '|---|---|',
    `| Test duration | ${num(seconds, 0)} s |`,
    `| Load | browse ${BROWSE_RATE}/s + new orders ${ORDER_RATE}/s (${USERS} customers) |`,
    `| Total HTTP requests | ${num(val(data, 'http_reqs', 'count'), 0)} (${num(val(data, 'http_reqs', 'rate'), 1)} req/s) |`,
    `| Failed HTTP requests | ${failedRate === null ? 'n/a' : (failedRate * 100).toFixed(2) + ' %'} |`,
    `| Browse products: p50 / p95 / p99 | ${ms(val(data, 'http_req_duration{name:browse_products}', 'med'))} / ${ms(val(data, 'http_req_duration{name:browse_products}', 'p(95)'))} / ${ms(val(data, 'http_req_duration{name:browse_products}', 'p(99)'))} |`,
    `| Create order (API response): p50 / p95 / p99 | ${ms(val(data, 'http_req_duration{name:create_order}', 'med'))} / ${ms(val(data, 'http_req_duration{name:create_order}', 'p(95)'))} / ${ms(val(data, 'http_req_duration{name:create_order}', 'p(99)'))} |`,
    `| Orders fully completed | ${completed} (${num(completed / seconds, 2)}/s) |`,
    `| Orders not completed | ${notCompleted} |`,
    `| Order end-to-end (create to COMPLETED): p50 / p95 / p99 | ${ms(val(data, 'order_completion_ms', 'med'))} / ${ms(val(data, 'order_completion_ms', 'p(95)'))} / ${ms(val(data, 'order_completion_ms', 'p(99)'))} |`,
    `| Checks passed | ${val(data, 'checks', 'rate') === null ? 'n/a' : (val(data, 'checks', 'rate') * 100).toFixed(2) + ' %'} |`,
    '',
    '_End-to-end time is measured by polling every 0.5 s, so it is accurate to about half a second._',
  ];
  return lines.join('\n') + '\n';
}

export function handleSummary(data) {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    'k6/results/summary.json': JSON.stringify(data, null, 2),
    'k6/results/summary.md': markdownSummary(data),
  };
}
