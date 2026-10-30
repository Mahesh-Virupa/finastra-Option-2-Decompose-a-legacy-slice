#!/usr/bin/env bash
# Option 2 walkthrough: the strangler pattern in action. Needs: curl. Optional: jq.
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"; ADMIN="${ADMIN_API_KEY:-dev-admin-key}"
pp() { if command -v jq >/dev/null; then jq .; else cat; fi; }
say() { printf '\n\033[1;36m== %s\033[0m\n' "$*"; }
trade() { curl -s -X POST "$BASE/api/v1/trades" -H "X-Tenant-Id: $1" -H 'Content-Type: application/json' -d "$2" | pp; }
route() { curl -s -X PUT "$BASE/admin/v1/routing/$1" -H "X-Admin-Key: $ADMIN" -H 'Content-Type: application/json' -d "{\"mode\":\"$2\"}" | pp; }

say "Health"; curl -s "$BASE/actuator/health" | pp

say "Same payload, three tenants: legacy if/else is now typed policy (see application.yml)"
P='{"instrument":"FX","trader":"t1","notional":1500000}'
echo "-- CLIENT_A (dual approval, 10% discount, deskCode, SHADOW: legacy runs alongside and is compared)"; trade CLIENT_A "$P"
echo "-- CLIENT_B (APAC tax, FX -> legacy queue)"; trade CLIENT_B "$P"
echo "-- CLIENT_Z (no policy: core only)";         trade CLIENT_Z "$P"

say "Shadow evidence: compared vs mismatched (target: mismatches = 0 before cut-over)"
curl -s "$BASE/actuator/metrics/shadow.compared"  | pp
curl -s "$BASE/actuator/metrics/shadow.mismatch"  | pp

say "Routing table, then ROLLBACK CLIENT_B to the legacy path instantly (no deploy)"
curl -s "$BASE/admin/v1/routing" -H "X-Admin-Key: $ADMIN" | pp
route CLIENT_B LEGACY
trade CLIENT_B '{"instrument":"FX","trader":"t1","notional":2000000}'   # servedBy: LEGACY, same business result
echo "-- and forward again"; route CLIENT_B NEW
trade CLIENT_B '{"instrument":"FX","trader":"t1","notional":2000000}'   # servedBy: NEW

say "Dry run (nothing persisted)"
curl -s -X POST "$BASE/api/v1/trades/simulate" -H 'X-Tenant-Id: CLIENT_A' -H 'Content-Type: application/json' -d "$P" | pp

say "Invalid trade -> RFC 7807 problem"
trade CLIENT_A '{"instrument":"XYZ","trader":"t1","notional":1000}'
