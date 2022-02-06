package dzaima.ui.gui;

import io.github.humbleui.skija.*;

public class OffscreenGraphics extends Graphics {
  public final Surface surface;
  
  public OffscreenGraphics(Surface ps, int w, int h) {
    surface = ps.makeSurface(w, h);
    if (surface==null) throw new RuntimeException("Couldn't create off-screen surface");
    init(surface);
  }
  
  public void close() {
    surface.close();
  }
  
  public void drawTo(Canvas c, int x, int y) {
    surface.draw(c, x, y, null);
  }
}
