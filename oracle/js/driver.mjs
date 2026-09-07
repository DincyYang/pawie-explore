/* Drives the visualizer's step engine the same way a click does.
 *
 * For each operation the driver calls the real generator, commits the result
 * through the engine's own loadOp, and projects the resulting frames down to a
 * canonical state trace that can be compared against the Java reference.
 *
 * The projection keeps only what both engines can be expected to agree on: the
 * structure's memory image, plus the counters that justify its Big-O. Colours,
 * arrows, phase ids and narration are the visualizer's own business. */

import { loadEngine } from './engine.mjs';

/* ---------- canonical state projections ---------- */

const preorder = (node, out = []) => {
  if (!node) { out.push(null); return out; }
  out.push(node.key);
  preorder(node.left, out);
  preorder(node.right, out);
  return out;
};

const chains = (table) => table.map((chain) => chain.map((n) => [n.k, n.v]));

/* One entry per structure: how to reset it, how to call it, and how to read a
   frame. `stateOf` is the per-step image; `finalOf` adds any cursor state that
   only has to agree at the end of an operation. */
const STRUCTURES = {
  ArrayList: {
    model: 'model',
    struct: 'array',
    fresh: ({ capacity = 5 }) => ({ data: new Array(capacity).fill(null), size: 0 }),
    stateOf: (f) => ({ data: [...f.data], size: f.size }),
    call: (e, name, args, model) => {
      switch (name) {
        case 'add': return e.genAdd(args[0], args[1], false);
        case 'append': return e.genAdd(model.size, args[0], true);
        case 'remove': return e.genRemove(args[0]);
        case 'set': return e.genSet(args[0], args[1]);
        case 'get': return e.genGet(args[0]);
        case 'size': return e.genArrSize();
        case 'getCapacity': return e.genArrGetCapacity();
        default: throw new Error('unknown ArrayList op ' + name);
      }
    },
  },

  LinkedList: {
    model: 'modelLL',
    struct: 'linked',
    fresh: () => ({ nodes: [] }),
    stateOf: (f) => ({ vals: [...f.vals] }),
    call: (e, name, args, model) => {
      switch (name) {
        case 'add': return e.genLLadd(args[0], args[1], false);
        case 'append': return e.genLLadd(model.nodes.length, args[0], true);
        case 'remove': return e.genLLremove(args[0]);
        case 'set': return e.genLLset(args[0], args[1]);
        case 'get': return e.genLLget(args[0]);
        case 'clear': return e.genLLclear();
        case 'isEmpty': return e.genLLisEmpty();
        case 'size': return e.genLLsize();
        default: throw new Error('unknown LinkedList op ' + name);
      }
    },
  },

  ListIterator: {
    model: 'modelIter',
    struct: 'linked',
    iterator: true,
    fresh: ({ initial = [] }) => ({ nodes: [...initial], idx: 0, forward: true, canSet: false }),
    /* Intermediate frames show the list; the cursor fields are only required to
       agree once the operation has finished, because where exactly the caret
       lands mid-splice is presentation, not state. */
    stateKeys: ['vals'],
    stateOf: (f) => ({ vals: [...f.vals] }),
    finalOf: (m) => ({ vals: [...m.nodes], idx: m.idx, forward: m.forward, canSet: m.canSet }),
    call: (e, name, args) => {
      switch (name) {
        case 'next': return e.genItNext();
        case 'previous': return e.genItPrevious();
        case 'add': return e.genItAdd(args[0]);
        case 'set': return e.genItSet(args[0]);
        case 'remove': return e.genItRemove();
        case 'hasNext': return e.genItQuery('hasnext');
        case 'hasPrevious': return e.genItQuery('hasprev');
        case 'nextIndex': return e.genItQuery('nextidx');
        case 'previousIndex': return e.genItQuery('previdx');
        default: throw new Error('unknown ListIterator op ' + name);
      }
    },
  },

  Stack: {
    model: 'modelStack',
    struct: 'stack',
    fresh: ({ capacity = 5 }) => ({ data: new Array(capacity).fill(null), size: 0, front: 0, rear: 0 }),
    stateOf: (f) => ({ data: [...f.data], size: f.size, front: f.front, rear: f.rear }),
    call: (e, name, args) => {
      switch (name) {
        case 'push': return e.genStackPush(args[0]);
        case 'pop': return e.genStackPop();
        case 'peek': return e.genStackPeek();
        case 'empty': return e.genStackEmpty();
        case 'size': return e.genStackSize();
        default: throw new Error('unknown Stack op ' + name);
      }
    },
  },

  Queue: {
    model: 'modelQueue',
    struct: 'queue',
    fresh: ({ capacity = 5 }) => ({ data: new Array(capacity).fill(null), size: 0, front: 0, rear: 0 }),
    stateOf: (f) => ({ data: [...f.data], size: f.size, front: f.front, rear: f.rear }),
    call: (e, name, args) => {
      switch (name) {
        case 'enqueue': return e.genQueueEnqueue(args[0]);
        case 'dequeue': return e.genQueueDequeue();
        case 'peek': return e.genQueuePeek();
        case 'empty': return e.genQueueEmpty();
        case 'size': return e.genQueueSize();
        default: throw new Error('unknown Queue op ' + name);
      }
    },
  },

  MinHeap: {
    model: 'modelHeap',
    struct: 'heap',
    fresh: () => ({ data: [] }),
    stateOf: (f) => ({ data: [...f.data] }),
    call: (e, name, args) => {
      switch (name) {
        case 'insert': return e.genHeapInsert(args[0]);
        case 'removeMin': return e.genHeapRemoveMin();
        case 'getMin': return e.genHeapPeek();
        case 'size': return e.genHeapSize();
        case 'clear': return e.genHeapClear();
        default: throw new Error('unknown MinHeap op ' + name);
      }
    },
  },

  BST: {
    model: 'modelTree',
    struct: 'tree',
    fresh: () => ({ root: null }),
    stateOf: (f) => ({ tree: preorder(f.root) }),
    call: (e, name, args) => {
      switch (name) {
        case 'insert': return e.genTreeInsert(args[0]);
        case 'search': return e.genTreeSearch(args[0]);
        case 'remove': return e.genTreeRemove(args[0]);
        case 'size': return e.genTreeSize();
        case 'clear': return e.genTreeClear();
        default: throw new Error('unknown BST op ' + name);
      }
    },
  },

  HashMap: {
    model: 'modelHash',
    struct: 'hash',
    hashMode: 'map',
    fresh: ({ capacity = 5 }) => ({ table: Array.from({ length: capacity }, () => []), size: 0 }),
    stateOf: (f) => ({ table: chains(f.table), size: f.size }),
    call: (e, name, args) => {
      switch (name) {
        case 'put': return e.genHput(args[0], args[1], false);
        case 'get': return e.genHget(args[0], false);
        case 'remove': return e.genHremove(args[0], false);
        case 'size': return e.genHsize(false);
        case 'isEmpty': return e.genHisEmpty(false);
        case 'clear': return e.genHclear(false);
        case 'getCapacity': return e.genHgetCapacity();
        default: throw new Error('unknown HashMap op ' + name);
      }
    },
  },

  HashSet: {
    model: 'modelHash',
    struct: 'hash',
    hashMode: 'set',
    setPlaceholder: '•',
    fresh: ({ capacity = 5 }) => ({ table: Array.from({ length: capacity }, () => []), size: 0 }),
    stateOf: (f) => ({ table: chains(f.table), size: f.size }),
    call: (e, name, args) => {
      switch (name) {
        case 'add': return e.genHput(args[0], '•', true);
        case 'contains': return e.genHget(args[0], true);
        case 'remove': return e.genHremove(args[0], true);
        case 'size': return e.genHsize(true);
        case 'isEmpty': return e.genHisEmpty(true);
        case 'clear': return e.genHclear(true);
        default: throw new Error('unknown HashSet op ' + name);
      }
    },
  },
};

