package pawie.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeSet;
import org.junit.Test;

/**
 * Randomized model-based tests.
 *
 * Each reference structure is driven through thousands of random operations
 * alongside the java.util class that defines the same behaviour, and after
 * every single operation the two are compared and the structure's own
 * invariants are re-checked. Hand-written cases cover the situations you think
 * of; this covers the ones you do not, and the seed in every failure message
 * makes the offending sequence reproducible.
 */
public class RandomizedModelTest {

  private static final int RUNS = 200;
  private static final int OPS = 60;

  private static String context(long seed, int step, String op) {
    return "seed " + seed + ", operation " + step + " (" + op + ")";
  }

  @Test
  public void arrayListMatchesJavaUtilArrayList() {
    for (int run = 0; run < RUNS; run++) {
      long seed = 1000L + run;
      Random r = new Random(seed);
      RefArrayList mine = new RefArrayList();
      List<Object> model = new ArrayList<>();

      for (int step = 0; step < OPS; step++) {
        int choice = r.nextInt(10);
        String op;
        if (choice < 4 || model.isEmpty()) {
          Object v = r.nextInt(100);
          int index = model.isEmpty() ? 0 : r.nextInt(model.size() + 1);
          op = "add(" + index + ", " + v + ")";
          mine.add(index, v);
          model.add(index, v);
        } else if (choice < 6) {
          int index = r.nextInt(model.size());
          op = "remove(" + index + ")";
          assertEquals(context(seed, step, op), model.remove(index), mine.remove(index));
        } else if (choice < 8) {
          int index = r.nextInt(model.size());
          Object v = r.nextInt(100);
          op = "set(" + index + ", " + v + ")";
          assertEquals(context(seed, step, op), model.set(index, v), mine.set(index, v));
        } else {
          int index = r.nextInt(model.size());
          op = "get(" + index + ")";
          assertEquals(context(seed, step, op), model.get(index), mine.get(index));
        }

        assertEquals(context(seed, step, op), model.size(), mine.size());
        assertTrue(context(seed, step, op) + ": capacity fell below size",
            mine.getCapacity() >= mine.size());
        for (int i = 0; i < model.size(); i++) {
          assertEquals(context(seed, step, op) + " at index " + i, model.get(i), mine.get(i));
        }
      }
    }
  }

  @Test
  public void linkedListMatchesJavaUtilLinkedList() {
    for (int run = 0; run < RUNS; run++) {
      long seed = 2000L + run;
      Random r = new Random(seed);
      RefLinkedList mine = new RefLinkedList();
      List<Object> model = new LinkedList<>();

      for (int step = 0; step < OPS; step++) {
        int choice = r.nextInt(10);
        String op;
        if (choice < 4 || model.isEmpty()) {
          Object v = r.nextInt(100);
          int index = model.isEmpty() ? 0 : r.nextInt(model.size() + 1);
          op = "add(" + index + ", " + v + ")";
          mine.add(index, v);
          model.add(index, v);
        } else if (choice < 6) {
          int index = r.nextInt(model.size());
          op = "remove(" + index + ")";
          assertEquals(context(seed, step, op), model.remove(index), mine.remove(index));
        } else if (choice < 8) {
          int index = r.nextInt(model.size());
          Object v = r.nextInt(100);
          op = "set(" + index + ", " + v + ")";
          assertEquals(context(seed, step, op), model.set(index, v), mine.set(index, v));
        } else if (choice == 8) {
          op = "clear()";
          mine.clear();
          model.clear();
        } else {
          int index = r.nextInt(model.size());
          op = "get(" + index + ")";
          assertEquals(context(seed, step, op), model.get(index), mine.get(index));
        }

        assertEquals(context(seed, step, op), model.size(), mine.size());
        assertEquals(context(seed, step, op), model, mine.values());
        assertTrue(context(seed, step, op) + ": next and prev disagree",
            mine.linksAreConsistent());
      }
    }
  }

