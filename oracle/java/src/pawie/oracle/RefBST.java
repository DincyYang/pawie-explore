package pawie.oracle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An unbalanced binary search tree.
 *
 * Removal is the interesting case and the one students get wrong: a node with
 * two children keeps its own identity, takes the in-order successor's key, and
 * then the successor is spliced out of the right subtree. Doing it the other
 * way round (moving the node instead of the key) produces a differently shaped
 * tree that still passes a naive in-order check, which is exactly why the
 * differential test compares tree shape and not just the sorted keys.
 */
public class RefBST extends RefStructure {

  static final class Node {
    Object key;
    Node left;
    Node right;
    Node(Object key) { this.key = key; }
  }

  private Node root;
  private int size;

  public RefBST() {
  }

  @Override
  public Object state() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("tree", shape(root, new ArrayList<>()));
    return m;
  }

  /** Pre-order with explicit nulls: two different shapes never serialise alike. */
  private static List<Object> shape(Node n, List<Object> out) {
    if (n == null) {
      out.add(null);
      return out;
    }
    out.add(n.key);
    shape(n.left, out);
    shape(n.right, out);
    return out;
  }

  public int size() { return size; }

  public List<Object> inorder() {
    List<Object> out = new ArrayList<>();
    inorder(root, out);
    return out;
  }

  private static void inorder(Node n, List<Object> out) {
    if (n == null) return;
    inorder(n.left, out);
    out.add(n.key);
    inorder(n.right, out);
  }

  public boolean insert(Object key) {
    if (key == null) throw new NullPointerException("a BST key cannot be null");
    if (root == null) {
      root = new Node(key);
      count("links");
      size++;
      snap();
      return true;
    }
    Node curr = root;
    while (true) {
      count("compares");
      int c = Values.cmp(key, curr.key);
      if (c == 0) return false;                    // already present, shape unchanged
      if (c > 0) {
        if (curr.right == null) {
          curr.right = new Node(key);
          count("links");
          size++;
          snap();
          return true;
        }
        curr = curr.right;
      } else {
        if (curr.left == null) {
          curr.left = new Node(key);
          count("links");
          size++;
          snap();
          return true;
        }
        curr = curr.left;
      }
    }
  }

  public boolean search(Object key) {
    if (key == null) return false;
    Node curr = root;
    while (curr != null) {
      count("compares");
      int c = Values.cmp(key, curr.key);
      if (c == 0) return true;
      curr = c < 0 ? curr.left : curr.right;
    }
    return false;
  }

  public boolean remove(Object key) {
    if (key == null) return false;
    Node curr = root;
    Node parent = null;
    while (curr != null) {
      count("compares");
      int c = Values.cmp(key, curr.key);
      if (c == 0) break;
      parent = curr;
      curr = c < 0 ? curr.left : curr.right;
    }
    if (curr == null) return false;

    if (curr.left != null && curr.right != null) {
      // Two children: borrow the in-order successor's key, then delete the
      // successor, which by construction has no left child.
      Node successorParent = curr;
      Node successor = curr.right;
      while (successor.left != null) {
        successorParent = successor;
        successor = successor.left;
      }
      curr.key = successor.key;
      snap();                                   // the tree briefly holds the key twice
      relink(successorParent, successor, successor.right);
      count("links");
    } else {
      Node child = curr.left != null ? curr.left : curr.right;
      relink(parent, curr, child);
      count("links");
    }
    size--;
    snap();
    return true;
  }

  private void relink(Node parent, Node oldNode, Node replacement) {
    if (parent == null) root = replacement;
    else if (parent.left == oldNode) parent.left = replacement;
    else parent.right = replacement;
  }

  public void clear() {
    if (root == null) return;
    root = null;
    size = 0;
    snap();
  }

  /* ---------------- invariants ---------------- */

  /** Every key sits inside the range its ancestors allow. */
  public boolean orderingHolds() {
    return ordered(root, null, null);
  }

  private static boolean ordered(Node n, Object low, Object high) {
    if (n == null) return true;
    if (low != null && Values.cmp(n.key, low) <= 0) return false;
    if (high != null && Values.cmp(n.key, high) >= 0) return false;
    return ordered(n.left, low, n.key) && ordered(n.right, n.key, high);
  }

  public int countNodes() {
    return count(root);
  }

  private static int count(Node n) {
    return n == null ? 0 : 1 + count(n.left) + count(n.right);
  }

  @Override
  public Object invoke(String op, List<Object> args) {
    switch (op) {
      case "insert": return insert(args.get(0));
      case "search": return search(args.get(0));
      case "remove": return remove(args.get(0));
      case "size": return size();
      case "clear": clear(); return null;
      default:
        throw new IllegalArgumentException("unknown BST operation: " + op);
    }
  }

  @Override
  protected String[] counterNames() {
    return new String[] { "compares", "links" };
  }

}
