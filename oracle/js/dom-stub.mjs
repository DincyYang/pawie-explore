/* A minimal, permissive DOM stand-in.
   explore.html's engine script wires up event handlers and paints the page at
   load time. None of that matters to the algorithms, but the script has to run
   all the way through or its top-level `let` bindings (model, modelHeap, ...)
   never get initialised. This stub answers every DOM call with something
   harmless and chainable so the script completes, then the tests talk to the
   generator functions directly. */

const NOOP = () => {};

/* querySelectorAll answers with an empty list that still hands back a stub for
   any index. Loops over it do nothing (length is 0), while direct indexing such
   as `nodes[1].textContent = ...` keeps working. */
function makeNodeList() {
  const backing = [];
  const cache = new Map();
  return new Proxy(backing, {
    get(target, prop) {
      if (typeof prop === 'string' && /^\d+$/.test(prop)) {
        if (!cache.has(prop)) cache.set(prop, makeElement('node' + prop));
        return cache.get(prop);
      }
      return Reflect.get(target, prop);
    },
  });
}

function makeElement(id) {
  const el = {
    id: id || '',
    value: '',
    innerHTML: '',
    outerHTML: '',
    textContent: '',
    className: '',
    checked: false,
    hidden: false,
    disabled: false,
    scrollTop: 0,
    scrollHeight: 0,
    offsetWidth: 0,
    offsetHeight: 0,
    children: [],
    childNodes: [],
    parentNode: null,
    firstChild: null,
    lastChild: null,
    nextSibling: null,
    style: {},
    dataset: {},
    classList: {
      add: NOOP, remove: NOOP, toggle: NOOP, contains: () => false, replace: NOOP,
    },
    addEventListener: NOOP,
    removeEventListener: NOOP,
    dispatchEvent: () => true,
    appendChild: (c) => c,
    removeChild: (c) => c,
    insertBefore: (c) => c,
    replaceChild: (c) => c,
    insertAdjacentHTML: NOOP,
    setAttribute: NOOP,
    removeAttribute: NOOP,
    getAttribute: () => null,
    hasAttribute: () => false,
    scrollIntoView: NOOP,
    focus: NOOP,
    blur: NOOP,
    click: NOOP,
    remove: NOOP,
    closest: () => null,
    contains: () => false,
    getBoundingClientRect: () => ({ top: 0, left: 0, right: 0, bottom: 0, width: 0, height: 0 }),
    getContext: () => makeCanvasContext(),
    querySelector: () => makeElement(),
    querySelectorAll: () => makeNodeList(),
    getElementsByClassName: () => makeNodeList(),
    getElementsByTagName: () => makeNodeList(),
    animate: () => ({ finished: Promise.resolve(), cancel: NOOP }),
  };
  /* Anything the engine reaches for that is not listed above answers with a
     no-op function, which covers onclick/oninput assignment reads and any
     helper we have not thought of. Assignment still works normally. */
  return new Proxy(el, {
    get(target, prop) {
      if (prop in target) return target[prop];
      if (typeof prop === 'symbol') return undefined;
      return NOOP;
    },
    has() { return true; },
  });
}

function makeCanvasContext() {
  return new Proxy({ canvas: null, fillStyle: '', strokeStyle: '', font: '', globalAlpha: 1,
    measureText: () => ({ width: 0 }), createLinearGradient: () => ({ addColorStop: NOOP }),
    getImageData: () => ({ data: [] }) }, {
    get(t, p) { return p in t ? t[p] : NOOP; },
  });
}

export function makeDomStub() {
  const documentEl = makeElement('document');
  const document = new Proxy({
    body: makeElement('body'),
    documentElement: makeElement('html'),
    head: makeElement('head'),
    readyState: 'complete',
    getElementById: (id) => makeElement(id),
    querySelector: () => makeElement(),
    querySelectorAll: () => makeNodeList(),
    getElementsByClassName: () => makeNodeList(),
    getElementsByTagName: () => makeNodeList(),
    createElement: (tag) => makeElement(tag),
    createElementNS: (ns, tag) => makeElement(tag),
    createTextNode: (t) => ({ textContent: t }),
    createDocumentFragment: () => makeElement('fragment'),
    addEventListener: NOOP,
    removeEventListener: NOOP,
    execCommand: NOOP,
    activeElement: documentEl,
  }, { get(t, p) { return p in t ? t[p] : NOOP; } });

  const localStorage = {
    _m: new Map(),
    getItem(k) { return this._m.has(k) ? this._m.get(k) : null; },
    setItem(k, v) { this._m.set(k, String(v)); },
    removeItem(k) { this._m.delete(k); },
    clear() { this._m.clear(); },
  };

  const win = {
    document,
    localStorage,
    sessionStorage: localStorage,
    location: { href: 'http://localhost/explore.html', search: '', hash: '', protocol: 'http:' },
    navigator: { userAgent: 'node-oracle', language: 'en-US', clipboard: { writeText: () => Promise.resolve() } },
    innerWidth: 1440,
    innerHeight: 900,
    devicePixelRatio: 1,
    addEventListener: NOOP,
    removeEventListener: NOOP,
    matchMedia: () => ({ matches: false, addEventListener: NOOP, addListener: NOOP }),
    requestAnimationFrame: (cb) => { void cb; return 0; },
    cancelAnimationFrame: NOOP,
    setTimeout: () => 0,
    clearTimeout: NOOP,
    setInterval: () => 0,
    clearInterval: NOOP,
    getComputedStyle: () => new Proxy({}, { get: () => '' }),
    scrollTo: NOOP,
    alert: NOOP,
    console,
    fetch: () => Promise.reject(new Error('network disabled in the oracle harness')),
  };
  win.window = win;
  win.self = win;
  win.globalThis = win;
  win.top = win;
  return win;
}
