package pawie.oracle;

/**
 * The value domain shared by the visualizer and these reference structures:
 * whole numbers and short strings. Ordering and hashing are defined here once
 * so every structure agrees, and so they agree with the JavaScript engine.
 */
public final class Values {

  private Values() { }

  /**
   * Orders two elements. Two numbers compare numerically; anything else
   * compares as text. This mirrors the engine's comparator, which has to cope
   * with a student typing either 3 or "cat" into the same box.
   */
  public static int cmp(Object a, Object b) {
    if (a instanceof Integer && b instanceof Integer) {
      return Integer.compare((Integer) a, (Integer) b);
    }
    return String.valueOf(a).compareTo(String.valueOf(b));
  }

  /**
   * Java's own hashCode for the two types we store. Integer.hashCode is the int
   * itself and String.hashCode is the documented 31-polynomial, which is what
   * the engine reimplements in JavaScript.
   */
  public static int hash(Object key) {
    if (key instanceof Integer) return (Integer) key;
    return String.valueOf(key).hashCode();
  }

  /** The bucket a key belongs in, sign-stripped exactly as the classic formulation asks. */
  public static int bucket(Object key, int capacity) {
    return (hash(key) & 0x7fffffff) % capacity;
  }

  public static boolean eq(Object a, Object b) {
    return a == null ? b == null : a.equals(b);
  }
}
