/* Loads the real step engine out of explore.html and hands it back as a live
   JavaScript object graph.

   The point of loading the shipped file instead of a copy is that the tests
   can never drift from what students actually run. explore.html stays a single
   deliverable file; this module is the seam that makes it testable. */

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import vm from 'node:vm';
import { makeDomStub } from './dom-stub.mjs';

const HERE = dirname(fileURLToPath(import.meta.url));
export const EXPLORE_PATH = join(HERE, '..', '..', 'explore.html');

/* The engine lives in the first inline <script> of explore.html. Slicing on the
   tags rather than on line numbers keeps this working while the file is edited. */
export function extractEngineSource(html) {
  const open = html.indexOf('<script>');
  if (open < 0) throw new Error('explore.html has no inline <script>');
  const start = open + '<script>'.length;
  const end = html.indexOf('</script>', start);
  if (end < 0) throw new Error('unterminated <script> in explore.html');
  const src = html.slice(start, end);
  if (!/function newGen\s*\(/.test(src)) {
    throw new Error('the first inline script is not the step engine any more');
  }
  return src;
}

/* Names the harness pulls out of the engine's own lexical scope. Top-level
   `let` bindings do not land on globalThis, so they are read back through an
   expression evaluated in the same context. */
const EXPORTED = [
  // shared
  'DEFAULT_CAPACITY', 'FACTOR', 'capacityFor', 'parseToken',
  // ArrayList
  'model', 'genAdd', 'genRemove', 'genSet', 'genGet', 'genArrSize', 'genArrGetCapacity',
  'genCtorNoarg', 'genCtorCap', 'genCtorArr',
  // LinkedList
  'modelLL', 'genLLadd', 'genLLremove', 'genLLset', 'genLLget', 'genLLclear',
  'genLLisEmpty', 'genLLsize',
  // ListIterator
  'modelIter', 'genItNext', 'genItPrevious', 'genItAdd', 'genItSet', 'genItRemove', 'genItQuery',
  // Stack and Queue (both over the circular-array deque)
  'STACK_DEFAULT_CAP', 'QUEUE_DEFAULT_CAP',
  'modelStack', 'genStackPush', 'genStackPop', 'genStackPeek', 'genStackEmpty', 'genStackSize',
  'modelQueue', 'genQueueEnqueue', 'genQueueDequeue', 'genQueuePeek', 'genQueueEmpty', 'genQueueSize',
  // Heap
  'modelHeap', 'genHeapInsert', 'genHeapRemoveMin', 'genHeapPeek', 'genHeapSize', 'genHeapClear',
  'genHeapBuild',
  // BST
  'modelTree', 'genTreeInsert', 'genTreeSearch', 'genTreeRemove', 'genTreeSize', 'genTreeClear',
  'bstCount', 'cloneTreeNode',
  // HashMap and HashSet
  'HASH_DEFAULT_CAP', 'HASH_LOAD', 'modelHash', 'javaHashCode', 'hashIndex',
  'genHput', 'genHget', 'genHremove', 'genHsize', 'genHisEmpty', 'genHclear', 'genHgetCapacity',
];

export function loadEngine(explorePath = EXPLORE_PATH) {
  const html = readFileSync(explorePath, 'utf8');
  const src = extractEngineSource(html);

  const sandbox = makeDomStub();
  const context = vm.createContext(sandbox);
  new vm.Script(src, { filename: 'explore.html:engine' }).runInContext(context);

  const grab = `({${EXPORTED.map((n) => `${n}: typeof ${n} === 'undefined' ? undefined : ${n}`).join(', ')}})`;
  const api = vm.runInContext(grab, context);

  const missing = EXPORTED.filter((n) => api[n] === undefined);
  if (missing.length) {
    throw new Error('engine is missing expected names: ' + missing.join(', '));
  }

  /* The generators read the committed model out of the engine's scope, so the
     harness has to write it back there rather than mutating a local copy. */
  api.setModel = (name, value) => {
    context.__incoming = value;
    vm.runInContext(`${name} = __incoming;`, context);
    delete context.__incoming;
    return vm.runInContext(name, context);
  };
  api.read = (expr) => vm.runInContext(`(${expr})`, context);
  api.context = context;
  return api;
}
