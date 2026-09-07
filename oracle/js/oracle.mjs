/* The bridge to the Java reference implementations.
 *
 * A job is handed to pawie.oracle.Runner on stdin and the step trace comes back
 * on stdout. Jobs are batched into one JVM because the randomized suites replay
 * thousands of sequences and JVM startup would otherwise dominate the run. */

import { spawnSync, execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readdirSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const HERE = dirname(fileURLToPath(import.meta.url));
export const JAVA_DIR = join(HERE, '..', 'java');
const SRC_DIR = join(JAVA_DIR, 'src');
const OUT_DIR = join(JAVA_DIR, 'out');

function javaFiles(dir) {
  const out = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) out.push(...javaFiles(full));
    else if (entry.name.endsWith('.java')) out.push(full);
  }
  return out;
}

/** Compiles the reference implementations when a source file is newer than the
    classes, so `node --test` alone is enough to run everything. */
export function ensureCompiled() {
  const sources = javaFiles(SRC_DIR);
  const newestSource = Math.max(...sources.map((f) => statSync(f).mtimeMs));
  const marker = join(OUT_DIR, 'pawie', 'oracle', 'Runner.class');
  if (existsSync(marker) && statSync(marker).mtimeMs > newestSource) return;
  mkdirSync(OUT_DIR, { recursive: true });
  execFileSync('javac', ['-d', OUT_DIR, ...sources], { stdio: 'pipe' });
}

/** Runs one job and returns {structure, steps, final}. */
export function runJob(job) {
  return runJobs([job])[0];
}

export function runJobs(jobs) {
  ensureCompiled();
  const res = spawnSync('java', ['-cp', OUT_DIR, 'pawie.oracle.Runner'], {
    input: JSON.stringify({ jobs }),
    encoding: 'utf8',
    maxBuffer: 256 * 1024 * 1024,
  });
  if (res.status !== 0) {
    throw new Error('the Java oracle failed:\n' + (res.stderr || res.stdout));
  }
  return JSON.parse(res.stdout).results;
}
