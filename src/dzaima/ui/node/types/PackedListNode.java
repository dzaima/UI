package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Props;
import dzaima.ui.node.utils.*;

public class PackedListNode extends FrameNode {
  public PackedListNode(Ctx ctx, Props props) {
    super(ctx, props);
  }
  
  protected boolean v;
  protected int pad;
  public void propsUpd() {
    super.propsUpd();
    switch (getProp("dir").val()) { default: throw new RuntimeException("Bad PackedListNode \"dir\" value "+getProp("dir"));
      case "v": v=true; break;
      case "h": v=false; break;
    }
    pad = gc.lenD(this, "pad", 0);
  }
  
  public void drawCh(Graphics g, boolean full) {
    ListUtils.drawCh(g, full, this, v);
  }
  public Node nearestCh(int x, int y) {
    return ListUtils.findNearest(ch, x, y, v);
  }
  
  public int fillW() {
    return v? ListUtils.vMinW(ch)
            : ListUtils.hMinW(ch)+Math.max(0, pad*(ch.sz-1));
  }
  public int fillH(int w) {
    if (!v) w-= Math.max(0, pad*(ch.sz-1));
    return v? ListUtils.vMinH(ch, w)+Math.max(0, pad*(ch.sz-1))
            : ListUtils.hMinH(ch, w);
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
