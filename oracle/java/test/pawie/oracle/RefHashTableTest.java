package pawie.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * A hash map and a hash set: chaining, the load factor, and rehashing.
 *
 * Bucket placement is checked directly rather than through iteration order,
 * because that is what the visualizer draws and what a student is asked to
 * reason about when a chain gets long.
 */
public class RefHashTableTest {

  private static RefHashTable map() {
    return new RefHashTable(false, "•");
  }

  private static RefHashTable set() {
    return new RefHashTable(true, "•");
  }

  @Test
  public void putThenGetReturnsTheValue() {
    RefHashTable m = map();
    assertNull("a new key has no previous value", m.put(1, 10));
    assertEquals(Integer.valueOf(10), m.get(1));
    assertEquals(1, m.size());
  }

  @Test
  public void puttingTheSameKeyAgainOverwritesAndReturnsTheOldValue() {
    RefHashTable m = map();
    m.put(1, 10);
    assertEquals(Integer.valueOf(10), m.put(1, 20));
    assertEquals(Integer.valueOf(20), m.get(1));
    assertEquals("an overwrite is not a new entry", 1, m.size());
  }

  @Test
  public void missingKeysReadBackAsNull() {
    RefHashTable m = map();
    m.put(1, 10);
    assertNull(m.get(2));
    assertNull("removing something absent returns null", m.remove(2));
  }

  @Test
  public void removeTakesTheEntryOutAndReturnsItsValue() {
    RefHashTable m = map();
    m.put(1, 10);
    assertEquals(Integer.valueOf(10), m.remove(1));
    assertEquals(0, m.size());
    assertNull(m.get(1));
  }

  @Test
  public void nullKeysAndValuesAreRefused() {
    RefHashTable m = map();
    try {
      m.put(null, 1);
      fail("a null key is not allowed");
    } catch (NullPointerException expected) {
      assertEquals(0, m.size());
    }
    try {
      m.put(1, null);
      fail("a null value is not allowed");
    } catch (NullPointerException expected) {
      assertEquals(0, m.size());
    }
  }

  @Test
  public void collidingKeysShareABucketAndBothSurvive() {
    RefHashTable m = new RefHashTable(false, "•", 5);
    m.put(1, 10);
    m.put(6, 60);                        // 1 and 6 both land in bucket 1 of 5
    assertEquals(Values.bucket(1, 5), Values.bucket(6, 5));
    assertEquals(Integer.valueOf(10), m.get(1));
    assertEquals(Integer.valueOf(60), m.get(6));
    assertEquals(2, m.size());
  }

  @Test
  public void theTableGrowsAtEightyPercentLoad() {
    RefHashTable m = new RefHashTable(false, "•", 5);
    m.put(1, 1);
    m.put(2, 2);
    m.put(3, 3);
    assertEquals("three of five is under the limit", 5, m.getCapacity());
    m.put(4, 4);                          // four of five reaches 0.8, so the next put grows
    assertEquals(5, m.getCapacity());
    m.put(5, 5);
    assertEquals(10, m.getCapacity());
    assertEquals(5, m.size());
  }

  @Test
  public void rehashingKeepsEveryEntryAndPutsItInTheRightBucket() {
    RefHashTable m = new RefHashTable(false, "•", 5);
    for (int i = 0; i < 12; i++) m.put(i, i * 10);
    assertEquals(12, m.size());
    assertEquals(12, m.countEntries());
    assertTrue(m.everyKeyIsInItsBucket());
    for (int i = 0; i < 12; i++) assertEquals(Integer.valueOf(i * 10), m.get(i));
  }

  @Test
  public void stringKeysHashTheSameWayJavaDoes() {
    RefHashTable m = new RefHashTable(false, "•", 7);
    m.put("cat", 1);
    assertEquals("the reference has to agree with String.hashCode",
        Math.abs("cat".hashCode() % 7), Values.bucket("cat", 7));
    assertEquals(Integer.valueOf(1), m.get("cat"));
  }

  @Test
  public void clearEmptiesTheTableButKeepsItsCapacity() {
    RefHashTable m = new RefHashTable(false, "•", 5);
    for (int i = 0; i < 6; i++) m.put(i, i);
    int capacity = m.getCapacity();
    m.clear();
    assertEquals(0, m.size());
    assertEquals(0, m.countEntries());
    assertEquals(capacity, m.getCapacity());
  }

  @Test
  public void addingToASetReportsWhetherItWasNew() {
    RefHashTable s = set();
    assertEquals(Boolean.TRUE, s.put("cat", null));
    assertEquals("the second add finds it already there", Boolean.FALSE, s.put("cat", null));
    assertEquals(1, s.size());
    assertEquals(Boolean.TRUE, s.get("cat"));
    assertEquals(Boolean.FALSE, s.get("dog"));
  }

  @Test
  public void removingFromASetReportsWhetherItWasThere() {
    RefHashTable s = set();
    s.put("cat", null);
    assertEquals(Boolean.TRUE, s.remove("cat"));
    assertEquals(Boolean.FALSE, s.remove("cat"));
    assertTrue(s.isEmpty());
  }

  @Test
  public void lookingUpAKeyCostsOneProbePerNodeInItsChain() {
    RefHashTable m = new RefHashTable(false, "•", 5);
    m.put(1, 1);
    m.put(6, 6);                          // same bucket as 1
    m.beginOperation();
    m.get(6);
    assertEquals("the chain is walked from the front", 2,
        ((Number) m.cost().get("probes")).intValue());
  }

  @Test
  public void anEmptyBucketCostsNoProbesAtAll() {
    RefHashTable m = new RefHashTable(false, "•", 5);
    m.beginOperation();
    assertNull(m.get(3));
    assertEquals(0, ((Number) m.cost().get("probes")).intValue());
    assertFalse(m.everyKeyIsInItsBucket() == false);
  }
}
