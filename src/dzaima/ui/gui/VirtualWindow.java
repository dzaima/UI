package dzaima.ui.gui;

import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.utils.Rect;
import io.github.humbleui.skija.*;

public abstract class VirtualWindow {
  public static boolean DEBUG_REDRAW = false;
  
  public final NodeWindow w;
  public Graphics g;
  public Rect rect;
  
  protected VirtualWindow(NodeWindow w) { this.w = w; }
  
  public abstract boolean fullyOpaque(); // hint to allow skipping drawing things below this
  public abstract boolean drawShadow();
  protected abstract Rect getLocation(int pw, int ph);
  protected abstract void implDraw(Graphics g, boolean full);
  protected abstract boolean implRequiresRedraw();
  
  public final boolean requiresRedraw() {
    return implRequiresRedraw();
  }
  
  public final void newParentSize(int pw, int ph) {
    newRect(getLocation(pw, ph));
  }
  public final void newRect() {
    newRect(getLocation(w.w, w.h));
  }
  public final void newRect(Rect nr) {
    Rect pr = rect;
    if (!nr.equals(pr)) {
      rect = nr;
      newSize();
    }
  }
  
  private Surface lastParentSurface;
  public final boolean renderSelf(Graphics parent) {
    Surface ps = parent.currSurface();
    boolean refresh = ps!=lastParentSurface;
    if (refresh) {
      lastParentSurface = ps;
      if (g!=null) g.close();
      ImageInfo ii = ps.getImageInfo().withWidthHeight(Math.max(1, rect.w()), Math.max(1, rect.h()));
      if (fullyOpaque()) ii = ii.withColorAlphaType(ColorAlphaType.OPAQUE);
      g = new OffscreenGraphics(ps, ii);
    }
    
    boolean req = requiresRedraw();
    if (req) implDraw(g, refresh);
    if (DEBUG_REDRAW) g.rect(0, 0, g.w, g.h, 0x10000000);
    return req;
  }
  
  public void drawTo(Graphics pg) {
    assert g instanceof OffscreenGraphics;
    ((OffscreenGraphics) g).drawTo(pg, rect.sx, rect.sy);
  }
  
  public abstract boolean ownsXY(int x, int y);
  public abstract void mouseStart(Click cl);
  public abstract void initialMouseTick(Click c);
  public abstract void scroll(float dx, float dy);
  public boolean key(Key key, int scancode, KeyAction a) {
    Node n = w.focusNode();
    return n!=null && n.keyF(key, scancode, a);
  }
  public abstract void typed(int p);
  
  public abstract void started();
  
  public abstract void newSize();
  public abstract void maybeResize();
  
  public abstract void eventTick();
  public abstract void tick();
  
  public abstract Window.CursorType cursorType();
  
  public abstract boolean shouldRemove();
  public /*open*/ void stopped() {
    if (g!=null) g.close();
  }
}
