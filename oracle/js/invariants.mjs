/* Structural invariants: what must be true of each structure after every
 * operation, whatever sequence got it there.
 *
 * These are checked against the visualizer's own committed model, so they hold
 * the engine to the definition of the data structure rather than to a second
 * implementation. The differential test says two engines disagree; an
 * invariant says which one is wrong. */

const cmp = (a, b) => {
  if (typeof a === 'number' && typeof b === 'number') return a - b;
  const sa = String(a);
  const sb = String(b);
  return sa < sb ? -1 : sa > sb ? 1 : 0;
};

const javaHash = (key) => {
  if (typeof key === 'number') return Math.trunc(key) | 0;
  const s = String(key);
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  return h;
};

const bucketOf = (key, cap) => (javaHash(key) & 0x7fffffff) % cap;

function inorder(node, out = []) {
  if (!node) return out;
  inorder(node.left, out);
  out.push(node.key);
  inorder(node.right, out);
  return out;
}

function countNodes(node) {
  return node ? 1 + countNodes(node.left) + countNodes(node.right) : 0;
}

function bstOrdered(node, low, high) {
  if (!node) return true;
  if (low !== undefined && cmp(node.key, low) <= 0) return false;
  if (high !== undefined && cmp(node.key, high) >= 0) return false;
  return bstOrdered(node.left, low, node.key) && bstOrdered(node.right, node.key, high);
}

/* Each entry returns a list of violated invariants, empty when the model is
   well formed. `history` carries what earlier states established, which is how
   "capacity never shrinks" can be checked at all. */
export const INVARIANTS = {
  ArrayList(m, history) {
    const bad = [];
    const cap = m.data.length;
    if (m.size > cap) bad.push(`size ${m.size} exceeds capacity ${cap}`);
    for (let i = m.size; i < cap; i++) {
      if (m.data[i] !== null) bad.push(`slot ${i} is past size ${m.size} but still holds ${m.data[i]}`);
    }
    if (history.capacity !== undefined && cap < history.capacity) {
      bad.push(`capacity shrank from ${history.capacity} to ${cap}`);
    }
    history.capacity = cap;
    return bad;
  },

  LinkedList(m) {
    const bad = [];
    if (m.nodes.some((v) => v === null)) bad.push('a linked list node holds null');
    return bad;
  },

  ListIterator(m) {
    const bad = [];
    if (m.idx < 0 || m.idx > m.nodes.length) {
      bad.push(`the cursor is at ${m.idx}, outside [0, ${m.nodes.length}]`);
    }
    if (m.nodes.some((v) => v === null)) bad.push('a linked list node holds null');
    return bad;
  },

  Stack: deque,
  Queue: deque,

  MinHeap(m) {
    const bad = [];
    for (let i = 1; i < m.data.length; i++) {
      const p = (i - 1) >> 1;
      if (cmp(m.data[p], m.data[i]) > 0) {
        bad.push(`heap property broken: parent ${m.data[p]} at ${p} is greater than child ${m.data[i]} at ${i}`);
      }
    }
    if (m.data.some((v) => v === null || v === undefined)) bad.push('the heap array has a hole in it');
    return bad;
  },

  BST(m, history) {
    const bad = [];
    if (!bstOrdered(m.root, undefined, undefined)) {
      bad.push(`the in-order walk is not sorted: [${inorder(m.root).join(', ')}]`);
    }
    const keys = inorder(m.root);
    if (new Set(keys.map(String)).size !== keys.length) bad.push('the tree holds a duplicate key');
    if (countNodes(m.root) !== keys.length) bad.push('the tree has a cycle or a shared node');
    void history;
    return bad;
  },

  HashMap: hashTable,
  HashSet: hashTable,
};

function deque(m, history) {
  const bad = [];
  const cap = m.data.length;
  const filled = m.data.filter((v) => v !== null).length;
  if (m.size > cap) bad.push(`size ${m.size} exceeds capacity ${cap}`);
  if (filled !== m.size) bad.push(`size says ${m.size} but ${filled} slot(s) hold a value`);
  if (m.size > 0) {
    if (m.rear !== (m.front + m.size - 1) % cap) {
      bad.push(`rear ${m.rear} is not front ${m.front} plus size ${m.size} minus one, modulo ${cap}`);
    }
    for (let k = 0; k < m.size; k++) {
      const i = (m.front + k) % cap;
      if (m.data[i] === null) bad.push(`slot ${i} is inside the live run but empty`);
    }
  }
  if (history.capacity !== undefined && cap < history.capacity) {
    bad.push(`capacity shrank from ${history.capacity} to ${cap}`);
  }
  history.capacity = cap;
  return bad;
}

function hashTable(m, history) {
  const bad = [];
  const cap = m.table.length;
  let entries = 0;
  const seen = new Set();
  for (let b = 0; b < cap; b++) {
    for (const node of m.table[b]) {
      entries++;
      if (bucketOf(node.k, cap) !== b) {
        bad.push(`key ${node.k} sits in bucket ${b} but hashes to ${bucketOf(node.k, cap)}`);
      }
      const tag = typeof node.k + ':' + node.k;
      if (seen.has(tag)) bad.push(`key ${node.k} appears twice in the table`);
      seen.add(tag);
    }
  }
  if (entries !== m.size) bad.push(`size says ${m.size} but the chains hold ${entries} entries`);
  if (m.size > 0.8 * cap) bad.push(`load ${m.size}/${cap} is over the 0.8 maximum, so the table should have grown`);
  if (history.capacity !== undefined && cap < history.capacity) {
    bad.push(`capacity shrank from ${history.capacity} to ${cap}`);
  }
  history.capacity = cap;
  return bad;
}
