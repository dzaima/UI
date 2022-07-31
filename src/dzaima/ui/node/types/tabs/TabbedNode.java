package dzaima.ui.node.types.tabs;

import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.Graphics;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.ui.node.types.ReorderableNode;
import dzaima.utils.Tools;

public class TabbedNode extends Node {
  public TabbedNode(Ctx ctx, String[] ks, Prop[] vs) {
    this(ctx, ks, vs, findTL(ctx, ks, vs));
  }
  
  public TabbedNode(Ctx ctx, String[] ks, Prop[] vs, PNodeGroup tl) {
    super(ctx, ks, vs);
    tabListW = ctx.make(tl);
    tabList = (ReorderableNode) tabListW.ctx.id("r");
  }
  
  public void propsUpd() {
    tlBg = gc.col(this, "bg", "tabbed.barBg");
    tlBgOff = gc.col(this, "bgOff", "tabbed.bgOff");
    tlBgOn = gc.col(this, "bgOn", "tabbed.bgOn");
    tlRadius = gc.len(this, "radius", "tabbed.radius");
  }
  
  private static PNodeGroup findTL(Ctx ctx, String[] ks, Prop[] vs) {
    for (int i = 0; i < ks.length; i++) if (ks[i].equals("bar")) return vs[i].gr();
    return ctx.gc.getProp("tabbed.bar").gr();
  }
  
  // tab list properties
  int tlBg, tlRadius, tlBgOn, tlBgOff;
  
  public final Node tabListW;
  public final ReorderableNode tabList;
  
  public enum Mode { NEVER, ALWAYS, WHEN_MULTIPLE }
  public Mode mode = Mode.ALWAYS;
  
  public void setMode(Mode m) {
    mode = m;
    updated();
  }
  
  public TabWrapper addTab(Tab t) {
    TabWrapper w = new TabWrapper(this, t);
    tabList.add(w);
    updated();
    return w;
  }
  public void addSelectedTab(Tab t) {
    toTab(addTab(t));
  }
  
  public Tab getTab(int i) {
    return ((TabWrapper) tabList.ch.get(i)).tab;
  }
  public Tab[] getTabs() {
    Tab[] tabs = new Tab[tabList.ch.sz]; 
    for (int i = 0; i < tabs.length; i++) tabs[i] = getTab(i);
    return tabs;
  }
  
  boolean hasBar, pHasBar;
  Tab prevTab;
  public void updated() {
    hasBar = mode==Mode.ALWAYS || (mode==Mode.WHEN_MULTIPLE && tabList.ch.sz>1);
    if (!pHasBar && hasBar) insert(0, tabListW);
    if (pHasBar && !hasBar) remove(0, 1);
    pHasBar = hasBar;
    
    Tab c = cTab();
    if (prevTab!=c) {
      if (prevTab!=null) {
        prevTab.open = false;
        prevTab.hide();
        remove(ch.sz-1, ch.sz);
      }
      if (c!=null) {
        c.open = true;
        add(c.show());
      }
      prevTab = c;
    }
  }
  
  Tab cTab() {
    return cw==null? null : cw.tab;
  }
  
  TabWrapper cw;
  public void toTab(TabWrapper w) {
    if (cw==w) return;
    if (cw!=null) {
      cw.sel = false;
      cw.mRedraw();
    }
    cw = w;
    if (cw!=null) {
      cw.sel = true;
      cw.mRedraw();
    }
    updated();
  }
  
  public int minW() { return Solve.vMinW(ch); }
  public int minH(int w) { return Solve.vMinH(ch, w); }
  
  public void bg(Graphics g, boolean full) {
    pbg(g, full);
    if (hasBar && Tools.vs(tlBg)) g.rect(0, 0, w, tabListW.h, tlBg);
  }
  
  protected void resized() {
    int th;
    if (hasBar) {
      th = tabListW.minH(w);
      tabListW.resize(w, th, 0, 0);
    } else th = 0;
    
    Tab ct = cTab();
    if (ct!=null) ch.peek().resize(w, h-th, 0, th);
  }
}
