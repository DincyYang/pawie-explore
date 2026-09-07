package pawie.oracle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A double-ended queue over a circular array with a front index and a rear index.
 *
 * A stack and a queue are both thin wrappers over this one class: push/pop work the rear end, enqueue/dequeue work
 * opposite ends, and neither ever shifts elements. A full array doubles and the
 * elements are re-based to start at index 0, so front and rear only wrap
 * between resizes.
 */
public class RefDeque extends RefStructure {

  public static final int DEFAULT_CAPACITY = 5;

  private Object[] data;
  private int size;
  private int front;
  private int rear;

  public RefDeque() {
    this(DEFAULT_CAPACITY);
  }

  public RefDeque(int capacity) {
    if (capacity < 0) throw new IllegalArgumentException("capacity must not be negative");
    data = new Object[capacity];
  }

  @Override
  public Object state() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("data", new ArrayList<>(Arrays.asList(data)));
    m.put("size", size);
    m.put("front", front);
    m.put("rear", rear);
    return m;
  }

  public int size() { return size; }

  public boolean isEmpty() { return size == 0; }

  public int getCapacity() { return data.length; }

  /** The elements in logical order, front first. */
  public List<Object> toList() {
    List<Object> out = new ArrayList<>();
    if (size == 0) return out;
    int i = front;
    for (int k = 0; k < size; k++) {
      out.add(data[i]);
      i = (i + 1) % data.length;
    }
    return out;
  }

  public void addLast(Object value) {
    if (value == null) throw new NullPointerException("a deque cannot hold null");
    if (size == data.length) expandCapacity();
    if (data[rear] != null) {           // an empty deque already has a free rear
      rear = (rear + 1) % data.length;
      snap();
    }
    data[rear] = value;
    count("writes");
    snap();
    size++;
    snap();
  }

  public void addFirst(Object value) {
    if (value == null) throw new NullPointerException("a deque cannot hold null");
    if (size == data.length) expandCapacity();
    if (data[front] != null) {
      front = (front + data.length - 1) % data.length;
      snap();
    }
    data[front] = value;
    count("writes");
    snap();
    size++;
    snap();
  }

  /** Removes the rear element. An empty deque returns null rather than throwing. */
  public Object removeLast() {
    if (size == 0) return null;
    Object removed = data[rear];
    data[rear] = null;
    count("writes");
    snap();
    if (rear != front) {                // otherwise that was the last element
      rear = (rear + data.length - 1) % data.length;
      snap();
    }
    size--;
    snap();
    return removed;
  }

  /** Removes the front element. An empty deque returns null rather than throwing. */
  public Object removeFirst() {
    if (size == 0) return null;
    Object removed = data[front];
    data[front] = null;
    count("writes");
    snap();
    if (front != rear) {
      front = (front + 1) % data.length;
      snap();
    }
    size--;
    snap();
    return removed;
  }

  public Object peekLast() {
    return size == 0 ? null : data[rear];
  }

  public Object peekFirst() {
    return size == 0 ? null : data[front];
  }

  /** Doubles the array, copying in logical order so the elements re-base at 0. */
  private void expandCapacity() {
    int oldCapacity = data.length;
    int newCapacity = oldCapacity == 0 ? 10 : oldCapacity * 2;
    Object[] grown = new Object[newCapacity];
    int f = front;
    for (int i = 0; i < size; i++) {
      grown[i] = data[f];
      count("copies");
      count("writes");
      snap();                            // the field still points at the old array
      f = (f + 1) % oldCapacity;
    }
    data = grown;
    front = 0;
    rear = size <= 1 ? 0 : size - 1;
    snap();
  }

  @Override
  public Object invoke(String op, List<Object> args) {
    switch (op) {
      // Stack: push and pop both work the rear end
      case "push": addLast(args.get(0)); return null;
      case "pop": return removeLast();
      case "peek": return peekLast();
      // Queue: elements join at the rear and leave from the front
      case "enqueue": addLast(args.get(0)); return null;
      case "dequeue": return removeFirst();
      case "peekFirst": return peekFirst();
      case "addFirst": addFirst(args.get(0)); return null;
      case "addLast": addLast(args.get(0)); return null;
      case "removeFirst": return removeFirst();
      case "removeLast": return removeLast();
      case "empty": return isEmpty();
      case "isEmpty": return isEmpty();
      case "size": return size();
      case "getCapacity": return getCapacity();
      default:
        throw new IllegalArgumentException("unknown deque operation: " + op);
    }
  }

  @Override
  protected String[] counterNames() {
    return new String[] { "writes", "copies" };
  }

}
