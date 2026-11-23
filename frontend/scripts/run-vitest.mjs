/**
 * Vitest를 실제 경로 표기로 실행한다 (TSK-QA-004).
 *
 * Windows는 `d:\`와 `D:\`를 같은 폴더로 보지만, Node/Vite는 불러온 모듈을 경로 문자열로 기억한다.
 * 터미널이 소문자 드라이브(`d:\...`, VS Code 기본)에서 시작하면 같은 파일이 두 경로로 따로 로드되어
 * 모든 테스트가 `Cannot read properties of undefined (reading 'config')`로 실패한다.
 * 현재 폴더를 운영체제가 기록한 실제 표기(`D:\...`)로 바꾼 뒤 Vitest를 실행해 이를 피한다.
 * 다른 OS에서는 경로가 그대로라 동작 차이가 없다.
 */
import { realpathSync } from 'node:fs';
import { join } from 'node:path';
import { spawn } from 'node:child_process';

const cwd = realpathSync.native(process.cwd());
const vitestBin = join(cwd, 'node_modules', 'vitest', 'vitest.mjs');

const child = spawn(process.execPath, [vitestBin, ...process.argv.slice(2)], { cwd, stdio: 'inherit' });
child.on('exit', (code, signal) => process.exit(signal ? 1 : (code ?? 1)));
