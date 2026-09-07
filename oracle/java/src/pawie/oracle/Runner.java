package pawie.oracle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The oracle's command line face: reads one job from stdin, replays it against
 * the matching reference structure, and prints the resulting step trace as JSON.
 *
 * A job looks like
 *   {"structure":"ArrayList","capacity":5,"ops":[{"name":"add","args":[0,7]}]}
 *
 * and comes back as
 *   {"steps":[{"name":"add","args":[0,7],"states":[...],"cost":{...},
 *              "ret":null,"error":null}], "final":{...}}
 *
 * One process handles one job so a failing case can be reproduced by hand:
 *   echo '<job>' | java -cp out pawie.oracle.Runner
 */
public final class Runner {

  private Runner() { }

  public static RefStructure create(Map<String, Object> job) {
    String structure = String.valueOf(job.get("structure"));
    Object capacity = job.get("capacity");
    Object setPlaceholder = job.containsKey("setPlaceholder") ? job.get("setPlaceholder") : "•";
    @SuppressWarnings("unchecked")
    List<Object> initial = (List<Object>) job.get("initial");

    switch (structure) {
      case "ArrayList":
        if (initial != null) return new RefArrayList(initial);
        return capacity == null ? new RefArrayList()
                                : new RefArrayList(((Number) capacity).intValue());
      case "LinkedList": {
        RefLinkedList list = new RefLinkedList();
        if (initial != null) for (Object v : initial) list.append(v);
        list.beginOperation();
        return list;
      }
      case "ListIterator":
        return new RefListIterator(initial);
      case "Stack":
      case "Queue": {
        RefDeque deque = capacity == null ? new RefDeque()
                                          : new RefDeque(((Number) capacity).intValue());
        if (initial != null) for (Object v : initial) deque.addLast(v);
        deque.beginOperation();
        return deque;
      }
      case "MinHeap":
        return new RefMinHeap(initial);
      case "BST": {
        RefBST tree = new RefBST();
        if (initial != null) for (Object k : initial) tree.insert(k);
        tree.beginOperation();
        return tree;
      }
      case "HashMap":
      case "HashSet": {
        boolean isSet = structure.equals("HashSet");
        RefHashTable table = capacity == null
            ? new RefHashTable(isSet, setPlaceholder)
            : new RefHashTable(isSet, setPlaceholder, ((Number) capacity).intValue());
        if (initial != null) {
          for (Object entry : initial) {
            if (isSet) {
              table.put(entry, setPlaceholder);
            } else {
              @SuppressWarnings("unchecked")
              List<Object> pair = (List<Object>) entry;
              table.put(pair.get(0), pair.get(1));
            }
          }
        }
        table.beginOperation();
        return table;
      }
      default:
        throw new IllegalArgumentException("unknown structure: " + structure);
    }
  }

  /**
   * The visualizer names some operations after the wrapper class rather than
   * the deque underneath, so peek means the rear on a stack and the front on a
   * queue. Everything else passes straight through.
   */
  private static String resolve(String structure, String op) {
    if (structure.equals("Queue") && op.equals("peek")) return "peekFirst";
    return op;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> run(Map<String, Object> job) {
    String structure = String.valueOf(job.get("structure"));
    RefStructure ref = create(job);
    List<Object> ops = (List<Object>) job.getOrDefault("ops", new ArrayList<>());

    List<Object> steps = new ArrayList<>();
    for (Object rawOp : ops) {
      Map<String, Object> op = (Map<String, Object>) rawOp;
      String name = String.valueOf(op.get("name"));
      List<Object> args = (List<Object>) op.getOrDefault("args", new ArrayList<>());

      ref.beginOperation();
      Object ret = null;
      String error = null;
      try {
        ret = ref.invoke(resolve(structure, name), args);
      } catch (RuntimeException e) {
        error = e.getClass().getSimpleName();
      }

      Map<String, Object> step = new LinkedHashMap<>();
      step.put("name", name);
      step.put("args", args);
      step.put("states", new ArrayList<>(ref.states()));
      step.put("cost", new LinkedHashMap<>(ref.cost()));
      step.put("finalState", ref.state());
      step.put("ret", ret);
      step.put("error", error);
      steps.add(step);
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("structure", structure);
    out.put("steps", steps);
    out.put("final", ref.state());
    return out;
  }

  /**
   * Accepts either a single job or {"jobs":[...]}. Batching matters: the
   * randomized suites replay thousands of sequences, and paying JVM startup
   * once instead of once per sequence is the difference between a test run you
   * wait for and one you skip.
   */
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws IOException {
    Object parsed = Json.parse(readAll(System.in));
    Map<String, Object> input = (Map<String, Object>) parsed;
    if (input.containsKey("jobs")) {
      List<Object> results = new ArrayList<>();
      for (Object job : (List<Object>) input.get("jobs")) {
        results.add(run((Map<String, Object>) job));
      }
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("results", results);
      System.out.println(Json.write(out));
      return;
    }
    System.out.println(Json.write(run(input)));
  }

  private static String readAll(InputStream in) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[8192];
    int n;
    while ((n = in.read(chunk)) > 0) buffer.write(chunk, 0, n);
    return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
  }
}
