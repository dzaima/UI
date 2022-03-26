package dzaima.ui.gui;

import dzaima.utils.Rect;
import io.github.humbleui.skija.Surface;

public abstract class VirtualWindow {
  public static boolean DEBUG_REDRAW = false;
  
  public Graphics g;
  public final NodeWindow w;
  private boolean newCanvas;
  public Rect rect;
  
  protected VirtualWindow(NodeWindow w) { this.w = w; }
  
  public abstract boolean fullyOpaque(); // hint to allow skipping drawing things below this
  protected abstract Rect getSize(int pw, int ph);
  protected abstract void implDraw(Graphics g, boolean full);
  protected abstract boolean implRequiresRedraw();
  
  public final boolean requiresRedraw() {
    return implRequiresRedraw() || newCanvas;
  }
  
  public final void parentResized(Surface s, int pw, int ph) {
    Rect nr = getSize(pw, ph);
    if (!nr.equals(rect)) {
      rect = nr;
      if (g instanceof OffscreenGraphics) ((OffscreenGraphics) g).close();
      g = new OffscreenGraphics(s, rect.w(), rect.h());
      newCanvas = true;
    }
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
  
  public /*open*/ void stopped() {
    if (g instanceof OffscreenGraphics) ((OffscreenGraphics) g).close();
  }
  
  public void drawTo(Graphics pg) {
    if (g instanceof OffscreenGraphics) ((OffscreenGraphics) g).drawTo(pg, rect.sx, rect.sy);
  }
}
