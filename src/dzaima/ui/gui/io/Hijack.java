package dzaima.ui.gui.io;

import dzaima.ui.gui.*;
import dzaima.ui.node.Node;

public interface Hijack {
  default boolean hMouseDown(Click cl) { return false; }
  default void hStopped() { }
  default int hRedraw() { return 0; }
  default boolean hDoHover() { return true; }
  
  
  
  static void set(Window base, Hijack h) {
    base.hijack = h;
  }
  static void clear(Window base, Hijack h) {
    if (base.hijack==h) base.hijack = null;
  }
  static Node hoveredNode(NodeVW vw) {
    Node c = vw.base;
    int x = vw.w.mx - vw.rect.sx;
    int y = vw.w.my - vw.rect.sy;
    while (true) {
      x-= c.dx;
      y-= c.dy;
      Node n = c.findCh(x, y);
      if (n==null) break;
      c = n;
    }
    return c;
  }
}
