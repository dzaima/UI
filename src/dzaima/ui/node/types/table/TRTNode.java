package dzaima.ui.node.types.table;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;

public class TRTNode extends Node { // table row template node
  public TableNode t;
  
  int bg; boolean hasBg;
  public TRTNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  public void propsUpd() { super.propsUpd();
    int id = id("bg");
    hasBg = id>=0;
    if (hasBg) bg = vs[id].col();
  }
  
  public void shown() { super.shown();
    assert p instanceof TableNode : "`tr` must be a child of `table`";
    t = (TableNode) this.p;
  }
  
  
  public void drawCh(Graphics g, boolean full) {
    int[] cols = t.cols(t.w);
    for (int i = 0; i < ch.sz; i++) {
      Node n = ch.get(i);
      g.push();
      g.clip(n.dx, n.dy, cols[i], n.h);
      n.draw(g, full);
      g.pop();
    }
  }
  
  public int minH(int w) {
    int h = 0;
    int[] cols = t.cols(w); // w == t.w (except while mid-updating, like here)
    for (int i = 0; i < ch.sz; i++) {
      Node c = ch.get(i);
      h = Math.max(h, c.minH(Math.max(c.minW(), cols[i]))); // TODO somehow don't repeat work
    }
    return h;
  }
  
  public void resized() {
    int[] cols = t.cols(t.w); // TODO maybe this should use own width, as t.w might not be initialized
    int x = t.padX;
    for (int i = 0; i < ch.sz; i++) {
      Node c = ch.get(i);
      int w = cols[i];
      // TODO clip xalign from THNode
      c.resize(Math.max(w, c.minW()), h-t.padY*2, x, t.padY);
      x+= w+t.colDist;
    }
  }
}
