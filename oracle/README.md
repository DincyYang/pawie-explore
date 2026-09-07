# Pawie oracle: reference implementations and differential tests

The visualizer in `explore.html` animates eight data structures step by step.
Every frame it draws is a claim about what a real implementation would do at
that moment. This directory is what checks those claims.

There are two halves:

- `java/` holds a reference implementation of each structure, written the way
  the course teaches it and instrumented to record its own state after every
  field mutation. A JUnit suite holds those references to `java.util`
  semantics, so they are worth trusting as an oracle.
- `js/` loads the shipped step engine out of `explore.html`, replays the same
  operation sequences through it, and compares every state and every cost
  counter against the reference.

```
npm test          # both suites
npm run test:java # JUnit only
npm run test:js   # differential and property suites only
npm run test:soak # a much wider sweep of random sequences
```

Nothing here needs a build tool or a package to be installed. A JDK, Node, and
the two JUnit jars vendored in `java/lib` are the whole toolchain, which is
also what the GitHub Actions workflow uses.

## What is actually compared

For each operation the two engines produce a step trace: the sequence of states
the structure passes through, plus the counters that justify the operation's
Big-O (array writes, elements shifted, resize copies, hops, pointer writes,
comparisons, swaps, chain probes, rehashes).

Both sides snapshot the state their own fields describe. While a resize is
under way the field still points at the old array, so the state simply repeats
until the pointer swings across; runs of identical consecutive states are
collapsed on both sides before comparing. What is left is exactly the moments
where observable state changed, and those have to line up one for one.

On top of the trace, each operation has to agree on:

- whether the call was refused at all, and
- the committed state afterwards, cursor fields included.

Frame colours, arrows, phase ids and narration are the visualizer's own
business and are not compared.

## Why the reference implementations are the ones with the tests

A differential test says two implementations disagree. It cannot say which one
is wrong. That is what `java/test` is for: the references are checked against
`java.util.ArrayList`, `LinkedList` and its `ListIterator`, `ArrayDeque`,
`PriorityQueue`, `TreeSet`, `HashMap` and `HashSet`, both with hand-written
cases for the boundaries and with randomized model-based runs that re-check the
structure's invariants after every single operation.

The invariants are the ones that actually break in student code, and in
visualizers: the heap property, a sorted in-order walk with no duplicate keys,
next and prev links that agree in both directions, a size field that matches
the nodes you can walk to, every key in the bucket its hash names, a capacity
that never shrinks.

## Properties checked against the step engine directly

`js/invariants.test.mjs` holds the engine to things that must be true whatever
the reference says:

- the structure is still well formed after every operation
- the first frame shows the state the operation started from, and the last
  frame shows the state the application commits (an animation that ends
  somewhere other than where the app lands is a lie to the student)
- a refused operation changes nothing
- replaying an operation from the same state produces identical frames
- earlier frames are untouched by later operations, which is what stepping
  backwards through the transport controls depends on

## Reproducing a failure

Every random sequence comes from a seed, and every failure message carries it
along with the operations. A single case can also be replayed straight through
the reference:

```bash
echo '{"structure":"ArrayList","ops":[{"name":"append","args":[1]},{"name":"add","args":[0,9]}]}' \
  | java -cp oracle/java/out pawie.oracle.Runner
```

## What it found

Two defects in the shipped engine, both fixed in `explore.html`:

1. **The shift counter under-reported when the list stored a null.** Both shift
   helpers decided whether a slot was part of the list by testing it against
   null, which is the same thing as testing the position only while the list
   holds no null elements. The list allows a stored null, and the
   visualizer lets a student type one. With a null in the list, the copy that
   moved it was neither counted as a shift nor shown as a frame, so the
   element count behind the Big-O explanation was wrong. Both helpers now test
   the position (`i >= size`) instead of the value.

2. **`ListIterator.set` never showed the value it replaced.** The operation
   produced a single frame, and that frame already held the new value, so the
   node being overwritten was never on screen. Every sibling operation opens on
   the state it started from. It now does too.
