package pawie.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * An array-backed list: growth, shifting, and the boundaries around index checks.
 *
 * The interesting cases are the ones that go wrong most often: inserting at
 * exactly size (legal) versus past it (not), removing at size - 1 versus size,
 * and what the array looks like after a shift rather than just what size says.
 */
public class RefArrayListTest {

  private static List<Object> data(RefArrayList list) {
    Object[] copy = new Object[list.getCapacity()];
    for (int i = 0; i < list.getCapacity(); i++) copy[i] = i < list.size() ? list.get(i) : null;
    return Arrays.asList(copy);
  }

  @Test
  public void newListStartsEmptyAtTheDefaultCapacity() {
    RefArrayList list = new RefArrayList();
    assertEquals(0, list.size());
    assertEquals(5, list.getCapacity());
  }

  @Test
  public void appendFillsSlotsInOrder() {
    RefArrayList list = new RefArrayList();
    for (int i = 0; i < 5; i++) list.append(i);
    assertEquals(5, list.size());
    assertEquals(5, list.getCapacity());
    assertEquals(Arrays.asList(0, 1, 2, 3, 4), data(list));
  }

  @Test
  public void theArrayDoublesOnlyWhenItIsActuallyFull() {
    RefArrayList list = new RefArrayList();
    for (int i = 0; i < 5; i++) list.append(i);
    assertEquals("five elements still fit in five slots", 5, list.getCapacity());
    list.append(5);
    assertEquals(10, list.getCapacity());
    assertEquals(6, list.size());
    assertEquals(Integer.valueOf(5), list.get(5));
  }

  @Test
  public void growthPreservesEveryElementInOrder() {
    RefArrayList list = new RefArrayList();
    for (int i = 0; i < 23; i++) list.append(i);
    assertEquals(23, list.size());
    assertEquals(40, list.getCapacity());
    for (int i = 0; i < 23; i++) assertEquals(Integer.valueOf(i), list.get(i));
  }

  @Test
  public void addAtTheFrontSlidesEverythingRight() {
    RefArrayList list = new RefArrayList();
    list.append(1);
    list.append(2);
    list.add(0, 9);
    assertEquals(Arrays.asList(9, 1, 2, null, null), data(list));
    assertEquals(3, list.size());
  }

  @Test
  public void addAtSizeIsLegalAndAddPastSizeIsNot() {
    RefArrayList list = new RefArrayList();
    list.append(1);
    list.add(1, 2);                       // index == size is the append case
    assertEquals(2, list.size());
    try {
      list.add(3, 4);
      fail("adding past size should be rejected");
    } catch (IndexOutOfBoundsException expected) {
      assertEquals("a rejected add changes nothing", 2, list.size());
    }
  }

  @Test
  public void removeReturnsTheOldValueAndClosesTheGap() {
    RefArrayList list = new RefArrayList();
    for (int i = 0; i < 4; i++) list.append(i);
    assertEquals(Integer.valueOf(1), list.remove(1));
    assertEquals(Arrays.asList(0, 2, 3, null, null), data(list));
    assertEquals(3, list.size());
  }

  @Test
  public void removeNeverShrinksTheArray() {
    RefArrayList list = new RefArrayList();
    for (int i = 0; i < 6; i++) list.append(i);
    assertEquals(10, list.getCapacity());
    for (int i = 5; i >= 0; i--) list.remove(i);
    assertEquals(0, list.size());
    assertEquals("capacity survives an emptying", 10, list.getCapacity());
  }

  @Test
  public void removeRejectsSizeItself() {
    RefArrayList list = new RefArrayList();
    list.append(1);
    try {
      list.remove(1);
      fail("remove(size) should be rejected");
    } catch (IndexOutOfBoundsException expected) {
      assertEquals(1, list.size());
    }
  }

  @Test
  public void setReplacesInPlaceAndHandsBackTheOldValue() {
    RefArrayList list = new RefArrayList();
    list.append(1);
    list.append(2);
    assertEquals(Integer.valueOf(2), list.set(1, 99));
    assertEquals(Integer.valueOf(99), list.get(1));
    assertEquals("set never changes the length", 2, list.size());
  }

  @Test
  public void aStoredNullIsAnElementLikeAnyOther() {
    RefArrayList list = new RefArrayList();
    list.append(1);
    list.append(null);
    list.append(3);
    assertEquals(3, list.size());
    assertNull(list.get(1));
    list.add(0, 9);
    assertEquals("the stored null moved along with everything else", Arrays.asList(9, 1, null, 3, null),
        data(list));
  }

  @Test
  public void negativeIndicesAreRejectedEverywhere() {
    RefArrayList list = new RefArrayList();
    list.append(1);
    int rejections = 0;
    try { list.get(-1); } catch (IndexOutOfBoundsException e) { rejections++; }
    try { list.set(-1, 0); } catch (IndexOutOfBoundsException e) { rejections++; }
    try { list.remove(-1); } catch (IndexOutOfBoundsException e) { rejections++; }
    try { list.add(-1, 0); } catch (IndexOutOfBoundsException e) { rejections++; }
    assertEquals(4, rejections);
  }

  @Test
  public void aZeroCapacityListStillGrowsOnFirstAdd() {
    RefArrayList list = new RefArrayList(0);
    assertEquals(0, list.getCapacity());
    list.append(7);
    assertTrue("an empty array has to grow to hold anything", list.getCapacity() >= 1);
    assertEquals(Integer.valueOf(7), list.get(0));
  }

  @Test
  public void theArrayConstructorTakesItsLengthAsCapacity() {
    RefArrayList list = new RefArrayList(Arrays.<Object>asList(1, 2, 3));
    assertEquals(3, list.size());
    assertEquals(3, list.getCapacity());
    assertEquals(Integer.valueOf(3), list.get(2));
  }
}
