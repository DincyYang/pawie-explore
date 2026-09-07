package pawie.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import org.junit.Test;

/**
 * A binary search tree. Removal is the part that goes wrong most often, so each of the three cases gets its
 * own test, and the two-child case checks the resulting shape rather than only
 * that the key is gone.
 */
public class RefBSTTest {

  private static RefBST of(int... keys) {
    RefBST tree = new RefBST();
    for (int k : keys) tree.insert(k);
    return tree;
  }

  @Test
  public void insertKeepsTheInOrderWalkSorted() {
    RefBST tree = of(50, 30, 70, 20, 40, 60, 80);
    assertEquals(Arrays.asList(20, 30, 40, 50, 60, 70, 80), tree.inorder());
    assertTrue(tree.orderingHolds());
    assertEquals(7, tree.size());
  }

  @Test
  public void aDuplicateKeyChangesNothing() {
    RefBST tree = of(50, 30);
    assertFalse("the key is already there", tree.insert(30));
    assertEquals(2, tree.size());
    assertEquals(Arrays.asList(30, 50), tree.inorder());
  }

  @Test
  public void searchFindsWhatIsThereAndMissesWhatIsNot() {
    RefBST tree = of(50, 30, 70);
    assertTrue(tree.search(30));
    assertTrue(tree.search(70));
    assertFalse(tree.search(31));
    assertFalse("a null key is never in the tree", tree.search(null));
  }

  @Test
  public void removingALeafJustDetachesIt() {
    RefBST tree = of(50, 30, 70);
    assertTrue(tree.remove(30));
    assertEquals(Arrays.asList(50, 70), tree.inorder());
    assertEquals(2, tree.size());
    assertTrue(tree.orderingHolds());
  }

  @Test
  public void removingANodeWithOneChildSplicesTheChildIn() {
    RefBST tree = of(50, 30, 70, 20);
    assertTrue(tree.remove(30));
    assertEquals(Arrays.asList(20, 50, 70), tree.inorder());
    assertTrue("20 has taken 30's place", tree.orderingHolds());
    assertEquals(3, tree.size());
  }

  @Test
  public void removingANodeWithTwoChildrenTakesTheSuccessorsKey() {
    RefBST tree = of(50, 30, 70, 60, 80);
    assertTrue(tree.remove(50));
    assertEquals(Arrays.asList(30, 60, 70, 80), tree.inorder());
    assertEquals("60 is the in-order successor, so it becomes the root",
        Arrays.<Object>asList(60, 30, null, null, 70, null, 80, null, null), shape(tree));
    assertTrue(tree.orderingHolds());
  }

  @Test
  public void removingTheRootOfATwoNodeTreeWorks() {
    RefBST tree = of(50, 70);
    assertTrue(tree.remove(50));
    assertEquals(Arrays.asList(70), tree.inorder());
    assertEquals(1, tree.size());
  }

  @Test
  public void removingSomethingAbsentReportsIt() {
    RefBST tree = of(50, 30);
    assertFalse(tree.remove(31));
    assertEquals(2, tree.size());
    assertFalse("a null key removes nothing", tree.remove(null));
  }

  @Test
  public void aDegenerateChainStillBehaves() {
    RefBST tree = of(1, 2, 3, 4, 5);
    assertEquals(Arrays.asList(1, 2, 3, 4, 5), tree.inorder());
    assertEquals("every node is a right child here", 5, tree.countNodes());
    assertTrue(tree.remove(3));
    assertEquals(Arrays.asList(1, 2, 4, 5), tree.inorder());
    assertTrue(tree.orderingHolds());
  }

  @Test
  public void nullKeysAreRefusedOnInsert() {
    RefBST tree = new RefBST();
    try {
      tree.insert(null);
      fail("a BST key cannot be null");
    } catch (NullPointerException expected) {
      assertEquals(0, tree.size());
    }
  }

  @Test
  public void searchCostsOneComparisonPerNodeOnThePath() {
    RefBST tree = of(50, 30, 70, 20, 40);
    tree.beginOperation();
    tree.search(40);
    assertEquals("50, then 30, then 40", 3, ((Number) tree.cost().get("compares")).intValue());
  }

  @Test
  public void clearEmptiesTheWholeTree() {
    RefBST tree = of(50, 30, 70);
    tree.clear();
    assertEquals(0, tree.size());
    assertEquals(Arrays.asList(), tree.inorder());
  }

  @SuppressWarnings("unchecked")
  private static java.util.List<Object> shape(RefBST tree) {
    java.util.Map<String, Object> state = (java.util.Map<String, Object>) tree.state();
    return (java.util.List<Object>) state.get("tree");
  }
}
