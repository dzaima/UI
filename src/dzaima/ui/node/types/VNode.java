package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Props;
import dzaima.utils.XY;

public class VNode extends FrameNode {
  
  private int pad;
  public VNode(Ctx ctx, Props props) {
    super(ctx, props);
  }
  public void propsUpd() { super.propsUpd();
    pad = gc.pxD(this, "pad", 0);
  }
  
  public int fillW() {
    return Solve.vMinW(ch);
  }
  
  public int fillH(int w) {
    if (ch.sz==0) return 0;
    return Solve.vMinH(ch, w) + pad*(ch.sz-1);
  }
  
  public void drawCh(Graphics g, boolean full) {
    if (g.clip==null || ch.sz<10) { super.drawCh(g, full); return; }
    for (int i = Solve.vBinSearch(ch, g.clip.sy); i<ch.sz; i++) {
      Node c = ch.get(i);
      if (c.dy+c.h < g.clip.sy) continue;
      if (c.dy > g.clip.ey) break;
      c.draw(g, full);
    }
  }
  public Node findCh(int x, int y) {
    if (ch.sz<20) return super.findCh(x, y);
    Node c = ch.get(Solve.vBinSearch(ch, y));
    if (XY.inWH(x, y, c.dx, c.dy, c.w, c.h)) return c;
    return null;
  }
  
  public Node nearestCh(int x, int y) {
    return Solve.vFindNearest(ch, x, y);
  }
  
  public void resized() {
    if (ch.sz==0) return;
    
    int xal = xalign();
    int yal = yalign();
    int padTotal = pad*(ch.sz - 1);
    int[] div = Solve.solve(ch, h-padTotal, w, true);
    int th = padTotal; for (int i = 0; i<ch.sz; i++) th+= div[i];
    int y = align(yal, h, th);
    boolean r = th!=h;
    for (int i = 0; i < ch.sz; i++) {
      Node c = ch.get(i);
      int cminH = div[i];
      int nw = Math.min(c.maxW(), w);
      int x = align(xal, w, nw);
      c.resize(nw, cminH, x, y);
      r|= nw!=w;
      y+= cminH+pad;
    }
    if (r) mRedraw();
  }
}
