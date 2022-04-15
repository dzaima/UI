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
  
  protected void close() { closeRequested = true; }
  protected XY pos() {
    return new XY(startX, startY+1);
  }
  protected XY getSize() {
    int w = node.minW();
    int h = node.minH(w);
    return new XY(w, h);
  }
  
  protected boolean key(Key key, KeyAction a) { return false; }
  
  public void menuItem(String id) {
    throw new RuntimeException("menuItem not overridden! (ID = \""+id+"\")");
  }
  
  
  public void open(GConfig gc, Ctx ctx, PNodeGroup g) {
    openVW(gc, ctx, g);
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
  
  
  
  public void openVW(GConfig gc, Ctx ctx, PNodeGroup g) {
    VirtualMenu vw = new VirtualMenu(gc, ctx, g, this);
    isVW = true;
    pw.addVW(vw);
    pw.focusVW(vw);
    
    setup();
  }
  
  public void openWindow(GConfig gc, Ctx ctx, PNodeGroup g, String title) {
    MenuWindow win = new MenuWindow(gc, ctx, g, title, this);
    Node base = win.base;
    
    base.shown(); // TODO this is a little annoying
    base.hidden();
    
    XY sz = getSize();
    XY sp = pos();
    XY wp = pw.windowPos();
    win.impl.init.setWindowed(Rect.xywh(wp.x+sp.x, wp.y+sp.y, sz.x, sz.y));
    pw.impl.mgr.start(win);
    
    setup();
  }
  
  
  
  public boolean defaultKeys(Key key, KeyAction a) {
    switch (node.gc.keymap(key, a, "menu")) {
      case "exit":
        close();
        return true;
    }
    return false;
  }
  
  public static class RightClickMenu extends Popup implements Click.RequestImpl {
    private final Consumer<String> action;
    
    public RightClickMenu(Ctx ctx, Consumer<String> action) {
      super(ctx.win());
      this.action = action;
    }
  
    protected void setup() { ((MenuNode) node).obj = this; node.ctx.focus(node); }
  
    protected void unfocused() { close(); }
    
    public void menuItem(String id) {
      action.accept(id);
      close();
    }
    
    protected boolean key(Key key, KeyAction a) { return defaultKeys(key, a); }
    
    public void takeClick(Click c) { c.replace(this, 0, 0); }
    public void mouseDown(int x, int y, Click c) { }
    public void mouseTick(int x, int y, Click c) { }
    public void mouseUp(int x, int y, Click c) { }
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
      if (frameCount<60 && frameCount%20==5 && focused) impl.focus();
      if (m.closeRequested) closeOnNext();
    }
    
    public boolean key(Key key, int scancode, KeyAction a) {
      if (m.key(key, a)) return true;
      return super.key(key, scancode, a);
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
      XY sz = m.getSize();
      XY p = m.pos();
      return Rect.xywh(p.x, p.y, sz.x, sz.y);
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
    
    public boolean key(Key key, int scancode, KeyAction a) {
      if (m.key(key, a)) return true;
      return super.key(key, scancode, a);
    }
  }
}
