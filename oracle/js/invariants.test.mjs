/* Property tests over the visualizer's own step engine.
 *
 * The differential suite checks the engine against the Java reference. These
 * checks hold it to properties that have to be true on their own terms: the
 * structure is still well formed after every operation, the animation begins
 * and ends on the state the application actually commits, a refused call
 * changes nothing, and replaying is deterministic. */

import test from 'node:test';
import assert from 'node:assert/strict';
import { makeDriver, STRUCTURE_NAMES } from './driver.mjs';
import { randomSequence } from './sequences.mjs';
import { INVARIANTS } from './invariants.mjs';
import { loadEngine } from './engine.mjs';
import { canonical } from './compare.mjs';

const SEQUENCES = Number(process.env.PAWIE_SEQUENCES || 40);
const LENGTH = Number(process.env.PAWIE_SEQUENCE_LENGTH || 24);

const engine = loadEngine();
const clone = (v) => JSON.parse(JSON.stringify(v));

for (const structure of STRUCTURE_NAMES) {
  test(`${structure}: stays well formed after every operation`, () => {
    for (let i = 0; i < SEQUENCES; i++) {
      const seed = 0x85ebca6b ^ (i * 2246822519);
      const ops = randomSequence(structure, seed, LENGTH);
      const driver = makeDriver(structure, { engine });
      const history = {};
      ops.forEach((op, k) => {
        driver.apply(op.name, op.args);
        const bad = INVARIANTS[structure](driver.model(), history);
        assert.deepEqual(bad, [], `${structure} seed ${seed}, after op ${k + 1} `
          + `${op.name}(${op.args.join(', ')}): ${bad.join('; ')}`);
      });
    }
  });

  test(`${structure}: the animation starts and ends on the committed state`, () => {
    for (let i = 0; i < SEQUENCES; i++) {
      const seed = 0xc2b2ae35 ^ (i * 40503);
      const ops = randomSequence(structure, seed, LENGTH);
      const driver = makeDriver(structure, { engine });
      for (const op of ops) {
        const before = canonical(driver.stepState());
        const step = driver.apply(op.name, op.args);
        const first = canonical(step.states[1]);          // states[0] is the pre-state
        const last = canonical(step.states[step.states.length - 1]);
        const after = canonical(driver.stepState());      // now the committed state

        assert.equal(first, before,
          `${structure} ${op.name}: the first frame does not show the state the operation started from`);
        assert.equal(last, after,
          `${structure} ${op.name}: the last frame shows something other than what the app committed`);
      }
    }
  });

  test(`${structure}: a refused operation changes nothing`, () => {
    for (let i = 0; i < SEQUENCES; i++) {
      const seed = 0x27d4eb2f ^ (i * 668265263);
      const ops = randomSequence(structure, seed, LENGTH);
      const driver = makeDriver(structure, { engine });
      for (const op of ops) {
        const before = canonical(driver.model());
        const step = driver.apply(op.name, op.args);
        if (step.error) {
          assert.equal(canonical(driver.model()), before,
            `${structure} ${op.name}(${op.args.join(', ')}) was refused but still changed the structure`);
        }
      }
    }
  });

  test(`${structure}: replaying an operation gives the same frames`, () => {
    for (let i = 0; i < 8; i++) {
      const seed = 0x165667b1 ^ (i * 374761393);
      const ops = randomSequence(structure, seed, LENGTH);
      const driver = makeDriver(structure, { engine });
      for (const op of ops) {
        const saved = clone(driver.model());
        const once = canonical(driver.apply(op.name, op.args).states);
        driver.engine.setModel(driverModelName(driver), clone(saved));
        const twice = canonical(driver.apply(op.name, op.args).states);
        assert.equal(once, twice,
          `${structure} ${op.name} produced different frames from the same starting state`);
      }
    }
  });

  test(`${structure}: earlier frames are not disturbed by later operations`, () => {
    /* The player steps backwards through the frame list, so a frame that shares
       an array with the live model would show the wrong thing on the way back.
       This is the reversibility the transport controls depend on. */
    for (let i = 0; i < 8; i++) {
      const seed = 0x9e3779b1 ^ (i * 2654435769);
      const ops = randomSequence(structure, seed, LENGTH);
      const driver = makeDriver(structure, { engine });
      const kept = [];
      for (const op of ops) {
        const step = driver.apply(op.name, op.args);
        kept.push({ op, frames: step.frames, recorded: canonical(step.frames) });
      }
      for (const entry of kept) {
        assert.equal(canonical(entry.frames), entry.recorded,
          `${structure}: the frames of ${entry.op.name} changed after later operations ran`);
      }
    }
  });
}

function driverModelName(driver) {
  return {
    ArrayList: 'model', LinkedList: 'modelLL', ListIterator: 'modelIter',
    Stack: 'modelStack', Queue: 'modelQueue', MinHeap: 'modelHeap',
    BST: 'modelTree', HashMap: 'modelHash', HashSet: 'modelHash',
  }[driver.structure];
}
