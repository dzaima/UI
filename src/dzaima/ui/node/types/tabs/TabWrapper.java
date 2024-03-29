package dzaima.ui.node.types.tabs;

import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.Node;
import dzaima.ui.node.prop.Props;
import dzaima.ui.node.types.StringNode;

public class TabWrapper extends Node {
  public final TabbedNode o;
  public final Tab tab;
  
  public boolean sel;
  
  public TabWrapper(TabbedNode o, Tab t) {
    super(o.ctx, Props.none());
    add(new StringNode(o.ctx, t.name()));
    this.o = o;
    tab = t;
    tab.w = this;
  }
  
  public void propsUpd() {
  }
  
  public void nameUpdated() {
    replace(0, new StringNode(ctx, tab.name()));
  }
  
  public void drawCh(Graphics g, boolean full) {
    g.push();
    g.clip(o.tlL, o.tlU, w-o.tlW, h-o.tlH);
    super.drawCh(g, full);
    g.pop();
  }
  
  public int minH(int w) { return ch.get(0).minH(w-o.tlW)+o.tlH; }
  public int maxH(int w) { return ch.get(0).maxH(w-o.tlW)+o.tlH; }
  public void resized() {
    ch.get(0).resize(w-o.tlW, h-o.tlH, o.tlL, o.tlU);
  }
  
  public int minW() { return Math.max(ch.get(0).minW()+o.tlW, gc.getProp("tabbed.minWidth").len()); }
  public int maxW() { return Math.max(ch.get(0).maxW()+o.tlW, gc.getProp("tabbed.minWidth").len()); }
  
  public void drawC(Graphics g) {
    g.rrect(0, 0, w, h, o.tlRadius, o.tlRadius, 0, 0, sel? o.tlBgOn : o.tlBgOff);
  }
  
  public void mouseStart(int x, int y, Click c) { if (c.bL() || c.bR()) c.register(this, x, y); }
  public void mouseTick(int x, int y, Click c) { c.onClickEnd(); }
  public void mouseDown(int x, int y, Click c) {
    if (c.bR()) tab.onRightClick(c);
  }
  public void mouseUp(int x, int y, Click c) {
    if (c.bL() && visible) tab.switchTo();
  }
}
