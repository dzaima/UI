package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;

public class VNode extends FrameNode {
  
  private int pad;
  public VNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  public void propsUpd() { super.propsUpd();
    pad = gc.pxD(this, "pad", 0);
  }
  
  public int fillW() {
    if (ch.sz==0) return 0;
    int w = 0;
    for (Node c : ch) w = Math.max(w, c.minW());
    return w;
  }
  
  public int fillH(int w) {
    if (ch.sz==0) return 0;
    int h = 0;
    for (Node c : ch) h+= c.minH(w);
    return h + pad*(ch.sz-1);
  }
  
  public void drawCh(Graphics g, boolean full) {
    if (g.clip==null || ch.sz<10) { super.drawCh(g, full); return; }
    for (int i = s2(ch, g.clip.sy); i<ch.sz; i++) {
      Node c = ch.get(i);
      if (c.dy+c.h < g.clip.sy) continue;
      if (c.dy > g.clip.ey) break;
      c.draw(g, full);
    }
  }
  public Node findCh(int x, int y) {
    if (ch.sz<20) return super.findCh(x, y);
    Node c = ch.get(s2(ch, y));
    if (XY.inWH(x, y, c.dx, c.dy, c.w, c.h)) return c;
    return null;
  }
  
  public static int s2(Vec<Node> ch, int y) {
    int s = 0;
    int e = ch.sz;
    while (s+1<e) {
      int m = (s+e)/2;
      Node c = ch.get(m);
      if (y>c.dy) s=m;
      else e=m;
    }
    return s;
  }
  
  public Node nearestCh(int x, int y) {
    if (ch.sz<20) return super.nearestCh(x, y);
    int p = s2(ch, y);
    int min = Integer.MAX_VALUE, curr;
    Node best = null;
    for (int i = Math.max(0, p-2); i < Math.min(p+2, ch.sz); i++) {
      Node c = ch.get(i);
      if (c.w!=-1 && (curr=XY.dist(x, y, c.dx, c.dy, c.w, c.h))<min) { min=curr; best = c; }
    }
    if (best==null) return super.nearestCh(x, y); // fallback for w==-1
    return best;
  }
  
  public void resized() {
    if (ch.sz==0) return;
    int xal = xalign();
    int yal = yalign();
    int[] div = Solve.solve(ch, h - pad*(ch.sz-1), w, true);
    int th = pad*(ch.sz-1); for (int i=0; i<ch.sz; i++) th+= div[i];
    int y = align(yal, h, th);
    fillWC = 0;
    boolean r = th!=h;
    for (int i = 0; i < ch.sz; i++) {
      Node c = ch.get(i);
      int cminH = div[i];
      int nw = Math.min(c.maxW(), w);
      int x = align(xal, w, nw);
      c.resize(nw, cminH, x, y);
      r|= nw!=w;
      fillWC = Math.max(fillWC, c.minW());
      y+= cminH+pad;
    }
    if (r) mRedraw();
  }
}
