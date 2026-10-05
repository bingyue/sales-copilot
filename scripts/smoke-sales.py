#!/usr/bin/env python3
"""Exercise a disposable local sales record. No credentials or customer text are printed."""
import json
import os
import pathlib
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone

ROOT = pathlib.Path(__file__).resolve().parents[1]
config = dict(line.split('=', 1) for line in (ROOT / 'deploy/.env').read_text().splitlines()
              if line and not line.startswith('#'))
base = os.environ.get('SALES_TEST_URL', f"http://127.0.0.1:{config.get('HTTP_PORT', '8088')}") + '/api'
client = urllib.request.build_opener(urllib.request.ProxyHandler({}))
token = ''
def request(method, path, body=None, expected=200):
    req = urllib.request.Request(base + path, method=method,
        data=None if body is None else json.dumps(body).encode(),
        headers={'Content-Type': 'application/json', **({'Authorization': 'Bearer ' + token} if token else {})})
    result = json.load(client.open(req, timeout=120))
    assert result['code'] == expected, (path, result.get('code'), result.get('msg'))
    return result.get('data')
def key(): return str(uuid.uuid4())
def now(): return datetime.now(timezone.utc).isoformat()

request('GET', '/sales/metrics', expected=401)
auth = request('POST', '/iYqueSys/login', {'username': config['ADMIN_USERNAME'], 'password': config['ADMIN_PASSWORD']})
token = auth['token']
baseline = request('GET', '/sales/metrics')
lead = request('POST', '/sales/leads', {'name': '自动验收客户-' + key()[:8], 'source': '测试', 'ownerUserId': '验收'})
lid = lead['id']
activity = {'role': 'CUSTOMER', 'content': '我想了解服务，主要希望解决客户跟进遗漏的问题。预算尚未确定。', 'occurredAt': now(), 'requestKey': key()}
request('POST', f'/sales/leads/{lid}/activities', activity)
request('POST', f'/sales/leads/{lid}/activities', activity)
detail = request('GET', f'/sales/leads/{lid}')
assert len([a for a in detail['activities'] if a['type'] == 'CONVERSATION']) == 1
task_body = {'leadId': lid, 'action': '确认需要解决的跟进问题', 'reason': '测试任务', 'dueAt': (datetime.now(timezone.utc) + timedelta(days=1)).isoformat(), 'requestKey': key()}
task = request('POST', '/sales/followups', task_body)
assert request('POST', '/sales/followups', task_body)['id'] == task['id']
receipt_body = {'productId': '', 'amount': 1.23, 'paidAt': now(), 'requestKey': key()}
receipt = request('POST', f'/sales/leads/{lid}/receipts', receipt_body)
assert request('POST', f'/sales/leads/{lid}/receipts', receipt_body)['id'] == receipt['id']
detail = request('GET', f'/sales/leads/{lid}')
assert detail['lead']['stage'] == 'WON' and detail['tasks'][0]['status'] == 'CANCELLED'
request('POST', f"/sales/receipts/{receipt['id']}/void", {'reason': '自动验收结束，作废测试金额', 'stage': 'LOST'})
detail = request('GET', f'/sales/leads/{lid}')
assert detail['lead']['stage'] == 'LOST' and detail['receipts'][0]['status'] == 'VOIDED'
assert request('GET', '/sales/metrics')['revenueThisMonth'] == baseline['revenueThisMonth']
print(json.dumps({'passed': True, 'leadId': lid, 'checks': ['auth', 'login', 'manual conversation', 'idempotency', 'followup', 'receipt', 'stop on paid', 'void', 'revenue restored']}, ensure_ascii=False))
