package pawie.oracle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A doubly linked list with head and tail sentinels.
 *
 * This is a real chain of nodes rather than a list-backed stand-in, so the
 * invariant tests can check the things that actually break in student code:
 * that next and prev agree in both directions, that the sentinels stay put,
 * and that the stored size matches the number of nodes you can walk to.
 */
public class RefLinkedList extends RefStructure {

  static final class Node {
    Object value;
    Node prev;
    Node next;
    Node(Object value) { this.value = value; }
  }

  private final Node head = new Node(null);   // sentinel, never holds data
  private final Node tail = new Node(null);   // sentinel, never holds data
  private int size;

  public RefLinkedList() {
    head.next = tail;
    tail.prev = head;
  }

  @Override
  public Object state() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("vals", values());
    return m;
  }

  public List<Object> values() {
    List<Object> out = new ArrayList<>();
    for (Node n = head.next; n != tail; n = n.next) out.add(n.value);
    return out;
  }

  public int size() { return size; }

  public boolean isEmpty() { return size == 0; }

  /** Walks to the node at an index, counting one hop per link followed. */
  private Node nodeAt(int index) {
    Node curr = head;
    for (int i = 0; i <= index; i++) {
      curr = curr.next;
      count("hops");
    }
    return curr;
  }

  public void add(int index, Object value) {
    if (value == null) throw new NullPointerException("a node cannot hold null");
    if (index < 0 || index > size) {
      throw new IndexOutOfBoundsException("add allows [0, " + size + "], got " + index);
    }
    Node before = head;
    for (int i = 0; i < index; i++) {
      before = before.next;
      count("hops");
    }
    Node after = before.next;
    Node fresh = new Node(value);
    // The order matters: curr.next is rewired last, because the two writes
    // before it still need it to find the old right-hand neighbour.
    after.prev = fresh;   count("links");
    fresh.next = after;   count("links");
    fresh.prev = before;  count("links");
    before.next = fresh;  count("links");
    size++;
    snap();
  }

  public void append(Object value) {
    add(size, value);
  }

  public Object remove(int index) {
    rangeCheck(index);
    Node target = nodeAt(index);
    target.prev.next = target.next;  count("links");
    target.next.prev = target.prev;  count("links");
    target.next = null;              count("links");
    target.prev = null;              count("links");
    size--;
    snap();
    return target.value;
  }

  public Object set(int index, Object value) {
    if (value == null) throw new NullPointerException("a node cannot hold null");
    rangeCheck(index);
    Node target = nodeAt(index);
    Object old = target.value;
    target.value = value;
    snap();
    return old;
  }

  public Object get(int index) {
    rangeCheck(index);
    return nodeAt(index).value;
  }

  /** Drops the whole chain by repointing the sentinels. No walking, O(1). */
  public void clear() {
    if (size == 0) return;
    Node first = head.next;
    Node last = tail.prev;
    first.prev = null;   count("links");
    last.next = null;    count("links");
    head.next = tail;    count("links");
    tail.prev = head;    count("links");
    size = 0;
    snap();
  }

  private void rangeCheck(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("allows [0, " + (size - 1) + "], got " + index);
    }
  }

  /* ---------------- invariants, used by the property tests ---------------- */

  /** True when every next link is mirrored by a prev link and size is honest. */
  public boolean linksAreConsistent() {
    int forward = 0;
    for (Node n = head.next; n != tail; n = n.next) {
      if (n.next == null || n.next.prev != n) return false;
      forward++;
      if (forward > size + 1) return false;         // a cycle
    }
    int backward = 0;
    for (Node n = tail.prev; n != head; n = n.prev) {
      if (n.prev == null || n.prev.next != n) return false;
      backward++;
      if (backward > size + 1) return false;
    }
    return forward == size && backward == size && head.prev == null && tail.next == null;
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
      case "clear":
        clear();
        return null;
      case "size":
        return size();
      case "isEmpty":
        return isEmpty();
      default:
        throw new IllegalArgumentException("unknown LinkedList operation: " + op);
    }
  }

  @Override
  protected String[] counterNames() {
    return new String[] { "hops", "links" };
  }

}
