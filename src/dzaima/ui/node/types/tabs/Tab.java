package dzaima.ui.node.types.tabs;

import dzaima.ui.gui.PartialMenu;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;

public abstract class Tab {
  public TabWrapper w;
  public final Ctx ctx;
  public boolean open;
  
  public Tab(Ctx ctx) {
    this.ctx = ctx;
  }
  
  public abstract Node show();
  public /*open*/ void onShown() { } // called once after show()
  public /*open*/ void onHidden() { } // called once after onShown()
  
  public abstract String name();
  public final void nameUpdated() {
    if (w!=null) w.nameUpdated();
  }
  
  public void addMenuBarOptions(PartialMenu m) {
    boolean wm = w.o.mode == TabbedNode.Mode.WHEN_MULTIPLE;
    if (wm || w.o.tabCount()==1) m.add(ctx.gc.getProp(wm? "tabbed.unhideBar" : "tabbed.hideBar").gr(), s -> {
      switch (s) {
        case "base_hideBar": w.o.setMode(TabbedNode.Mode.WHEN_MULTIPLE); return true;
        case "base_unhideBar": w.o.setMode(TabbedNode.Mode.ALWAYS); return true;
        default: return false;
      }
    });
  }
  
  public /*open*/ void switchTo() { // explicit selection; override to run an action on any selection of this tab
    w.o.toTab(w);
  }
  
  public /*open*/ void onRightClick(Click cl) { }
  
  protected final void onRightClick() { throw new IllegalStateException(); } // deprecated version of the above
}
