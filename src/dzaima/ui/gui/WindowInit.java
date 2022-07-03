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
  
  
  
  public static Rect defaultExtraRect() {
    Rect d = Windows.primaryDisplay();
    return d.centered(d.w()/2, d.h()*9/10);
  }
  public static WindowInit defaultForMain(String name) {
    return new WindowInit(name);
  }
  public static WindowInit defaultForExtra(String name) {
    return new WindowInit(name, defaultExtraRect());
  }
}
