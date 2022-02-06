package dzaima.ui.gui;

import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.utils.*;

public class Menu extends NodeWindow {
  private final Window p;
  private boolean persistent;
  private final Runnable onExit;
  
  public Menu(GConfig gc, Ctx pctx, PNodeGroup g, Window p, boolean persistent, Runnable onExit) {
    super(gc, pctx, g, new WindowInit("(menu)").setType(WindowType.POPUP).setVisible(true));
    this.p = p;
    this.onExit = onExit;
    this.persistent = persistent;
  }
  
  public static Menu auto(Window p, Node n, PNodeGroup g, boolean persistent, Runnable onExit) {
    Menu m = new Menu(n.gc, n.ctx, g, p, persistent, onExit);
    Node base = m.base;
    base.shown(); // TODO this is a little annoying
    int w = base.minW();
    int h = base.minH(w);
    base.hidden();
    NodeWindow pw = n.ctx.win();
    XY pwp = pw.windowPos().add(pw.mx, pw.my);
    m.impl.init.setWindowed(Rect.xywh(pwp.x, pwp.y, w, h));
    pw.impl.mgr.start(m);
    return m;
  }
  
  public void unfocused() {
    super.unfocused();
    onExit.run();
    if (!persistent) closeOnNext();
  }
  
  public void tick() {
    super.tick();
    if (frameCount<60 && frameCount%20==5 && focused) impl.focus();
  }
}
