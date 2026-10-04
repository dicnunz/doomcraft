#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
qa_log=${DOOMCRAFT_QA_LOG:-docs/final-survival/latest-verification.log}
mkdir -p "$(dirname "$qa_log")"
./tools/gradle.sh runClient -Psurvival -PsurvivalDemo "$@" > "$qa_log" 2>&1
python3 - "$qa_log" <<'PY'
import pathlib,sys
p=pathlib.Path(sys.argv[1]);s=p.read_text()
assert 'SURVIVAL_DEMO_ALL_CHECKS_PASSED' in s and 'SURVIVAL_CHECK FAIL' not in s, f'Client QA failed; inspect {p}'
print(f'PASS: {s.count("SURVIVAL_CHECK PASS")} real-client assertions; {p}')
PY
