package dzaima.ui.node.types.tabs;

import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.*;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.*;
import dzaima.ui.node.utils.*;
import dzaima.utils.*;

public class TabbedNode extends Node {
  public TabbedNode(Ctx ctx, Props props) {
    this(ctx, props, findTL(ctx, props));
  }
  public TabbedNode(Ctx ctx) {
    this(ctx, Props.none(), findTL(ctx, Props.none()));
  }
  
  public TabbedNode(Ctx ctx, Props props, PNodeGroup tl) {
    super(ctx, props);
    Box<TabReorderNode> l = new Box<>();
    tabListW = ctx.makeKV(tl, "list", (Ctx.NodeGen) (ctx2, props2) -> {
      TabReorderNode r = new TabReorderNode(ctx2, props2, this);
      assert !l.has();
      l.set(r);
      return r;
    });
    tabList = l.get();
    assert tabList!=null;
    updated();
    mRedraw();
  }
  
  public TabbedNode findNewHolder(Tab forWhat, Node c, int x, int y) {
    while (!Rect.inXYWH(x, y, 0, 0, c.w, c.h)) {
      x+= c.dx;
      y+= c.dy;
      c = c.p;
      if (!(c instanceof WindowSplitNode || (c instanceof TabbedNode && ((TabbedNode) c).cTab() instanceof GroupTab))) return null; // incl. c==null
    }
    
    mid: while (true) {
      if (c instanceof TabbedNode) {
        Tab ct = ((TabbedNode) c).cTab();
        if (ct instanceof GroupTab && ct.open) {
          Node n = c.ch.get(1);
          if (Rect.inXYWH(x, y, n.dx, n.dy, n.w, n.h)) {
            if (ct==forWhat) return null;
            x-= n.dx;
            y-= n.dy;
            c = n;
            continue mid;
          }
        }
        return (TabbedNode) c;
      }
      
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
  
  public static class TabReorderNode extends ReorderableNode {
    public final TabbedNode t;
    boolean wasSel, firstMove, canceledReorder;
    public TabReorderNode(Ctx ctx, Props props, TabbedNode t) {
      super(ctx, props);
      this.t = t;
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
      if (c.bR()) c.register(this, x, y);
    }
    
    public void mouseDown(int x, int y, Click c) {
      super.mouseDown(x, y, c);
      if (c.bR()) {
        PartialMenu m = new PartialMenu(gc);
        m.add(gc.getProp("tabbed.barMenu.addGroup").gr(), "addGroup", () -> {
          t.addTab(t.makeGroupTab());
        });
        t.addModeMenu(m);
        m.open(ctx, c);
      }
    }
    
    public void mouseTick(int x0, int y0, Click c) {
      super.mouseTick(x0, y0, c);
      if (c.bL() && reordering()) {
        TabWrapper w = (TabWrapper) heldNode();
        TabbedNode oh = w.o;
        if (!oh.tabList.visible) oh.setMode(Mode.ALWAYS); // should only happen when dragged into a previously-zero-tab window with a hidden bar; TODO something better
        XY rel = oh.tabList.relPos(oh);
        int x = x0+rel.x;
        int y = y0+rel.y;
        int d = Rect.xywh(0, 0, oh.w, Math.min(oh.h, h)).manhattanDistance(x, y);
        int md = firstMove? gc.getProp("tabbed.dragOutMinDist").len() : 0;
        if (d > md && (!Rect.xywh(0, 0, oh.w, oh.h).contains(x, y) || t.cTab() instanceof GroupTab)) {
          TabbedNode nh = oh.findNewHolder(w.tab, oh, x, y);
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
  
  public void addModeMenu(PartialMenu m) {
    boolean wm = mode == TabbedNode.Mode.WHEN_MULTIPLE;
    if (wm || tabCount()==1) m.add(ctx.gc.getProp(wm? "tabbed.unhideBar" : "tabbed.hideBar").gr(), s -> {
      switch (s) {
        case "base_hideBar": setMode(TabbedNode.Mode.WHEN_MULTIPLE); return true;
        case "base_unhideBar": setMode(TabbedNode.Mode.ALWAYS); return true;
        default: return false;
      }
    });
  }
  
  private Tab makeGroupTab() {
    return new GroupTab(this);
  }
  
  public void propsUpd() {
    tlBg = gc.col(this, "bg", "tabbed.barBg");
    tlBgOff = gc.col(this, "bgOff", "tabbed.bgOff");
    tlBgOn = gc.col(this, "bgOn", "tabbed.bgOn");
    tlRadius = gc.len(this, "radius", "tabbed.radius");
    tlL = gc.len(this, "padL", "tabbed.padL");
    tlU = gc.len(this, "padU", "tabbed.padU");
    tlW = gc.len(this, "padR", "tabbed.padR")+tlL;
    tlH = gc.len(this, "padD", "tabbed.padD")+tlU;
  }
  
  private static PNodeGroup findTL(Ctx ctx, Props props) {
    Prop b = props.getNullable("bar");
    if (b!=null) return b.gr();
    return ctx.gc.getProp("tabbed.bar").gr();
  }
  
  // tab list properties
  int tlBg, tlRadius, tlBgOn, tlBgOff;
  int tlL, tlU, tlW, tlH;
  
  public final Node tabListW;
  public final TabReorderNode tabList;
  
  public enum Mode { NEVER, ALWAYS, WHEN_MULTIPLE }
  public Mode mode = Mode.ALWAYS; // when to show top bar
  
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
    if (tabCount()==1 && mode==Mode.WHEN_MULTIPLE) toTab(getTab(0).w);
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
        prevTab.onHidden();
        remove(ch.sz-1, ch.sz);
      }
      if (c!=null) {
        c.open = true;
        add(c.show());
        c.onShown();
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
  
  public int minW() { return ListUtils.vMinW(ch); }
  public int minH(int w) { return ListUtils.vMinH(ch, w); }
  
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
  
  public static class GroupTab extends Tab {
    public String name;
    private Node storedNode;
    public GroupTab(TabbedNode t) {
      this(null, new TabbedNode(t.ctx));
    }
    public GroupTab(Node content) {
      this(null, content);
    }
    public GroupTab(String name, Node content) {
      super(content.ctx);
      this.name = name==null? content.gc.getProp("tabbed.group.defaultName").str() : null;
      storedNode = content;
    }
    
    public Node show() {
      Node n = storedNode;
      storedNode = null;
      return n;
    }
    private Node getLive() {
      assert w.o!=null;
      return w.o.ch.get(1);
    }
    public void onHidden() {
      storedNode = getLive();
    }
    public Node getContent() {
      return open? getLive() : storedNode;
    }
    
    public String name() { return name; }
    
    private boolean emptyContent() {
      Node c = getContent();
      return c instanceof TabbedNode && ((TabbedNode) c).tabCount()==0;
    }
    public void onRightClick(Click cl) {
      PartialMenu m = new PartialMenu(ctx.gc);
      m.addField(name, s -> {
        name = s;
        nameUpdated();
      });
      if (w.o.tabCount()==1) m.add(ctx.gc.getProp("tabbed.barMenu.unwrap").gr(), "unwrap", () -> {
        if (w.o.tabCount()==1) w.o.replaceSelfInParent(this::getContent);
      });
      if (emptyContent()) m.add(w.gc.getProp("tabbed.group.delete").gr(), "remove", () -> {
        if (emptyContent()) w.o.removeTab(w.o.tabIndex(this));
      });
      m.addSep();
      addMenuBarOptions(m);
      WindowSplitNode.onTabRightClick(m, this);
      m.open(ctx, cl);
    }
  }
}
