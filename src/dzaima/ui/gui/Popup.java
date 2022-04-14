package dzaima.ui.gui;

import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.types.MenuNode;
import dzaima.utils.*;

import java.util.function.Consumer;

public abstract class Popup {
  public Node node;
  public final NodeWindow pw;
  private final int startX, startY;
  public Popup(NodeWindow pw) {
    this.pw = pw;
    startX = pw.mx;
    startY = pw.my;
  }
  
  protected abstract void unfocused();
  
  private boolean closeRequested;
  protected void close() { closeRequested = true; }
  
  public void menuItem(String id) {
    throw new RuntimeException("menuItem not overridden! (ID = \""+id+"\")");
  }
  
  
  public void open(GConfig gc, Ctx ctx, PNodeGroup g) {
    openVW(gc, ctx, g);
    // openWindow(gc, ctx, g);
    
    if (node instanceof MenuNode) {
      ((MenuNode) node).obj = this;
    }
  }
  public static void rightClickMenu(GConfig gc, Ctx ctx, PNodeGroup g, Consumer<String> action) {
    Popup m = new Popup(ctx.win()) {
      protected void unfocused() { close(); }
    
      public void menuItem(String id) {
        action.accept(id);
        close();
      }
    };
    m.open(gc, ctx, g);
  }
  public static void rightClickMenu(GConfig gc, Ctx ctx, String path, Consumer<String> action) {
    rightClickMenu(gc, ctx, gc.getProp(path).gr(), action);
  }
  
  
  
  public void openVW(GConfig gc, Ctx ctx, PNodeGroup g) {
    VirtualMenu vw = new VirtualMenu(gc, ctx, g, this);
    pw.addVW(vw);
    pw.focusVW(vw);
  }
  
  public void openWindow(GConfig gc, Ctx ctx, PNodeGroup g) {
    MenuWindow win = new MenuWindow(gc, ctx, g, this);
    Node base = win.base;
    
    base.shown(); // TODO this is a little annoying
    int w = base.minW();
    int h = base.minH(w);
    base.hidden();
    
    XY wp = pw.windowPos();
    win.impl.init.setWindowed(Rect.xywh(wp.x+ startX, wp.y+ startY, w, h));
    pw.impl.mgr.start(win);
  }
  
  
  
  
  
  static class MenuWindow extends NodeWindow {
    private final Popup m;
    
    public MenuWindow(GConfig gc, Ctx pctx, PNodeGroup g, Popup m) {
      super(gc, pctx, g, new WindowInit("(menu)").setType(WindowType.POPUP).setVisible(true));
      this.m = m;
      m.node = base;
    }
    
    public void unfocused() {
      super.unfocused();
      m.unfocused();
      if (m.closeRequested) closeOnNext();
    }
    
    public void tick() {
      super.tick();
      if (frameCount<60 && frameCount%20==5 && focused) impl.focus();
      if (m.closeRequested) closeOnNext();
    }
  }
  
  static class VirtualMenu extends NodeVW {
    private final Popup m;
    
    public VirtualMenu(GConfig gc, Ctx pctx, PNodeGroup g, Popup m) {
      super(m.pw, gc, pctx, g);
      this.m = m;
      m.node = base;
    }
    public boolean fullyOpaque() { return true; }
    public boolean drawShadow() { return true; }
    public boolean ownsXY(int x, int y) { return true; }
    
    protected Rect getSize(int pw, int ph) {
      int w = base.minW();
      int h = base.minH(w);
      return Rect.xywh(m.startX, m.startY, w, h);
    }
    boolean lastFocused = true;
    public void tick() {
      super.tick();
      boolean focused = m.pw.focusedVW == this;
      if (!focused && lastFocused) m.unfocused();
    }
    
    public boolean shouldRemove() {
      return m.closeRequested;
    }
  }
}
