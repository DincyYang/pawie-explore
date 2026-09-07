package pawie.oracle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An array-backed list: an Object[] with a size field, a default capacity of
 * 5, and doubling growth.
 *
 * The two shift helpers are written as copies rather than moves, because that is
 * the behaviour a learner has to reason about: a copy leaves the source slot holding a leftover duplicate
 * until the next copy overwrites it, and only one slot is ever explicitly
 * nulled.
 */
public class RefArrayList extends RefStructure {

  public static final int DEFAULT_CAPACITY = 5;
  public static final int GROWTH_FACTOR = 2;

  private Object[] data;
  private int size;

  public RefArrayList() {
    this(DEFAULT_CAPACITY);
  }

  public RefArrayList(int capacity) {
    if (capacity < 0) throw new IllegalArgumentException("capacity must not be negative");
    data = new Object[capacity];
    size = 0;
  }

  /** The array constructor: clone the input and take its length as capacity. */
  public RefArrayList(List<Object> initial) {
    if (initial == null) throw new NullPointerException("array must not be null");
    data = initial.toArray();
    size = data.length;
  }

  @Override
  public Object state() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("data", new ArrayList<>(Arrays.asList(data)));
    m.put("size", size);
    return m;
  }

  public int size() { return size; }

  public int getCapacity() { return data.length; }

  public Object get(int index) {
    rangeCheckRead(index);
    return data[index];
  }

  public Object set(int index, Object value) {
    rangeCheckRead(index);
    Object old = data[index];
    data[index] = value;
    count("writes");
    snap();
    return old;
  }

  public void add(int index, Object value) {
    if (index < 0 || index > size) {
      throw new IndexOutOfBoundsException("add allows [0, " + size + "], got " + index);
    }
    if (size == data.length) expandCapacity();
    shiftRight(index);
    data[index] = value;
    count("writes");
    snap();
    size++;
    snap();
  }

  public void append(Object value) {
    add(size, value);
  }

  public Object remove(int index) {
    rangeCheckRead(index);
    Object removed = data[index];
    shiftLeft(index);
    size--;
    snap();
    return removed;
  }

  /* ---------------- internals, instrumented ---------------- */

  private void rangeCheckRead(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("allows [0, " + (size - 1) + "], got " + index);
    }
  }

  private void expandCapacity() {
    int oldCapacity = data.length;
    int newCapacity = oldCapacity == 0 ? DEFAULT_CAPACITY : oldCapacity * GROWTH_FACTOR;
    if (newCapacity < size + 1) newCapacity = size + 1;
    Object[] grown = new Object[newCapacity];
    for (int i = 0; i < oldCapacity; i++) {
      grown[i] = data[i];
      count("copies");
      count("writes");
      snap();                     // data still points at the old array here
    }
    data = grown;                 // the one moment the object's state changes
    snap();
  }

  /** Copies each slot from its left neighbour, back to front, then clears the gap. */
  private void shiftRight(int index) {
    for (int i = data.length - 1; i > index; i--) {
      data[i] = data[i - 1];
      count("writes");
      if (i - 1 < size) count("shifts");
      snap();
    }
    data[index] = null;
    count("writes");
    snap();
  }

  /** Copies each slot from its right neighbour, front to back, then clears the tail. */
  private void shiftLeft(int index) {
    for (int i = index + 1; i < data.length; i++) {
      data[i - 1] = data[i];
      count("writes");
      if (i < size) count("shifts");
      snap();
    }
    data[data.length - 1] = null;
    count("writes");
    snap();
  }

  @Override
  public Object invoke(String op, List<Object> args) {
    switch (op) {
      case "add":
        add(((Number) args.get(0)).intValue(), args.get(1));
        return null;
      case "append":
        append(args.get(0));
        return null;
      case "remove":
        return remove(((Number) args.get(0)).intValue());
      case "set":
        return set(((Number) args.get(0)).intValue(), args.get(1));
      case "get":
        return get(((Number) args.get(0)).intValue());
      case "size":
        return size();
      case "getCapacity":
        return getCapacity();
      default:
        throw new IllegalArgumentException("unknown ArrayList operation: " + op);
    }
  }

  @Override
  protected String[] counterNames() {
    return new String[] { "writes", "shifts", "copies" };
  }

}
