package pawie.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.NoSuchElementException;
import org.junit.Test;

/**
 * A list iterator: the cursor sits between two nodes, and the rules about when
 * set() and remove() are allowed are the part that trips people up.
 */
public class RefListIteratorTest {

  private static RefListIterator on(Object... values) {
    return new RefListIterator(Arrays.asList(values));
  }

  @Test
  public void aFreshCursorSitsBeforeEverything() {
    RefListIterator it = on(1, 2, 3);
    assertTrue(it.hasNext());
    assertFalse(it.hasPrevious());
    assertEquals(0, it.nextIndex());
    assertEquals(-1, it.previousIndex());
  }

  @Test
  public void nextWalksForwardsAndPreviousWalksBack() {
    RefListIterator it = on(1, 2, 3);
    assertEquals(Integer.valueOf(1), it.next());
    assertEquals(Integer.valueOf(2), it.next());
    assertEquals(Integer.valueOf(2), it.previous());
    assertEquals(1, it.nextIndex());
  }

  @Test
  public void nextAtTheEndThrows() {
    RefListIterator it = on(1);
    it.next();
    try {
      it.next();
      fail("there is nothing to the right of the cursor");
    } catch (NoSuchElementException expected) {
      assertEquals(1, it.nextIndex());
    }
  }

  @Test
  public void previousAtTheFrontThrows() {
    RefListIterator it = on(1);
    try {
      it.previous();
      fail("there is nothing to the left of the cursor");
    } catch (NoSuchElementException expected) {
      assertEquals(0, it.nextIndex());
    }
  }

  @Test
  public void setWithoutAMovePendingIsIllegal() {
    RefListIterator it = on(1, 2);
    try {
      it.set(9);
      fail("set needs a next() or previous() to act on");
    } catch (IllegalStateException expected) {
      assertEquals(Arrays.asList(1, 2), listOf(it));
    }
  }

  @Test
  public void setChangesTheNodeTheLastMoveReturned() {
    RefListIterator it = on(1, 2, 3);
    it.next();
    it.next();
    assertEquals(Integer.valueOf(2), it.set(9));
    assertEquals(Arrays.asList(1, 9, 3), listOf(it));
  }

  @Test
  public void afterPreviousSetChangesTheNodeOnTheRight() {
    RefListIterator it = on(1, 2, 3);
    it.next();
    it.next();
    it.previous();                      // returned the 2 again, moving backwards
    it.set(9);
    assertEquals("going backwards, the last returned node is the one on the right",
        Arrays.asList(1, 9, 3), listOf(it));
  }

  @Test
  public void addSplicesAtTheCursorAndLeavesItJustPast() {
    RefListIterator it = on(1, 3);
    it.next();
    it.add(2);
    assertEquals(Arrays.asList(1, 2, 3), listOf(it));
    assertEquals(2, it.nextIndex());
    assertEquals(Integer.valueOf(3), it.next());
  }

  @Test
  public void removeIsNotAllowedStraightAfterAdd() {
    RefListIterator it = on(1);
    it.next();
    it.add(2);
    try {
      it.remove();
      fail("add() clears the pending move, so remove() has nothing to delete");
    } catch (IllegalStateException expected) {
      assertEquals(Arrays.asList(1, 2), listOf(it));
    }
  }

  @Test
  public void removeAfterNextClosesTheCursorUp() {
    RefListIterator it = on(1, 2, 3);
    it.next();
    it.next();
    assertEquals(Integer.valueOf(2), it.remove());
    assertEquals(Arrays.asList(1, 3), listOf(it));
    assertEquals(1, it.nextIndex());
    assertEquals(Integer.valueOf(3), it.next());
  }

  @Test
  public void removeAfterPreviousLeavesTheCursorWhereItIs() {
    RefListIterator it = on(1, 2, 3);
    it.next();
    it.next();
    it.previous();                      // last returned is the 2, on the right
    assertEquals(Integer.valueOf(2), it.remove());
    assertEquals(Arrays.asList(1, 3), listOf(it));
    assertEquals(1, it.nextIndex());
  }

  @Test
  public void twoRemovesInARowAreIllegal() {
    RefListIterator it = on(1, 2);
    it.next();
    it.remove();
    try {
      it.remove();
      fail("remove() clears the pending move as well");
    } catch (IllegalStateException expected) {
      assertEquals(Arrays.asList(2), listOf(it));
    }
  }

  @Test
  public void everyOperationIsConstantWork() {
    RefListIterator it = on(1, 2, 3, 4, 5, 6, 7, 8);
    for (int i = 0; i < 5; i++) it.next();
    it.beginOperation();
    it.add(99);
    assertEquals("splicing at the cursor never walks", 0, ((Number) it.cost().get("hops")).intValue());
    assertEquals(4, ((Number) it.cost().get("links")).intValue());
  }

  @SuppressWarnings("unchecked")
  private static java.util.List<Object> listOf(RefListIterator it) {
    java.util.Map<String, Object> state = (java.util.Map<String, Object>) it.state();
    return (java.util.List<Object>) state.get("vals");
  }
}
