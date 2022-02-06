package dzaima.ui.gui;

import dzaima.utils.Rect;

public class WindowInit {
  public String title;
  public boolean visible = true;
  public Window.WindowType type = Window.WindowType.NORMAL;
  public Rect rect; // if null, maximized
  
  public WindowInit(String title, Rect windowed) {
    this.title = title;
    this.rect = windowed;
  }
  public WindowInit(String title) { // maximized
    this.title = title;
    rect = null;
  }
  
  public WindowInit setVisible(boolean v) {
    this.visible = v;
    return this;
  }
  
  public WindowInit setType(Window.WindowType type) {
    this.type = type;
    return this;
  }
  
  public WindowInit setWindowed(Rect r) {
    this.rect = r;
    return this;
  }
  public WindowInit setMaximized() {
    this.rect = null;
    return this;
  }
}
