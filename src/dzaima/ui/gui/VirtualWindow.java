package dzaima.ui.gui;

import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.utils.*;
import io.github.humbleui.skija.*;

public abstract class VirtualWindow {
  public static boolean DEBUG_REDRAW = false;
  
  private Surface lastSurface;
  public Graphics g;
  public final NodeWindow w;
  private boolean newCanvas;
  public Rect rect;
  
  protected VirtualWindow(NodeWindow w) { this.w = w; }
  
  public abstract boolean fullyOpaque(); // hint to allow skipping drawing things below this
  public abstract boolean drawShadow();
  protected abstract Rect getLocation(int pw, int ph);
  protected abstract void implDraw(Graphics g, boolean full);
  protected abstract boolean implRequiresRedraw();
  
  public final boolean requiresRedraw() {
    return implRequiresRedraw() || newCanvas;
  }
  
  public final void newSurface(Surface s, int pw, int ph) {
    lastSurface = s;
    newRect(getLocation(pw, ph), true);
  }
  public final void newRect(Rect nr, boolean newCanvas) {
    Rect pr = rect;
    if (newCanvas || !nr.equals(pr)) {
      rect = nr;
      if (pr==null || pr.w()!=nr.w() || pr.h()!=nr.h() || newCanvas) {
        if (g!=null) g.close();
        ImageInfo ii = lastSurface.getImageInfo().withWidthHeight(Math.max(1, nr.w()), Math.max(1, nr.h()));
        if (fullyOpaque()) ii = ii.withColorAlphaType(ColorAlphaType.OPAQUE);
        g = new OffscreenGraphics(lastSurface, ii);
        this.newCanvas = true;
        newSize();
      }
    }
  }
  public final void newRect() {
    newRect(getLocation(w.w, w.h), false);
  }
  
  public final boolean draw() {
    boolean req = requiresRedraw();
    if (req) {
      implDraw(g, newCanvas);
      newCanvas = false;
    }
    if (DEBUG_REDRAW) g.rect(0, 0, g.w, g.h, 0x10000000);
    return req;
  }
  
  public void drawTo(Graphics pg) {
    if (g instanceof OffscreenGraphics) ((OffscreenGraphics) g).drawTo(pg, rect.sx, rect.sy);
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
    g.close();
  }
}
