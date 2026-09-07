package pawie.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import org.junit.Test;

/**
 * A doubly linked list: the pointer work, checked through the values you can walk to
 * and through the link consistency the sentinels are supposed to guarantee.
 */
public class RefLinkedListTest {

  private static RefLinkedList of(Object... values) {
    RefLinkedList list = new RefLinkedList();
    for (Object v : values) list.append(v);
    return list;
  }

  @Test
  public void anEmptyListHasBothSentinelsPointingAtEachOther() {
    RefLinkedList list = new RefLinkedList();
    assertEquals(0, list.size());
    assertTrue(list.isEmpty());
    assertTrue(list.linksAreConsistent());
    assertEquals(Arrays.asList(), list.values());
  }

  @Test
  public void addingAtTheFrontAndTheBackBothKeepTheChainWalkable() {
    RefLinkedList list = of(2, 3);
    list.add(0, 1);
    list.add(3, 4);
    assertEquals(Arrays.asList(1, 2, 3, 4), list.values());
    assertTrue(list.linksAreConsistent());
  }

  @Test
  public void addingInTheMiddleRewiresBothNeighbours() {
    RefLinkedList list = of(1, 3);
    list.add(1, 2);
    assertEquals(Arrays.asList(1, 2, 3), list.values());
    assertTrue("prev and next have to agree in both directions", list.linksAreConsistent());
  }

  @Test
  public void removeReturnsTheValueAndLeavesTheChainIntact() {
    RefLinkedList list = of(1, 2, 3);
    assertEquals(Integer.valueOf(2), list.remove(1));
    assertEquals(Arrays.asList(1, 3), list.values());
    assertTrue(list.linksAreConsistent());
  }

  @Test
  public void removingTheOnlyNodeGoesBackToTwoSentinels() {
    RefLinkedList list = of(7);
    assertEquals(Integer.valueOf(7), list.remove(0));
    assertEquals(0, list.size());
    assertTrue(list.isEmpty());
    assertTrue(list.linksAreConsistent());
  }

  @Test
  public void nullIsRejectedByAddAndBySet() {
    RefLinkedList list = of(1);
    try {
      list.add(0, null);
      fail("a linked list node cannot hold null");
    } catch (NullPointerException expected) {
      assertEquals(1, list.size());
    }
    try {
      list.set(0, null);
      fail("set(null) has to be rejected too");
    } catch (NullPointerException expected) {
      assertEquals(Integer.valueOf(1), list.get(0));
    }
  }

  @Test
  public void addAllowsIndexSizeButRemoveDoesNot() {
    RefLinkedList list = of(1, 2);
    list.add(2, 3);
    assertEquals(Arrays.asList(1, 2, 3), list.values());
    try {
      list.remove(3);
      fail("remove(size) should be rejected");
    } catch (IndexOutOfBoundsException expected) {
      assertEquals(3, list.size());
    }
  }

  @Test
  public void clearEmptiesTheListWithoutWalkingIt() {
    RefLinkedList list = of(1, 2, 3, 4, 5);
    list.beginOperation();
    list.clear();
    assertEquals(0, list.size());
    assertTrue(list.linksAreConsistent());
    assertEquals("clear repoints a fixed number of links whatever the length",
        4, ((Number) list.cost().get("links")).intValue());
    assertEquals("clear never walks the chain", 0, ((Number) list.cost().get("hops")).intValue());
  }

  @Test
  public void clearOnAnEmptyListDoesNothingAtAll() {
    RefLinkedList list = new RefLinkedList();
    list.beginOperation();
    list.clear();
    assertEquals(0, ((Number) list.cost().get("links")).intValue());
    assertTrue(list.linksAreConsistent());
  }

  @Test
  public void reachingAnIndexCostsOneHopPerLinkFollowed() {
    RefLinkedList list = of(1, 2, 3, 4);
    list.beginOperation();
    list.get(3);
    assertEquals("index 3 is four links from the head sentinel",
        4, ((Number) list.cost().get("hops")).intValue());
  }

  @Test
  public void addAtTheFrontCostsNoWalkAtAll() {
    RefLinkedList list = of(1, 2, 3);
    list.beginOperation();
    list.add(0, 0);
    assertEquals(0, ((Number) list.cost().get("hops")).intValue());
    assertFalse(list.isEmpty());
  }
}
