package dzaima.ui.gui;

import io.github.humbleui.skija.*;

public class OffscreenGraphics extends Graphics {
  public Surface surface;
  
  public OffscreenGraphics(Surface ps, int w, int h) {
    assert w>0 && h>0;
    surface = ps.makeSurface(w, h);
    if (surface==null) throw new RuntimeException("Couldn't create off-screen surface");
    init(surface);
  }
  public OffscreenGraphics(Surface ps, ImageInfo ii) {
    assert ii.getWidth()>0 && ii.getHeight()>0;
    surface = ps.makeSurface(ii);
    if (surface==null) throw new RuntimeException("Couldn't create off-screen surface");
    init(surface);
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