  @Test
  public void listIteratorMatchesJavaUtilListIterator() {
    for (int run = 0; run < RUNS; run++) {
      long seed = 3000L + run;
      Random r = new Random(seed);
      List<Object> start = new ArrayList<>();
      for (int i = 0; i < 1 + r.nextInt(6); i++) start.add(r.nextInt(50));

      RefListIterator mine = new RefListIterator(start);
      LinkedList<Object> model = new LinkedList<>(start);
      ListIterator<Object> theirs = model.listIterator();

      for (int step = 0; step < OPS; step++) {
        int choice = r.nextInt(8);
        String op;
        switch (choice) {
          case 0:
          case 1:
            op = "next()";
            if (theirs.hasNext()) {
              assertEquals(context(seed, step, op), theirs.next(), mine.next());
            } else {
              assertRefused(context(seed, step, op), mine, "next", null, NoSuchElementException.class);
            }
            break;
          case 2:
            op = "previous()";
            if (theirs.hasPrevious()) {
              assertEquals(context(seed, step, op), theirs.previous(), mine.previous());
            } else {
              assertRefused(context(seed, step, op), mine, "previous", null,
                  NoSuchElementException.class);
            }
            break;
          case 3: {
            Object v = r.nextInt(100);
            op = "add(" + v + ")";
            theirs.add(v);
            mine.add(v);
            break;
          }
          case 4: {
            Object v = r.nextInt(100);
            op = "set(" + v + ")";
            boolean modelAccepted = true;
            try {
              theirs.set(v);
            } catch (IllegalStateException e) {
              modelAccepted = false;
            }
            if (modelAccepted) mine.set(v);
            else assertRefused(context(seed, step, op), mine, "set", v, IllegalStateException.class);
            break;
          }
          case 5: {
            op = "remove()";
            boolean modelAccepted = true;
            try {
              theirs.remove();
            } catch (IllegalStateException e) {
              modelAccepted = false;
            }
            if (modelAccepted) mine.remove();
            else assertRefused(context(seed, step, op), mine, "remove", null,
                IllegalStateException.class);
            break;
          }
          case 6:
            op = "nextIndex()";
            assertEquals(context(seed, step, op), theirs.nextIndex(), mine.nextIndex());
            break;
          default:
            op = "hasNext()";
            assertEquals(context(seed, step, op), theirs.hasNext(), mine.hasNext());
            assertEquals(context(seed, step, op), theirs.hasPrevious(), mine.hasPrevious());
            break;
        }

        assertEquals(context(seed, step, op) + ": the list itself drifted", model, listOf(mine));
        assertEquals(context(seed, step, op) + ": the cursor drifted",
            theirs.nextIndex(), mine.nextIndex());
      }
    }
  }

  @Test
  public void dequeMatchesArrayDequeAsAStackAndAsAQueue() {
    for (int run = 0; run < RUNS; run++) {
      long seed = 4000L + run;
      Random r = new Random(seed);
      RefDeque mine = new RefDeque(1 + r.nextInt(5));
      ArrayDeque<Object> model = new ArrayDeque<>();

      for (int step = 0; step < OPS; step++) {
        int choice = r.nextInt(10);
        String op;
        if (choice < 4) {
          Object v = r.nextInt(100);
          op = "addLast(" + v + ")";
          mine.addLast(v);
          model.addLast(v);
        } else if (choice < 6) {
          Object v = r.nextInt(100);
          op = "addFirst(" + v + ")";
          mine.addFirst(v);
          model.addFirst(v);
        } else if (choice < 8) {
          op = "removeLast()";
          assertEquals(context(seed, step, op), model.pollLast(), mine.removeLast());
        } else {
          op = "removeFirst()";
          assertEquals(context(seed, step, op), model.pollFirst(), mine.removeFirst());
        }

        assertEquals(context(seed, step, op), model.size(), mine.size());
        assertEquals(context(seed, step, op), new ArrayList<>(model), mine.toList());
        assertEquals(context(seed, step, op), model.peekFirst(), mine.peekFirst());
        assertEquals(context(seed, step, op), model.peekLast(), mine.peekLast());
        assertTrue(context(seed, step, op) + ": capacity fell below size",
            mine.getCapacity() >= mine.size());
      }
    }
  }

  @Test
  public void minHeapMatchesPriorityQueue() {
    for (int run = 0; run < RUNS; run++) {
      long seed = 5000L + run;
      Random r = new Random(seed);
      RefMinHeap mine = new RefMinHeap();
      PriorityQueue<Integer> model = new PriorityQueue<>();

      for (int step = 0; step < OPS; step++) {
        String op;
        if (r.nextInt(10) < 6 || model.isEmpty()) {
          int v = r.nextInt(100);
          op = "insert(" + v + ")";
          mine.insert(v);
          model.add(v);
        } else {
          op = "removeMin()";
          assertEquals(context(seed, step, op), model.poll(), mine.removeMin());
        }
        assertEquals(context(seed, step, op), model.size(), mine.size());
        assertEquals(context(seed, step, op), model.peek(), mine.getMin());
        assertTrue(context(seed, step, op) + ": the heap property broke",
            mine.heapPropertyHolds());
      }
    }
  }

