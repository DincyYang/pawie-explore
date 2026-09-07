package pawie.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

/**
 * Properties of the step traces themselves.
 *
 * The traces are what the differential test compares, so they have to be
 * trustworthy on their own: anchored at both ends, made of independent
 * snapshots rather than views onto live state, and empty of changes when an
 * operation was refused. A trace that shares an array with the structure would
 * quietly rewrite history every time the structure changed.
 */
public class TraceIntegrityTest {

  private static List<Map<String, Object>> allStructures() {
    List<Map<String, Object>> jobs = new ArrayList<>();
    for (String name : Arrays.asList("ArrayList", "LinkedList", "ListIterator", "Stack",
        "Queue", "MinHeap", "BST", "HashMap", "HashSet")) {
      Map<String, Object> job = new LinkedHashMap<>();
      job.put("structure", name);
      jobs.add(job);
    }
    return jobs;
  }

  private static List<Object> op(String name, Object... args) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("name", name);
    m.put("args", new ArrayList<>(Arrays.asList(args)));
    return new ArrayList<>(Arrays.asList((Object) m));
  }

  @Test
  public void everyTraceStartsFromTheStateTheOperationBeganIn() {
    RefArrayList list = new RefArrayList();
    list.append(1);
    list.append(2);
    list.beginOperation();
    Object before = list.states().get(0);
    list.add(0, 9);
    assertEquals("the first snapshot is the state before anything happened",
        Json.write(before), Json.write(list.states().get(0)));
    assertEquals("the last snapshot is the state the operation left behind",
        Json.write(list.state()), Json.write(list.states().get(list.states().size() - 1)));
  }

  @Test
  public void snapshotsAreCopiesRatherThanViews() {
    RefArrayList list = new RefArrayList();
    list.beginOperation();
    list.append(1);
    List<String> recorded = new ArrayList<>();
    for (Object s : list.states()) recorded.add(Json.write(s));

    for (int i = 0; i < 20; i++) list.append(i);      // plenty of growth and shifting

    List<String> now = new ArrayList<>();
    for (int i = 0; i < recorded.size(); i++) now.add(Json.write(list.states().get(i)));
    assertEquals("earlier snapshots must not change when the structure moves on",
        recorded, now);
  }

  @Test
  public void aRefusedOperationRecordsNoChange() {
    RefArrayList list = new RefArrayList();
    list.append(1);
    list.beginOperation();
    try {
      list.add(7, 9);
    } catch (IndexOutOfBoundsException expected) {
      // the point of the test is what the trace looks like afterwards
    }
    assertEquals("a refused call snapshots nothing beyond the state it started in",
        1, list.states().size());
    assertEquals(1, list.size());
  }

  @Test
  public void countersResetBetweenOperations() {
    RefArrayList list = new RefArrayList();
    for (int i = 0; i < 6; i++) list.append(i);       // this run includes a resize
    list.beginOperation();
    list.append(99);
    assertEquals("a plain append copies nothing", 0,
        ((Number) list.cost().get("copies")).intValue());
    assertEquals("and shifts nothing", 0, ((Number) list.cost().get("shifts")).intValue());
    assertTrue(((Number) list.cost().get("writes")).intValue() > 0);
  }

  @Test
  public void everyStructureReportsAllOfItsCountersEveryTime() {
    for (Map<String, Object> job : allStructures()) {
      RefStructure ref = Runner.create(job);
      ref.beginOperation();
      for (Map.Entry<String, Object> e : ref.cost().entrySet()) {
        assertEquals(job.get("structure") + " counter " + e.getKey()
            + " should start each operation at zero", 0, ((Number) e.getValue()).intValue());
      }
      assertTrue(job.get("structure") + " reports no counters at all", ref.cost().size() > 0);
    }
  }

  @Test
  public void theRunnerReplaysASequenceIntoStepsWithStatesAndCosts() {
    Map<String, Object> job = new LinkedHashMap<>();
    job.put("structure", "ArrayList");
    job.put("ops", op("append", 7));
    Map<String, Object> out = Runner.run(job);

    @SuppressWarnings("unchecked")
    List<Object> steps = (List<Object>) out.get("steps");
    assertEquals(1, steps.size());
    @SuppressWarnings("unchecked")
    Map<String, Object> step = (Map<String, Object>) steps.get(0);
    assertEquals("append", step.get("name"));
    assertEquals(null, step.get("error"));
    @SuppressWarnings("unchecked")
    List<Object> states = (List<Object>) step.get("states");
    assertTrue("an append moves through more than one state", states.size() > 1);
    assertEquals(Json.write(step.get("finalState")), Json.write(out.get("final")));
  }

  @Test
  public void theRunnerReportsRefusalsInsteadOfCrashing() {
    Map<String, Object> job = new LinkedHashMap<>();
    job.put("structure", "ArrayList");
    job.put("ops", op("remove", 4));
    Map<String, Object> out = Runner.run(job);

    @SuppressWarnings("unchecked")
    List<Object> steps = (List<Object>) out.get("steps");
    @SuppressWarnings("unchecked")
    Map<String, Object> step = (Map<String, Object>) steps.get(0);
    assertEquals("IndexOutOfBoundsException", step.get("error"));
  }

  @Test
  public void replayingTheSameSequenceGivesTheSameTrace() {
    Random r = new Random(4242);
    List<Object> ops = new ArrayList<>();
    for (int i = 0; i < 40; i++) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("name", r.nextInt(3) == 0 ? "remove" : "append");
      m.put("args", m.get("name").equals("remove")
          ? new ArrayList<>(Arrays.asList((Object) r.nextInt(5)))
          : new ArrayList<>(Arrays.asList((Object) r.nextInt(50))));
      ops.add(m);
    }
    Map<String, Object> job = new LinkedHashMap<>();
    job.put("structure", "ArrayList");
    job.put("ops", ops);

    assertEquals("the reference has to be deterministic to be an oracle at all",
        Json.write(Runner.run(job)), Json.write(Runner.run(job)));
  }

  @Test
  public void jsonSurvivesARoundTrip() {
    Map<String, Object> job = new LinkedHashMap<>();
    job.put("structure", "HashMap");
    job.put("ops", op("put", "a \"quoted\" key", 1));
    String written = Json.write(Runner.run(job));
    assertNotEquals(0, written.length());
    Object reparsed = Json.parse(written);
    assertEquals(written, Json.write(reparsed));
  }
}
