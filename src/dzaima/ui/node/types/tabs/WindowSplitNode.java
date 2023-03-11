package dzaima.ui.node.types.tabs;

import dzaima.ui.gui.PartialMenu;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.WeighedNode;

public class WindowSplitNode extends WeighedNode {
  public WindowSplitNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  
  public boolean wantClick(Click c) {
    return super.wantClick(c)  ||  c.bR() && (canMerge() || canUnhide());
  }
  
  public void mouseTick(int x, int y, Click c) { if (c.bR()) c.onClickEnd(); else super.mouseTick(x, y, c); }
  
  public boolean canMerge() {
    if (!visible || ch.sz!=2) return false;
    for (Node c : ch) if (!(c instanceof TabbedNode)) return false;
    return true;
  }
  public boolean canUnhide() {
    return ch.filter(n -> n instanceof TabbedNode && ((TabbedNode) n).mode!=TabbedNode.Mode.ALWAYS).sz!=0;
  }
  
  public void mouseUp(int x, int y, Click c) {
    if (c.bR()) {
      PartialMenu m = new PartialMenu(gc);
      if (canMerge()) m.add(gc.getProp("tabbed.mergeMenu").gr(), "base_merge", () -> {
        if (canMerge() && p!=null) {
          TabbedNode t0 = (TabbedNode) ch.get(0);
          TabbedNode t1 = (TabbedNode) ch.get(1);
          Tab toSelect = t0.cTab()==null? t1.cTab() : null;
          for (int i = t1.tabCount(); i-->0; ) {
            Tab t = t1.getTab(i);
            t1.removeTab(i);
            t0.addTab(t);
          }
          p.replace(p.ch.indexOf(this), n -> n.ch.get(0));
          if (toSelect!=null) toSelect.switchTo();
        }
      });
      if (canUnhide()) m.add(gc.getProp("tabbed.unhideAdj").gr(), "base_unhideAdj", () -> {
        for (Node n : ch) {
          if (n instanceof TabbedNode && ((TabbedNode) n).mode!=TabbedNode.Mode.ALWAYS) ((TabbedNode) n).setMode(TabbedNode.Mode.ALWAYS);
        }
      });
      m.open(ctx);
      return;
    }
    super.mouseUp(x, y, c);
  }
  
  private static final String[] k_dir = new String[]{"dir"};
  private static final Prop[] v_v = new Prop[]{new EnumProp("v")};
  private static final Prop[] v_h = new Prop[]{new EnumProp("h")};
  public static void onTabRightClick(PartialMenu m, Tab t) {
    m.add(t.ctx.gc.getProp("tabbed.splitMenu").gr(), s -> {
      boolean v, r; // vertical, reverse
      switch (s) {
        default: return false;
        case "base_splitR": r=false; v=false; break; 
        case "base_splitD": r=false; v=true;  break;
        case "base_splitL": r=true;  v=false; break;
        case "base_splitU": r=true;  v=true;  break;
      }
      TabbedNode w = t.w.o;
      Node wp = w.p;
      if (t.w.visible && wp!=null) {
        w.removeTab(w.tabIndex(t));
        WindowSplitNode sp = new WindowSplitNode(wp.ctx, k_dir, v? v_v : v_h);
        TabbedNode t1 = new TabbedNode(wp.ctx, KS_NONE, VS_NONE);
        
        wp.replace(wp.ch.indexOf(w), t0 -> {
          sp.add(r? t1 : t0);
          sp.add(r? t0 : t1);
          return sp;
        });
        
        t1.addSelectedTab(t);
      }
      return true;
    });
  }
  
}