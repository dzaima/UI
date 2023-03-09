package dzaima.ui.node.types.tabs;

import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.ui.node.types.ReorderableNode;
import dzaima.utils.*;

public class TabbedNode extends Node {
  public TabbedNode(Ctx ctx, String[] ks, Prop[] vs) {
    this(ctx, ks, vs, findTL(ctx, ks, vs));
  }
  
  public TabbedNode(Ctx ctx, String[] ks, Prop[] vs, PNodeGroup tl) {
    super(ctx, ks, vs);
    Box<TabReorderNode> l = new Box<>();
    tabListW = ctx.makeKV(tl, "list", (Ctx.NodeGen) (ctx2, ks2, vs2) -> {
      TabReorderNode r = new TabReorderNode(ctx2, ks2, vs2);
      assert !l.has();
      l.set(r);
      return r;
    });
    tabList = l.get();
    assert tabList!=null;
  }
  
  public TabbedNode findNewHolder(Node c, int x, int y) {
    while (!Rect.inXYWH(x, y, 0, 0, c.w, c.h)) {
      x+= c.dx;
      y+= c.dy;
      c = c.p;
      if (!(c instanceof WindowSplitNode)) return null; // incl. c==null
    }
    
    mid: while (true) {
      if (c instanceof TabbedNode) return (TabbedNode) c;
      
      for (Node n : c.ch) {
        if (Rect.inXYWH(x, y, n.dx, n.dy, n.w, n.h)) {
          x-= n.dx;
          y-= n.dy;
          c = n;
          continue mid;
        }
      }
      return null;
    }
  }
  private static class TabReorderNode extends ReorderableNode {
    boolean wasSel, firstMove, canceledReorder;
    public TabReorderNode(Ctx ctx, String[] ks, Prop[] vs) {
      super(ctx, ks, vs);
    }
    
    public boolean shouldReorder(int idx, Node n) {
      return n instanceof TabWrapper;
    }
    public void reorderStarted(Node n) {
      wasSel = ((TabWrapper) n).sel;
      firstMove = true;
      canceledReorder = false;
    }
    
    static int depth = 0;
    
    public void mouseStart(int x, int y, Click c) {
      depth = 0;
      super.mouseStart(x, y, c);
    }
    
    public void mouseTick(int x0, int y0, Click c) {
      super.mouseTick(x0, y0, c);
      if (c.bL() && reordering()) {
        TabWrapper w = (TabWrapper) heldNode();
        TabbedNode oh = w.o;
        XY rel = oh.tabList.relPos(oh);
        int x = x0+rel.x;
        int y = y0+rel.y;
        int d = Rect.xywh(0, 0, oh.w, Math.min(oh.h, h)).manhattanDistance(x, y);
        int md = firstMove? gc.getProp("tabbed.dragOutMinDist").len() : 0;
        if (d > md && !Rect.xywh(0, 0, oh.w, oh.h).contains(x, y)) {
          TabbedNode nh = oh.findNewHolder(oh, x, y);
          if (depth>10) {
            Log.warn("tabs", "recursive window moving!!");
          } else if (nh!=null) {
            depth++;
            canceledReorder = true;
            stopReorder(false);
            oh.removeTab(oh.tabIndex(w.tab));
            TabWrapper nw = nh.addTab(w.tab);
            nw.sel = wasSel;
            nh.tabList.manualReorderStart(takeX, takeY, c, nw);
            c.replace(nh.tabList, 0, 0);
            c.tickClick();
            nh.tabList.wasSel = wasSel;
            nh.tabList.firstMove = false;
            depth--;
          }
        }
      }
    }
    
    public void reorderEnded(int oldIdx, int newIdx, Node n) {
      if (wasSel && !canceledReorder) {
        TabWrapper w = (TabWrapper) n;
        w.tab.switchTo();
      }
    }
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
  public final TabReorderNode tabList;
  
  public enum Mode { NEVER, ALWAYS, WHEN_MULTIPLE }
  public Mode mode = Mode.ALWAYS;
  
  public void setMode(Mode m) {
    mode = m;
    updated();
  }
  
  public TabWrapper addTab(Tab t) {
    if (tabList.reordering()) tabList.stopReorder(false);
    TabWrapper w = new TabWrapper(this, t);
    tabList.add(w);
    updated();
    return w;
  }
  public int tabCount() {
    return tabList.ch.sz;
  }
  public int tabIndex(Tab t) {
    if (tabList.reordering()) tabList.stopReorder(false);
    int i = 0;
    while (getTab(i)!=t) i++;
    return i;
  }
  public void removeTab(int i) {
    if (tabList.reordering()) tabList.stopReorder(false);
    if (cw!=null && getTab(i)==cw.tab) cw = null;
    tabList.remove(i, i+1);
    updated();
  }
  public void addSelectedTab(Tab t) {
    toTab(addTab(t));
  }
  
  public Tab getTab(int i) {
    if (tabList.reordering()) tabList.stopReorder(false);
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
  
  public Tab cTab() {
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
