package pawie.oracle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A hash map and a hash set over separate chaining, one chain per bucket,
 * new entries appended at the end of their chain.
 *
 * The table doubles when size reaches 0.8 of capacity, checked before the
 * insert rather than after, and rehashing walks the old table bucket by bucket
 * so the resulting chain order is deterministic. That order is not incidental:
 * it is what the visualizer draws, so the differential test compares full
 * bucket contents rather than just which keys are present.
 *
 * A set is the same table with a placeholder value, which is the usual way a
 * hash set is built on top of a hash map.
 */
public class RefHashTable extends RefStructure {

  public static final int DEFAULT_CAPACITY = 5;
  public static final double MAX_LOAD = 0.8;

  static final class Entry {
    final Object key;
    Object value;
    Entry(Object key, Object value) { this.key = key; this.value = value; }
  }

  private List<List<Entry>> table;
  private int size;
  private final boolean isSet;
  private final Object setPlaceholder;

  public RefHashTable(boolean isSet, Object setPlaceholder) {
    this(isSet, setPlaceholder, DEFAULT_CAPACITY);
  }

  public RefHashTable(boolean isSet, Object setPlaceholder, int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
    this.isSet = isSet;
    this.setPlaceholder = setPlaceholder;
    table = freshTable(capacity);
  }

  private static List<List<Entry>> freshTable(int capacity) {
    List<List<Entry>> t = new ArrayList<>(capacity);
    for (int i = 0; i < capacity; i++) t.add(new ArrayList<>());
    return t;
  }

  @Override
  public Object state() {
    List<Object> buckets = new ArrayList<>();
    for (List<Entry> chain : table) {
      List<Object> out = new ArrayList<>();
      for (Entry e : chain) {
        List<Object> pair = new ArrayList<>();
        pair.add(e.key);
        pair.add(e.value);
        out.add(pair);
      }
      buckets.add(out);
    }
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("table", buckets);
    m.put("size", size);
    return m;
  }

  public int size() { return size; }

  public boolean isEmpty() { return size == 0; }

  public int getCapacity() { return table.size(); }

  /** put(key, value) for a map, add(element) for a set. */
  public Object put(Object key, Object value) {
    if (key == null) throw new NullPointerException("a null key is not allowed");
    if (!isSet && value == null) throw new NullPointerException("a null value is not allowed");
    if (size >= MAX_LOAD * table.size()) expandCapacity();

    List<Entry> chain = table.get(Values.bucket(key, table.size()));
    int found = probeFor(chain, key);
    if (found >= 0) {
      Object old = chain.get(found).value;
      if (isSet) return Boolean.FALSE;          // the element was already there
      chain.get(found).value = value;
      count("writes");
      snap();
      return old;
    }
    chain.add(new Entry(key, isSet ? setPlaceholder : value));
    count("writes");
    size++;
    snap();
    return isSet ? Boolean.TRUE : null;
  }

  /** get(key) for a map, contains(element) for a set. */
  public Object get(Object key) {
    if (key == null) throw new NullPointerException("a null key is not allowed");
    List<Entry> chain = table.get(Values.bucket(key, table.size()));
    int found = probeFor(chain, key);
    if (isSet) return found >= 0;
    return found >= 0 ? chain.get(found).value : null;
  }

  public Object remove(Object key) {
    if (key == null) throw new NullPointerException("a null key is not allowed");
    List<Entry> chain = table.get(Values.bucket(key, table.size()));
    int found = probeFor(chain, key);
    if (found < 0) return isSet ? Boolean.FALSE : null;
    Object removed = chain.get(found).value;
    chain.remove(found);
    count("writes");
    size--;
    snap();
    return isSet ? Boolean.TRUE : removed;
  }

  public void clear() {
    for (int i = 0; i < table.size(); i++) table.set(i, new ArrayList<>());
    size = 0;
    snap();
  }

  /** Walks one chain, counting a probe per key comparison. */
  private int probeFor(List<Entry> chain, Object key) {
    for (int i = 0; i < chain.size(); i++) {
      count("probes");
      if (Values.eq(chain.get(i).key, key)) return i;
    }
    return -1;
  }

  private void expandCapacity() {
    int newCapacity = table.size() * 2;
    List<List<Entry>> grown = freshTable(newCapacity);
    for (List<Entry> chain : table) {
      for (Entry e : chain) {
        grown.get(Values.bucket(e.key, newCapacity)).add(e);
        count("rehashes");
      }
    }
    table = grown;
    snap();
  }

  /** Every key sits in the bucket its hash says it should. */
  public boolean everyKeyIsInItsBucket() {
    for (int b = 0; b < table.size(); b++) {
      for (Entry e : table.get(b)) {
        if (Values.bucket(e.key, table.size()) != b) return false;
      }
    }
    return true;
  }

  public int countEntries() {
    int n = 0;
    for (List<Entry> chain : table) n += chain.size();
    return n;
  }

  @Override
  public Object invoke(String op, List<Object> args) {
    switch (op) {
      case "put": return put(args.get(0), args.get(1));
      case "add": return put(args.get(0), setPlaceholder);
      case "get": return get(args.get(0));
      case "contains": return get(args.get(0));
      case "remove": return remove(args.get(0));
      case "size": return size();
      case "isEmpty": return isEmpty();
      case "clear": clear(); return null;
      case "getCapacity": return getCapacity();
      default:
        throw new IllegalArgumentException("unknown hash table operation: " + op);
    }
  }

  @Override
  protected String[] counterNames() {
    return new String[] { "probes", "writes", "rehashes" };
  }

}
