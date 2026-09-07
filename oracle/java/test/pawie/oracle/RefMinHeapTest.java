package pawie.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** A binary min-heap: percolation, and the array layout it depends on. */
public class RefMinHeapTest {

  @Test
  public void theSmallestValueEndsUpAtTheRoot() {
    RefMinHeap heap = new RefMinHeap();
    for (int v : new int[] { 5, 3, 8, 1, 9 }) heap.insert(v);
    assertEquals(Integer.valueOf(1), heap.getMin());
    assertTrue(heap.heapPropertyHolds());
  }

  @Test
  public void insertingInDescendingOrderPercolatesEveryTime() {
    RefMinHeap heap = new RefMinHeap();
    for (int v : new int[] { 9, 8, 7, 6, 5 }) heap.insert(v);
    assertEquals(Integer.valueOf(5), heap.getMin());
    assertTrue(heap.heapPropertyHolds());
  }

  @Test
  public void removeMinHandsBackValuesInSortedOrder() {
    RefMinHeap heap = new RefMinHeap();
    int[] input = { 7, 2, 9, 4, 4, 1, 8, 3 };
    for (int v : input) heap.insert(v);
    List<Object> drained = new ArrayList<>();
    while (heap.size() > 0) drained.add(heap.removeMin());
    assertEquals(Arrays.asList(1, 2, 3, 4, 4, 7, 8, 9), drained);
  }

  @Test
  public void anEmptyHeapPeeksAndPopsNull() {
    RefMinHeap heap = new RefMinHeap();
    assertNull(heap.getMin());
    assertNull(heap.removeMin());
    assertEquals(0, heap.size());
  }

  @Test
  public void removingTheLastElementLeavesAnEmptyHeap() {
    RefMinHeap heap = new RefMinHeap();
    heap.insert(1);
    assertEquals(Integer.valueOf(1), heap.removeMin());
    assertEquals(0, heap.size());
    assertTrue(heap.heapPropertyHolds());
  }

  @Test
  public void duplicatesAreKeptRatherThanCollapsed() {
    RefMinHeap heap = new RefMinHeap();
    heap.insert(4);
    heap.insert(4);
    heap.insert(4);
    assertEquals(3, heap.size());
    assertEquals(Integer.valueOf(4), heap.removeMin());
    assertEquals(2, heap.size());
  }

  @Test
  public void nullIsRefused() {
    RefMinHeap heap = new RefMinHeap();
    try {
      heap.insert(null);
      fail("a min-heap cannot hold null");
    } catch (NullPointerException expected) {
      assertEquals(0, heap.size());
    }
  }

  @Test
  public void insertingIntoAnEmptyHeapComparesNothing() {
    RefMinHeap heap = new RefMinHeap();
    heap.beginOperation();
    heap.insert(1);
    assertEquals(0, ((Number) heap.cost().get("compares")).intValue());
    assertEquals(0, ((Number) heap.cost().get("swaps")).intValue());
  }

  @Test
  public void percolationCostStaysWithinTheTreeHeight() {
    RefMinHeap heap = new RefMinHeap();
    for (int v = 100; v > 0; v--) heap.insert(v);      // every insert is a new minimum
    heap.beginOperation();
    heap.insert(0);
    int swaps = ((Number) heap.cost().get("swaps")).intValue();
    assertTrue("a new minimum climbs to the root in at most log2(n) swaps, not n",
        swaps <= 7);
  }

  @Test
  public void aTieSendsPercolationDownTheLeftChild() {
    RefMinHeap heap = new RefMinHeap(Arrays.<Object>asList(5, 2, 2));
    assertEquals("with equal children the left one is chosen", 1, heap.minChild(0));
  }
}
