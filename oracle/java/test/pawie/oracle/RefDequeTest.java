package pawie.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import org.junit.Test;

/**
 * The deque, and with it the stack and the queue. The wrap-around is the whole point,
 * so most of these tests care about where front and rear end up, not just what
 * comes back out.
 */
public class RefDequeTest {

  @Test
  public void pushAndPopWorkTheSameEnd() {
    RefDeque stack = new RefDeque();
    stack.addLast(1);
    stack.addLast(2);
    stack.addLast(3);
    assertEquals(Integer.valueOf(3), stack.peekLast());
    assertEquals(Integer.valueOf(3), stack.removeLast());
    assertEquals(Integer.valueOf(2), stack.removeLast());
    assertEquals(1, stack.size());
  }

  @Test
  public void enqueueAndDequeueWorkOppositeEnds() {
    RefDeque queue = new RefDeque();
    queue.addLast(1);
    queue.addLast(2);
    queue.addLast(3);
    assertEquals(Integer.valueOf(1), queue.peekFirst());
    assertEquals(Integer.valueOf(1), queue.removeFirst());
    assertEquals(Integer.valueOf(2), queue.removeFirst());
    assertEquals(Arrays.asList(3), queue.toList());
  }

  @Test
  public void anEmptyDequeReturnsNullRatherThanThrowing() {
    RefDeque deque = new RefDeque();
    assertNull(deque.peekFirst());
    assertNull(deque.peekLast());
    assertNull(deque.removeFirst());
    assertNull(deque.removeLast());
    assertEquals(0, deque.size());
  }

  @Test
  public void nullIsRefusedAtBothEnds() {
    RefDeque deque = new RefDeque();
    try {
      deque.addLast(null);
      fail("a deque cannot hold null");
    } catch (NullPointerException expected) {
      assertEquals(0, deque.size());
    }
    try {
      deque.addFirst(null);
      fail("a deque cannot hold null");
    } catch (NullPointerException expected) {
      assertEquals(0, deque.size());
    }
  }

  @Test
  public void elementsWrapAroundTheEndOfTheArray() {
    RefDeque queue = new RefDeque(3);
    queue.addLast(1);
    queue.addLast(2);
    queue.addLast(3);
    queue.removeFirst();
    queue.removeFirst();
    queue.addLast(4);                   // this one has to wrap to slot 0
    queue.addLast(5);
    assertEquals("wrapping must not disturb the logical order",
        Arrays.asList(3, 4, 5), queue.toList());
    assertEquals(3, queue.getCapacity());
  }

  @Test
  public void aFullDequeDoublesAndRebasesAtZero() {
    RefDeque queue = new RefDeque(3);
    queue.addLast(1);
    queue.addLast(2);
    queue.addLast(3);
    queue.removeFirst();
    queue.addLast(4);                   // now wrapped: front is at slot 1
    queue.addLast(5);                   // full again, so this one grows it
    assertEquals(6, queue.getCapacity());
    assertEquals(Arrays.asList(2, 3, 4, 5), queue.toList());
    assertEquals("growth re-bases the elements at slot 0",
        Integer.valueOf(2), queue.peekFirst());
  }

  @Test
  public void growthCopiesInLogicalOrderNotArrayOrder() {
    RefDeque queue = new RefDeque(2);
    queue.addLast(1);
    queue.addLast(2);
    queue.removeFirst();
    queue.addLast(3);                   // array is now [3, 2] with front at 1
    queue.beginOperation();
    queue.addLast(4);                   // triggers growth
    assertEquals(Arrays.asList(2, 3, 4), queue.toList());
    assertEquals("one copy per live element", 2, ((Number) queue.cost().get("copies")).intValue());
  }

  @Test
  public void emptyingAndRefillingKeepsFrontAndRearInStep() {
    RefDeque deque = new RefDeque(4);
    for (int round = 0; round < 6; round++) {
      deque.addLast(round);
      assertEquals(Integer.valueOf(round), deque.removeFirst());
      assertEquals(0, deque.size());
    }
    deque.addLast(99);
    assertEquals(Arrays.asList(99), deque.toList());
  }

  @Test
  public void popNeverShrinksTheArray() {
    RefDeque stack = new RefDeque(2);
    stack.addLast(1);
    stack.addLast(2);
    stack.addLast(3);                   // grows to 4
    assertEquals(4, stack.getCapacity());
    stack.removeLast();
    stack.removeLast();
    stack.removeLast();
    assertTrue(stack.isEmpty());
    assertEquals(4, stack.getCapacity());
  }
}
