package dzaima.ui.node.types.tree;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;

public abstract class ATreeNode extends Node {
  public boolean open = true;
  public int depth;
  public TreeNode base;
  public int totalIndent;
  public boolean openable = true;
  public boolean even;
  public int startN;
  
  public ATreeNode(Ctx ctx, String[] ks, Prop[] vs, int startN) {
    super(ctx, ks, vs);
    this.startN = startN;
  }
  
  public void drawCh(Graphics g, boolean full) {
    if (g.clip==null || ch.sz<10) { super.drawCh(g, full); return; }
    for (int i = Solve.vBinSearch(ch, g.clip.sy); i < ch.sz; i++) {
      Node c = ch.get(i);
      if (c.dy+c.h < g.clip.sy) continue; // TODO binary search?
      if (c.dy > g.clip.ey) break;
      c.draw(g, full);
    }
  }
  
  protected int indent() {
    return base.indentFor(depth);
  }
  
  public boolean recalcEven(boolean start) {
    even = start;
    boolean c = !start;
    if (open) for (int i = startN; i < ch.sz; i++) {
      c = ((ATreeNode) ch.get(i)).recalcEven(c);
    }
    return c;
  }
}
