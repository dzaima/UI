package dzaima.ui.gui;

import io.github.humbleui.skija.Surface;

public class MainGraphics extends Graphics {
  private Surface s;
  
  public Surface currSurface() {
    return s;
  }
  
  public void close() { }
  
  public void setSurface(Surface s) {
    this.s = s;
    init(s);
  }
}
