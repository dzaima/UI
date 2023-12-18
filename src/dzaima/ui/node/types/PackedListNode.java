package dzaima.ui.node.types;

import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;

public class PackedListNode extends FrameNode {
  public PackedListNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  protected boolean v;
  protected int pad;
  public void propsUpd() {
    super.propsUpd();
    switch (vs[id("dir")].val()) { default: throw new RuntimeException("Bad PackedListNode \"dir\" value "+vs[id("dir")]);
      case "v": v=true; break;
      case "h": v=false; break;
    }
    pad = gc.lenD(this, "pad", 0);
  }
  
  // TODO binary search trimming drawCh
  
  public int fillW() {
    return v? Solve.vMinW(ch)
            : Solve.hMinW(ch)+Math.max(0, pad*(ch.sz-1));
  }
  public int fillH(int w) {
    if (!v) w-= Math.max(0, pad*(ch.sz-1));
    return v? Solve.vMinH(ch, w)+Math.max(0, pad*(ch.sz-1))
            : Solve.hMinH(ch, w);
  }
  protected void resized() {
    boolean r = pad!=0;
    if (v) {
      int y = 0;
      for (Node c : ch) {
        int cw = Math.min(c.maxW(), w);
        int ch = c.minH(cw);
        c.resize(cw, ch, 0, y);
        r|= cw!=w;
        y+= ch+pad;
      }
      r|= y!=h; // TODO can this (& below) even happen?
    } else {
      int x = 0;
      for (Node c : ch) {
        int cw = c.minW();
        int ch = c.minH(cw);
        c.resize(cw, ch, x, 0);
        r|= ch!=h;
        x+= cw+pad;
      }
      r|= x!=w;
    }
    if (r) mRedraw();
  }
}
