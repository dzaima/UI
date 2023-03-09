package dzaima.ui.node.types.tabs;

import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.types.*;

public class TabWrapper extends PadCNode {
  public final TabbedNode o;
  public final Tab tab;
  
  public boolean sel;
  
  public TabWrapper(TabbedNode o, Tab t) {
    super(o.ctx, new StringNode(o.ctx, t.name()), 0.4f, 1.1f, 0.3f, 0.3f); // TODO don't use hard-coded constant
    this.o = o;
    tab = t;
    tab.w = this;
  }
  
  public void nameUpdated() {
    replace(0, new StringNode(ctx, tab.name()));
  }
  
  public void drawCh(Graphics g, boolean full) {
    g.push();
    g.clip(lI, uI, w-dwI, h-dhI);
    super.drawCh(g, full);
    g.pop();
  }
  
  public int minW() { return Math.max(super.minW(), gc.em*10); }
  public int maxW() { return Math.max(super.maxW(), gc.em*10); }
  
  public void drawC(Graphics g) {
    g.rrect(0, 0, w, h, o.tlRadius, o.tlRadius, 0, 0, sel? o.tlBgOn : o.tlBgOff);
  }
  
  public void mouseStart(int x, int y, Click c) { if (c.bL() || c.bR()) c.register(this, x, y); }
  public void mouseTick(int x, int y, Click c) { c.onClickEnd(); }
  public void mouseUp(int x, int y, Click c) {
    if (c.bL()) {
      if (visible) tab.switchTo();
    } else if (c.bR()) {
      tab.onRightClick();
    }
  }
}
