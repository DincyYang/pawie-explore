package pawie.oracle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A list iterator: a cursor that sits in the gap between two nodes.
 *
 * The whole lesson of this structure is that the cursor is already where the
 * work is, so nothing walks from the head. Every operation here touches a
 * bounded number of links, which the hop and link counters record and the
 * differential test checks against the visualizer.
 *
 * State carried across operations: idx (the index next() would return),
 * forward (the direction of the last move), and canSet (whether a next() or
 * previous() is still pending, which is what set() and remove() need).
 */
public class RefListIterator extends RefStructure {

  private final List<Object> vals = new ArrayList<>();
  private int idx;
  private boolean forward = true;
  private boolean canSet;

  public RefListIterator(List<Object> initial) {
    if (initial != null) vals.addAll(initial);
  }

  @Override
  public Object state() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("vals", new ArrayList<>(vals));
    m.put("idx", idx);
    m.put("forward", forward);
    m.put("canSet", canSet);
    return m;
  }

  public boolean hasNext() { return idx < vals.size(); }

  public boolean hasPrevious() { return idx > 0; }

  public int nextIndex() { return idx; }

  public int previousIndex() { return idx - 1; }

  public Object next() {
    if (!hasNext()) throw new NoSuchElementException("the cursor is at the end");
    Object v = vals.get(idx);
    count("hops");
    idx++;
    forward = true;
    canSet = true;
    snap();
    return v;
  }

  public Object previous() {
    if (!hasPrevious()) throw new NoSuchElementException("the cursor is at the front");
    Object v = vals.get(idx - 1);
    count("hops");
    idx--;
    forward = false;
    canSet = true;
    snap();
    return v;
  }

  /** Splices a node into the gap, then leaves the cursor just past it. */
  public void add(Object value) {
    if (value == null) throw new NullPointerException("a node cannot hold null");
    count("links", 4);            // prev, next, and the two neighbour rewires
    vals.add(idx, value);
    idx++;
    canSet = false;               // remove() is not allowed straight after add()
    snap();
  }

  /** Overwrites the node the last next() or previous() handed back. */
  public Object set(Object value) {
    if (value == null) throw new NullPointerException("a node cannot hold null");
    if (!canSet) throw new IllegalStateException("no next() or previous() is pending");
    int target = forward ? idx - 1 : idx;
    Object old = vals.get(target);
    vals.set(target, value);
    count("links");
    snap();
    return old;
  }

  /** Unlinks the node the last next() or previous() handed back. */
  public Object remove() {
    if (!canSet) throw new IllegalStateException("no next() or previous() is pending");
    int target = forward ? idx - 1 : idx;
    Object removed = vals.remove(target);
    count("links", 2);            // the two neighbours bridge across the gap
    if (forward) idx--;           // going forward, the cursor closes up
    canSet = false;
    snap();
    return removed;
  }

  @Override
  public Object invoke(String op, List<Object> args) {
    switch (op) {
      case "next": return next();
      case "previous": return previous();
      case "add": add(args.get(0)); return null;
      case "set": return set(args.get(0));
      case "remove": return remove();
      case "hasNext": return hasNext();
      case "hasPrevious": return hasPrevious();
      case "nextIndex": return nextIndex();
      case "previousIndex": return previousIndex();
      default:
        throw new IllegalArgumentException("unknown ListIterator operation: " + op);
    }
  }

  @Override
  protected String[] counterNames() {
    return new String[] { "hops", "links" };
  }

}
