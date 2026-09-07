package pawie.oracle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A binary min-heap: a complete binary tree stored in an array, where the
 * child of slot i lives at 2i+1 and 2i+2 and the parent at (i-1)/2.
 *
 * insert appends at the end and percolates up; removeMin takes the root, moves
 * the last element into its place and percolates down. Comparisons and swaps
 * are counted because those two numbers are the whole argument for O(log n),
 * and a visualizer that shows the wrong number of them teaches the wrong cost.
 */
public class RefMinHeap extends RefStructure {

  private final List<Object> data = new ArrayList<>();

  public RefMinHeap() {
  }

  public RefMinHeap(List<Object> initial) {
    this();
    if (initial != null) data.addAll(initial);
  }

  @Override
  public Object state() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("data", new ArrayList<>(data));
    return m;
  }

  public int size() { return data.size(); }

  public Object getMin() {
    return data.isEmpty() ? null : data.get(0);
  }

  static int parent(int i) { return (i - 1) / 2; }

  /** The smaller of slot i's children, or -1 when i is a leaf. Ties go left. */
  int minChild(int i) {
    int n = data.size();
    int l = 2 * i + 1;
    int r = 2 * i + 2;
    if (l >= n) return -1;
    if (r >= n) return l;
    return Values.cmp(data.get(l), data.get(r)) <= 0 ? l : r;
  }

  public void insert(Object value) {
    if (value == null) throw new NullPointerException("a min-heap cannot hold null");
    data.add(value);
    snap();
    percolateUp(data.size() - 1);
  }

  public Object removeMin() {
    if (data.isEmpty()) return null;
    Object removed = data.get(0);
    if (data.size() == 1) {
      data.remove(0);
      snap();
      return removed;
    }
    data.set(0, data.get(data.size() - 1));
    data.remove(data.size() - 1);
    snap();
    percolateDown(0);
    return removed;
  }

  public void clear() {
    if (data.isEmpty()) return;
    data.clear();
    snap();
  }

  private void percolateUp(int i) {
    while (i > 0) {
      int p = parent(i);
      count("compares");
      if (Values.cmp(data.get(p), data.get(i)) <= 0) return;   // parent is not greater
      swap(p, i);
      i = p;
    }
  }

  private void percolateDown(int i) {
    while (true) {
      int m = minChild(i);
      if (m == -1) return;                                     // a leaf, nothing below
      count("compares");
      if (Values.cmp(data.get(m), data.get(i)) >= 0) return;   // child is not smaller
      swap(i, m);
      i = m;
    }
  }

  private void swap(int a, int b) {
    Object tmp = data.get(a);
    data.set(a, data.get(b));
    data.set(b, tmp);
    count("swaps");
    snap();
  }

  /** Every parent is no greater than either child. The defining property. */
  public boolean heapPropertyHolds() {
    for (int i = 1; i < data.size(); i++) {
      if (Values.cmp(data.get(parent(i)), data.get(i)) > 0) return false;
    }
    return true;
  }

  @Override
  public Object invoke(String op, List<Object> args) {
    switch (op) {
      case "insert": insert(args.get(0)); return null;
      case "removeMin": return removeMin();
      case "getMin": return getMin();
      case "size": return size();
      case "clear": clear(); return null;
      default:
        throw new IllegalArgumentException("unknown heap operation: " + op);
    }
  }

  @Override
  protected String[] counterNames() {
    return new String[] { "compares", "swaps" };
  }

}