export const STRUCTURE_NAMES = Object.keys(STRUCTURES);

export function makeDriver(structureName, options = {}) {
  const spec = STRUCTURES[structureName];
  if (!spec) throw new Error('no such structure: ' + structureName);
  const engine = options.engine || loadEngine();

  /* The engine keeps the active structure in its own globals; set them the way
     clicking the nav would, so loadOp commits into the right model. */
  engine.read(`STRUCT = ${JSON.stringify(spec.struct)}`);
  engine.read(`ITER = ${spec.iterator ? 'true' : 'false'}`);
  if (spec.hashMode) engine.read(`HASHMODE = ${JSON.stringify(spec.hashMode)}`);

  const reset = () => engine.setModel(spec.model, spec.fresh(options));
  reset();

  const model = () => engine.read(spec.model);

  return {
    engine,
    structure: structureName,
    reset,
    model,
    stateKeys: spec.stateKeys || null,
    /** The whole committed state, cursor fields included. */
    state: () => fullState(model(), spec),
    /** The committed state projected the way a frame is, for trace comparisons. */
    stepState: () => spec.stateOf(frameLike(model(), spec)),

    /** Runs one operation and returns its canonical step trace. */
    apply(name, args = []) {
      const before = spec.stateOf(frameLike(model(), spec));
      const result = spec.call(engine, name, args, model());
      const frames = result.frames;

      engine.context.__result = result;
      engine.read('loadOp(__result, false, {noReact:true})');
      delete engine.context.__result;

      const last = frames[frames.length - 1];
      return {
        name,
        args,
        states: [before, ...frames.map(spec.stateOf)],
        finalState: fullState(model(), spec),
        cost: { ...last.cost },
        error: frames.some((f) => f.err) || null,
        label: result.label,
        narration: frames.map((f) => f.say),
        frames,
      };
    },
  };
}

/* The whole committed state, including any cursor the frames do not carry. */
function fullState(model, spec) {
  return spec.finalOf ? spec.finalOf(model) : spec.stateOf(frameLike(model, spec));
}

/* The committed model and a frame carry the same fields under the same names,
   so a model can be projected with the frame projector. The one exception is
   the iterator, which keeps its list under `nodes`. */
function frameLike(model, spec) {
  if (spec.model === 'modelLL') return { vals: model.nodes };
  if (spec.model === 'modelIter') return { vals: model.nodes };
  return model;
}