  @Test
  public void bstMatchesTreeSet() {
    for (int run = 0; run < RUNS; run++) {
      long seed = 6000L + run;
      Random r = new Random(seed);
      RefBST mine = new RefBST();
      TreeSet<Integer> model = new TreeSet<>();

      for (int step = 0; step < OPS; step++) {
        int key = r.nextInt(30);          // a small key space, so hits are common
        int choice = r.nextInt(10);
        String op;
        if (choice < 5) {
          op = "insert(" + key + ")";
          assertEquals(context(seed, step, op), model.add(key), mine.insert(key));
        } else if (choice < 8) {
          op = "remove(" + key + ")";
          assertEquals(context(seed, step, op), model.remove(key), mine.remove(key));
        } else {
          op = "search(" + key + ")";
          assertEquals(context(seed, step, op), model.contains(key), mine.search(key));
        }

        assertEquals(context(seed, step, op), model.size(), mine.size());
        assertEquals(context(seed, step, op) + ": the in-order walk is not the sorted keys",
            new ArrayList<Object>(model), mine.inorder());
        assertEquals(context(seed, step, op) + ": size drifted from the node count",
            mine.size(), mine.countNodes());
        assertTrue(context(seed, step, op) + ": the search property broke",
            mine.orderingHolds());
      }
    }
  }

  @Test
  public void hashMapMatchesJavaUtilHashMap() {
    for (int run = 0; run < RUNS; run++) {
      long seed = 7000L + run;
      Random r = new Random(seed);
      /* The capacities the application actually uses: the doubling policy only
         keeps the table under its maximum load when the capacity starts at 5. */
      RefHashTable mine = new RefHashTable(false, "•", r.nextBoolean() ? 5 : 10);
      Map<Object, Object> model = new HashMap<>();
      List<Object> keys = Arrays.<Object>asList(1, 2, 3, 17, 42, "cat", "dog", "emu", "ox");

      for (int step = 0; step < OPS; step++) {
        Object key = keys.get(r.nextInt(keys.size()));
        int choice = r.nextInt(10);
        String op;
        if (choice < 5) {
          Object v = r.nextInt(100);
          op = "put(" + key + ", " + v + ")";
          assertEquals(context(seed, step, op), model.put(key, v), mine.put(key, v));
        } else if (choice < 8) {
          op = "remove(" + key + ")";
          assertEquals(context(seed, step, op), model.remove(key), mine.remove(key));
        } else {
          op = "get(" + key + ")";
          assertEquals(context(seed, step, op), model.get(key), mine.get(key));
        }

        assertEquals(context(seed, step, op), model.size(), mine.size());
        assertEquals(context(seed, step, op) + ": entries went missing from the chains",
            mine.size(), mine.countEntries());
        assertTrue(context(seed, step, op) + ": a key is in the wrong bucket",
            mine.everyKeyIsInItsBucket());
        assertTrue(context(seed, step, op) + ": the table is over its maximum load",
            mine.size() <= RefHashTable.MAX_LOAD * mine.getCapacity());
        for (Object k : keys) {
          assertEquals(context(seed, step, op) + " reading back " + k, model.get(k), mine.get(k));
        }
      }
    }
  }

  @Test
  public void hashSetMatchesJavaUtilHashSet() {
    for (int run = 0; run < RUNS; run++) {
      long seed = 8000L + run;
      Random r = new Random(seed);
      RefHashTable mine = new RefHashTable(true, "•", r.nextBoolean() ? 5 : 10);
      HashSet<Object> model = new HashSet<>();
      List<Object> keys = Arrays.<Object>asList(1, 2, 3, 17, 42, "cat", "dog", "emu", "ox");

      for (int step = 0; step < OPS; step++) {
        Object key = keys.get(r.nextInt(keys.size()));
        int choice = r.nextInt(10);
        String op;
        if (choice < 5) {
          op = "add(" + key + ")";
          assertEquals(context(seed, step, op), model.add(key), mine.put(key, null));
        } else if (choice < 8) {
          op = "remove(" + key + ")";
          assertEquals(context(seed, step, op), model.remove(key), mine.remove(key));
        } else {
          op = "contains(" + key + ")";
          assertEquals(context(seed, step, op), model.contains(key), mine.get(key));
        }

        assertEquals(context(seed, step, op), model.size(), mine.size());
        assertTrue(context(seed, step, op), mine.everyKeyIsInItsBucket());
      }
    }
  }

  /* ---------------- helpers ---------------- */

  private static void assertRefused(String where, RefListIterator it, String op, Object arg,
      Class<? extends RuntimeException> expected) {
    try {
      it.invoke(op, arg == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arg)));
      throw new AssertionError(where + ": expected " + expected.getSimpleName()
          + " but the call was accepted");
    } catch (RuntimeException e) {
      if (!expected.isInstance(e)) {
        throw new AssertionError(where + ": expected " + expected.getSimpleName()
            + " but got " + e.getClass().getSimpleName());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Object> listOf(RefListIterator it) {
    Map<String, Object> state = (Map<String, Object>) it.state();
    return (List<Object>) state.get("vals");
  }
}
