package pawie.oracle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for the eight reference data structures.
 *
 * Every reference is instrumented: it snapshots its own committed state after
 * each field mutation, and counts the work it does (array writes, shifts,
 * resize copies, hops, pointer writes, comparisons, swaps, chain probes,
 * rehashes). The visualizer's step engine emits the same two things, which is
 * what makes a step-by-step differential test possible rather than a
 * final-answer-only one.
 *
 * A snapshot is taken of the state the object's own fields describe. During a
 * resize the field still points at the old array, so the snapshots repeat until
 * the pointer is swung across; the comparison collapses runs of identical
 * states on both sides, so the two engines only have to agree on the moments
 * where observable state actually changes.
 */
public abstract class RefStructure {

  private final List<Object> states = new ArrayList<>();
  private final Map<String, Object> cost = new LinkedHashMap<>();

  /** The committed state, as JSON-writable data. Shape is per structure. */
  public abstract Object state();

  /** Runs one operation. Illegal calls throw, exactly as the real class would. */
  public abstract Object invoke(String op, List<Object> args);

  protected void snap() {
    states.add(state());
  }

  protected void count(String key, int by) {
    cost.put(key, ((Number) cost.getOrDefault(key, 0)).intValue() + by);
  }

  protected void count(String key) {
    count(key, 1);
  }

  /**
   * The counters this structure reports. They are seeded to 0 at the start of
   * every operation so a step that does no work still says so explicitly,
   * rather than leaving the key out and making the comparison guess.
   */
  protected abstract String[] counterNames();

  public void beginOperation() {
    states.clear();
    cost.clear();
    for (String k : counterNames()) cost.put(k, 0);
    snap();                       // anchor every step trace at the pre-state
  }

  public List<Object> states() { return states; }

  public Map<String, Object> cost() { return cost; }
}
