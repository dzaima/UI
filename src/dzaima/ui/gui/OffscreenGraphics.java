package dzaima.ui.gui;

import io.github.humbleui.skija.*;

public class OffscreenGraphics extends Graphics {
  public Surface surface;
  
  public OffscreenGraphics(Graphics parent, ImageInfo ii) {
    assert ii.getWidth()>0 && ii.getHeight()>0;
    surface = parent.currSurface().makeSurface(ii);
    if (surface==null) throw new RuntimeException("Couldn't create off-screen surface");
    init(surface, parent.winID, parent.reconfigureRef);
  }
  
  public Surface currSurface() {
    return surface;
  }
  
  public void close() {
    surface.close();
  }
  
  public void drawTo(Graphics g, int x, int y) {
    drawTo(g.canvas, x, y);
  }
  public void drawTo(Canvas c, int x, int y) {
    surface.draw(c, x, y, null);
  }
}
