package dzaima.ui.node.types.tabs;

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
  public /*open*/ void hide() { }
  
  public abstract String name();
  public final void nameUpdated() {
    if (w!=null) w.nameUpdated();
  }
  
  public /*open*/ void onRightClick() { }
}
