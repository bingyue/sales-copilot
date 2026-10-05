#!/usr/bin/env python3
"""Generate fresh deployment credentials without printing them or replacing an existing environment."""
from pathlib import Path
import secrets
import re
root = Path(__file__).resolve().parents[1]
target = root / 'deploy/.env'
if target.exists():
    raise SystemExit('deploy/.env already exists; left unchanged.')
content = (root / 'deploy/.env.example').read_text()
for name, length in [('ADMIN_PASSWORD', 24), ('JWT_SECRET', 48), ('DB_PASSWORD', 32), ('MYSQL_ROOT_PASSWORD', 32)]:
    content = re.sub('^' + name + '=.*$', name + '=' + secrets.token_urlsafe(length), content, flags=re.M)
target.write_text(content)
target.chmod(0o600)
print('Created deploy/.env with fresh credentials. Edit the AI and public URL settings before starting.')
