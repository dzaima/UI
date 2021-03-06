package dzaima.ui.node.types;

import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;

public class HNode extends FrameNode {
  
  private int pad;
  public HNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  public void propsUpd() { super.propsUpd();
    pad = gc.pxD(this, "pad", 0);
  }
  
  public int fillW() {
    if (ch.sz==0) return 0;
    return Solve.hMinW(ch) + pad*(ch.sz-1);
  }
  public int fillH(int w) {
    if (ch.sz==0) return 0;
    int h = 0;
    int[] div = Solve.solve(ch, w-pad*(ch.sz-1), -1, false); // may or may not worth caching? (same thing will possibly be used in `resized()`)
    for (int i = 0; i < ch.sz; i++) h = Math.max(h, ch.get(i).minH(div[i]));
    return h;
  }
  
  
  
  public void resized() {
    if (ch.sz==0) return;
    int xal = xalign();
    int yal = yalign();
    int padTotal = pad*(ch.sz-1);
    int[] div = Solve.solve(ch, w-padTotal, -1, false);
    int tw = padTotal; for (int i=0; i<ch.sz; i++) tw+= div[i];
    int x = align(xal, w, tw);
    boolean r = tw!=w;
    for (int i = 0; i < ch.sz; i++) {
      Node c = ch.get(i);
      int cmaxH = c.maxH(div[i]);
      int nh = Math.min(cmaxH, h);
      r|= nh!=h; // TODO do this only when the used space differs from previous; same in VNode
      int y = align(yal, h, nh);
      c.resize(div[i], nh, x, y);
      x+= div[i]+pad;
    }
    if (r) mRedraw();
  }
}