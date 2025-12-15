package dzaima.ui.gui;

import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.types.MenuNode;
import dzaima.utils.*;

import java.util.function.Consumer;

public abstract class Popup {
  public Node node;
  public final NodeWindow pw;
  
  private final int startX, startY;
  private boolean closeRequested;
  protected boolean isVW;
  
  public Popup(NodeWindow pw) {
    this.pw = pw;
    startX = pw.mx;
    startY = pw.my;
  }
  
  protected abstract void unfocused();
  protected abstract void setup();
  protected void preSetup() { } // maybe can just move setup to the place of preSetup?
  protected void tick() { }
  
  public void close() { closeRequested = true; } // request to be closed
  public void stopped() { } // invoked once when closing for any reason
  
  protected Rect fullRect() { return null; }
  public Rect centered(VirtualWindow vw, double fx, double fy) {
    Rect pr = vw.rect;
    int w = Math.max(node.minW( ), (int) (pr.w()*fx));
    int h = Math.max(node.minH(w), (int) (pr.h()*fy));
    return pr.centered(w, h);
  }
  
  protected XY pos(XY size, Rect bounds) {
    Rect r = fullRect();
    if (r!=null) return new XY(r.sx, r.sy);
    return new XY(Math.max(0, Math.min(startX+1, bounds.ex-size.x)),
                  Math.max(0, Math.min(startY+1, bounds.ey-size.y)));
  }
  protected XY getSize() {
    Rect r = fullRect();
    if (r!=null) return new XY(r.w(), r.h());
    int w = node.minW();
    int h = node.minH(w);
    return new XY(w, h);
  }
  
  protected boolean key(Key key, KeyAction a) { return false; }
  
  public void menuItem(String id) {
    throw new RuntimeException("menuItem not overridden! (ID = \""+id+"\")");
  }
  
  
  public void open(GConfig gc, Ctx ctx, PNodeGroup g) {
    openVW(gc, ctx, g, true);
    // openWindow(gc, ctx, g, null);
  }
  public static RightClickMenu rightClickMenu(GConfig gc, Ctx ctx, PNodeGroup g, Consumer<String> action) {
    RightClickMenu m = new RightClickMenu(ctx, action);
    m.open(gc, ctx, g);
    return m;
  }
  public static RightClickMenu rightClickMenu(GConfig gc, Ctx ctx, String path, Consumer<String> action) {
    return rightClickMenu(gc, ctx, gc.getProp(path).gr(), action);
  }
  
  
  
  public VirtualMenu openVW(GConfig gc, Ctx ctx, PNodeGroup g, boolean focus) {
    VirtualMenu vw = new VirtualMenu(gc, ctx, g, this, focus);
    isVW = true;
    preSetup();
    pw.addVW(vw);
    if (focus) pw.focusVW(vw);
    
    setup();
    return vw;
  }
  
  public MenuWindow openWindow(GConfig gc, Ctx ctx, PNodeGroup g, String title) {
    MenuWindow win = new MenuWindow(gc, ctx, g, title, this);
    Node base = win.base;
    
    base.shown(); // TODO this is a little annoying
    base.hidden();
    
    XY sz = getSize();
    XY sp = pos(sz, new Rect(-Tools.BIG, -Tools.BIG, Tools.BIG, Tools.BIG));
    XY wp = pw.windowPos();
    win.impl.init.setWindowed(Rect.xywh(wp.x+sp.x, wp.y+sp.y, sz.x, sz.y));
    pw.impl.mgr.start(win);
    
    setup();
    return win;
  }
  
  
  
  public boolean defaultKeys(Key key, KeyAction a) {
    switch (node.gc.keymap(key, a, "menu")) {
      case "exit": close(); return true;
      default: return false;
    }
  }
  
  public static class RightClickMenu extends Popup implements Click.RequestImpl {
    private final Consumer<String> action;
    
    public RightClickMenu(Ctx ctx, Consumer<String> action) {
      super(ctx.win());
      this.action = action;
    }
    
    protected void setup() { ((MenuNode) node).obj = this; node.ctx.focus(node); }
    
    protected void unfocused() { close(); }
    
    public void stopped() { action.accept("(closed)"); }
    
    public void menuItem(String id) {
      action.accept(id);
      close();
    }
    
    protected boolean key(Key key, KeyAction a) { return defaultKeys(key, a); }
    
    public void takeClick(Click c) { c.replace(this, 0, 0); }
    public void mouseDown(int x, int y, Click c) { }
    public void mouseTick(int x, int y, Click c) { }
    public void mouseUp(int x, int y, Click c) {
      if (!gc().getProp("menu.rightClickImplicitClick.enabled").b()) return;
      if ( gc().getProp("menu.rightClickImplicitClick.minDist").len() > c.distFromStart()) return;
      if ( gc().getProp("menu.rightClickImplicitClick.minTime").d()   > c.msTimeFromStart()/1e3) return;
      Node n = node.findCh(x, y);
      if (n instanceof MenuNode.MINode) ((MenuNode.MINode) n).run();
    }
    public GConfig gc() { return node.gc; }
    public XY relPos(Node nullArgument) { return XY.ZERO; }
  }
  
  
  
  static class MenuWindow extends NodeWindow {
    private final Popup m;
    
    public MenuWindow(GConfig gc, Ctx pctx, PNodeGroup g, String title, Popup m) {
      super(gc, pctx, g, new WindowInit(title==null? "(menu)" : title).setType(title==null? WindowType.POPUP : WindowType.NORMAL).setVisible(true));
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
      m.tick();
      if (frameCount<60 && frameCount%20==5 && focused) impl.focus();
      if (m.closeRequested) closeOnNext();
    }
    
    public void stopped() {
      super.stopped();
      m.stopped();
    }
    
    public boolean key(Key key, int scancode, KeyAction a) {
      if (m.key(key, a)) return true;
      return super.key(key, scancode, a);
    }
  }
  
  static class VirtualMenu extends NodeVW {
    private final Popup m;
    
    public VirtualMenu(GConfig gc, Ctx pctx, PNodeGroup g, Popup m, boolean lastFocused) {
      super(m.pw, gc, pctx, g);
      this.lastFocused = lastFocused;
      this.m = m;
      m.node = base;
    }
    public boolean fullyOpaque() { return true; }
    public boolean drawShadow() { return true; }
    public boolean ownsXY(int x, int y) { return true; }
    
    public void stopped() {
      super.stopped();
      m.stopped();
    }
    
    protected Rect getLocation(int pw, int ph) {
      XY sz = m.getSize();
      XY p = m.pos(sz, new Rect(0, 0, m.pw.w, m.pw.h));
      return Rect.xywh(p.x, p.y, sz.x, sz.y);
    }
    boolean lastFocused = true;
    public void tick() {
      super.tick();
      m.tick();
      boolean focused = m.pw.focusedVW == this;
      if (!focused && lastFocused) m.unfocused();
    }
    
    public boolean shouldRemove() {
      return m.closeRequested;
    }
    
    public boolean key(Key key, int scancode, KeyAction a) {
      if (m.key(key, a)) return true;
      return super.key(key, scancode, a);
    }
  }
}
