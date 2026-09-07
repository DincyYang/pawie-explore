/* Seeded random operation sequences.
 *
 * Every sequence is reproducible from its seed, so a failure can be replayed
 * exactly: the tests print the seed, and rerunning with it gives the same
 * operations. Sequences deliberately include calls that should fail (indices
 * out of range, nulls where the structure forbids them, set() before any
 * next()), because the two engines have to agree about refusals too. */

export function rng(seed) {
  let a = seed >>> 0;
  return () => {
    a |= 0; a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const pick = (r, xs) => xs[Math.floor(r() * xs.length)];
const int = (r, lo, hi) => lo + Math.floor(r() * (hi - lo + 1));

const WORDS = ['cat', 'dog', 'emu', 'fox', 'ant', 'owl', 'bee', 'elk'];

/* An index that is usually valid and occasionally is not, so the out-of-range
   branches get exercised without drowning the sequence in refusals. */
function indexNear(r, size, inclusive) {
  const top = inclusive ? size : size - 1;
  if (r() < 0.15) return int(r, -2, top + 2);
  return top < 0 ? 0 : int(r, 0, top);
}

/**
 * Builds a sequence for one structure. The generator tracks just enough shadow
 * state (a size, a cursor) to aim most calls at legal arguments.
 */
export function randomSequence(structure, seed, length = 24) {
  const r = rng(seed);
  const ops = [];
  const value = () => int(r, 0, 30);
  /* An ArrayList may legitimately store null while the linked structures
     refuse it, so both engines have to agree on which calls are refused, and
     on how a stored null is shifted around. */
  const maybeNull = () => (r() < 0.08 ? null : value());

  if (structure === 'ArrayList' || structure === 'LinkedList') {
    let size = 0;
    const names = structure === 'ArrayList'
      ? ['append', 'add', 'remove', 'set', 'get', 'size', 'getCapacity']
      : ['append', 'add', 'remove', 'set', 'get', 'size', 'isEmpty', 'clear'];
    for (let i = 0; i < length; i++) {
      const name = size === 0 ? pick(r, ['append', 'add', 'append']) : pick(r, names);
      if (name === 'append') { ops.push({ name, args: [maybeNull()] }); size++; }
      else if (name === 'add') {
        const idx = indexNear(r, size, true);
        ops.push({ name, args: [idx, maybeNull()] });
        if (idx >= 0 && idx <= size) size++;
      } else if (name === 'remove') {
        const idx = indexNear(r, size, false);
        ops.push({ name, args: [idx] });
        if (idx >= 0 && idx < size) size--;
      } else if (name === 'set') ops.push({ name, args: [indexNear(r, size, false), value()] });
      else if (name === 'get') ops.push({ name, args: [indexNear(r, size, false)] });
      else if (name === 'clear') { ops.push({ name, args: [] }); size = 0; }
      else ops.push({ name, args: [] });
    }
    return ops;
  }

  if (structure === 'ListIterator') {
    for (let i = 0; i < length; i++) {
      const name = pick(r, ['next', 'next', 'previous', 'add', 'set', 'remove',
        'hasNext', 'hasPrevious', 'nextIndex', 'previousIndex']);
      ops.push({ name, args: name === 'add' || name === 'set' ? [maybeNull()] : [] });
    }
    return ops;
  }

  if (structure === 'Stack') {
    for (let i = 0; i < length; i++) {
      const name = pick(r, ['push', 'push', 'push', 'pop', 'peek', 'empty', 'size']);
      ops.push({ name, args: name === 'push' ? [maybeNull()] : [] });
    }
    return ops;
  }

  if (structure === 'Queue') {
    for (let i = 0; i < length; i++) {
      const name = pick(r, ['enqueue', 'enqueue', 'enqueue', 'dequeue', 'peek', 'empty', 'size']);
      ops.push({ name, args: name === 'enqueue' ? [maybeNull()] : [] });
    }
    return ops;
  }

  if (structure === 'MinHeap') {
    for (let i = 0; i < length; i++) {
      const name = pick(r, ['insert', 'insert', 'insert', 'removeMin', 'getMin', 'size', 'clear']);
      ops.push({ name, args: name === 'insert' ? [maybeNull()] : [] });
    }
    return ops;
  }

  if (structure === 'BST') {
    /* Keys are drawn from a small pool so duplicates, hits and misses all come
       up, and so two-child removals actually happen. */
    const pool = [];
    for (let i = 0; i < 14; i++) pool.push(int(r, 0, 40));
    for (let i = 0; i < length; i++) {
      const name = pick(r, ['insert', 'insert', 'insert', 'remove', 'search', 'size', 'clear']);
      ops.push({ name, args: ['insert', 'remove', 'search'].includes(name) ? [pick(r, pool)] : [] });
    }
    return ops;
  }

  if (structure === 'HashMap' || structure === 'HashSet') {
    const isSet = structure === 'HashSet';
    const pool = [];
    for (let i = 0; i < 12; i++) pool.push(r() < 0.5 ? int(r, 0, 40) : pick(r, WORDS));
    for (let i = 0; i < length; i++) {
      const name = isSet
        ? pick(r, ['add', 'add', 'add', 'remove', 'contains', 'size', 'isEmpty', 'clear'])
        : pick(r, ['put', 'put', 'put', 'remove', 'get', 'size', 'isEmpty', 'clear', 'getCapacity']);
      if (name === 'put') ops.push({ name, args: [pick(r, pool), value()] });
      else if (['add', 'remove', 'get', 'contains'].includes(name)) {
        ops.push({ name, args: [pick(r, pool)] });
      } else ops.push({ name, args: [] });
    }
    return ops;
  }

  throw new Error('no sequence generator for ' + structure);
}
