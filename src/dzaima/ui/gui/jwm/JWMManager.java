package dzaima.ui.gui.jwm;

import dzaima.ui.gui.*;
import dzaima.utils.Rect;
import io.github.humbleui.jwm.App;
import io.github.humbleui.skija.impl.Library;

import java.util.function.Consumer;

public class JWMManager extends Windows {
  public JWMManager() {
    if ("false".equals(System.getProperty("skija.staticLoad"))) Library.load();
  }
  
  protected Rect selfPrimaryDisplay() {
    return JWMWindow.primaryDisplay();
  }
  
  protected WindowImpl selfMakeWindow(Window w, WindowInit init) {
    return new JWMWindow(w, init);
  }
  
  protected void startSelf(Consumer<Windows> fn) {
    App.start(() -> fn.accept(this));
  }
  
  public void stopped(Window w) {
    super.stopped(w);
    if (ws.size()==0) App.terminate();
  }
}
