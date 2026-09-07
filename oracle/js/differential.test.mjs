/* Differential test: the visualizer's step engine against the Java references.
 *
 * The same operation sequence is replayed on both. Every state the two engines
 * pass through has to match, one for one, and so do the counters that justify
 * each operation's Big-O. Sequences are seeded, so any failure reported here
 * can be replayed by running the same seed again. */

import test from 'node:test';
import assert from 'node:assert/strict';
import { makeDriver, STRUCTURE_NAMES } from './driver.mjs';
import { runJobs } from './oracle.mjs';
import { diffRun } from './compare.mjs';
import { randomSequence } from './sequences.mjs';
import { loadEngine } from './engine.mjs';

const SEQUENCES_PER_STRUCTURE = Number(process.env.PAWIE_SEQUENCES || 60);
const SEQUENCE_LENGTH = Number(process.env.PAWIE_SEQUENCE_LENGTH || 24);

/* One engine instance is shared: loading it parses a 300 KB file, and the
   driver resets the model between sequences anyway. */
const engine = loadEngine();

function replay(structure, ops, options = {}) {
  const driver = makeDriver(structure, { engine, ...options });
  return { steps: ops.map((op) => driver.apply(op.name, op.args)), stateKeys: driver.stateKeys };
}

for (const structure of STRUCTURE_NAMES) {
  test(`${structure}: every step matches the Java reference`, () => {
    const jobs = [];
    const engineRuns = [];
    const seeds = [];

    for (let i = 0; i < SEQUENCES_PER_STRUCTURE; i++) {
      const seed = 0x9e3779b9 ^ (i * 2654435761);
      const ops = randomSequence(structure, seed, SEQUENCE_LENGTH);
      seeds.push({ seed, ops });
      engineRuns.push(replay(structure, ops));
      jobs.push({ structure, ops });
    }

    const javaRuns = runJobs(jobs);

    for (let i = 0; i < jobs.length; i++) {
      const problems = diffRun(engineRuns[i].steps, javaRuns[i].steps,
        { stateKeys: engineRuns[i].stateKeys });
      assert.deepEqual(
        problems,
        [],
        `${structure} sequence #${i} (seed ${seeds[i].seed}) diverged:\n  `
        + problems.join('\n  ')
        + `\n  operations: ${JSON.stringify(seeds[i].ops)}`,
      );
    }
  });
}
