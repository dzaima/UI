package dzaima.ui.gui.jwm;

import dzaima.ui.gui.WindowImpl;
import dzaima.ui.gui.io.*;
import dzaima.utils.Time;
import dzaima.utils.Log;
import io.github.humbleui.jwm.Key;
import io.github.humbleui.jwm.*;
import io.github.humbleui.types.IRect;

import java.util.function.*;

import static dzaima.ui.gui.io.Key.*;

public class JWMEventHandler implements Consumer<Event> {
  public final JWMWindow w;
  private final dzaima.ui.gui.Window ww;
  public final Window jwmw;
  public int wx, wy;
  public boolean running;
  
  public JWMEventHandler(JWMWindow w) {
    this.w = w;
    this.ww = w.w;
    this.jwmw = w.jwmw;
    running = true;
  }
  
  public void accept(Event ev) {
    try {
      accept0(ev);
    } catch (Exception e) {
      dzaima.ui.gui.Window.onFrameError(ww, e);
    }
  }
  private void accept0(Event ev) { // TODO this is willy nilly immediately invoking functions on the window without going through the event queue to execute at the proper time
    if (ev instanceof EventFrame) {
      if (w.visible) {
        if (JWMWindow.DEBUG_UPDATES) Log.info(Time.logStart(w.id)+"EventFrame");
        paint();
      }
    } else if (ev instanceof EventWindowScreenChange) {
      if (w.visible) {
        w.layer.reconfigure();
        IRect r = jwmw.getContentRect();
        w.layer.resize(r.getWidth(), r.getHeight());
        paint();
      }
    } else if (ev instanceof EventWindowResize) {
      EventWindowResize e = (EventWindowResize) ev;
      ww.w = e.getContentWidth();
      ww.h = e.getContentHeight();
      w.layer.resize(ww.w, ww.h);
      w.w.updateSize.set(true);
      // eventFrame will appear later, so not requesting tick is fine
    } else if (ev instanceof EventMouseScroll) {
      EventMouseScroll e = (EventMouseScroll) ev;
      w.enqueue(() -> ww.scroll(e.getDeltaX()/15f, e.getDeltaY()/15f, e.isModifierDown(KeyModifier.SHIFT)));
    } else if (ev instanceof EventWindowCloseRequest) {
      w.w.closeRequested();
      w.requestTick();
    } else if (ev instanceof EventMouseMove) { // waiting on https://github.com/HumbleUI/JWM/issues/146
      EventMouseMove e = (EventMouseMove) ev;
      int mx = e.getX();
      int my = e.getY();
      ww.dx+= mx-ww.mx;
      ww.dy+= my-ww.my;
      ww.mx = mx;
      ww.my = my;
      w.requestTick();
    } else if (ev instanceof EventWindowMove) {
      EventWindowMove e = (EventWindowMove) ev;
      wx = e._windowLeft;
      wy = e._windowTop;
      w.requestTick();
    } else if (ev instanceof EventMouseButton) {
      EventMouseButton e = (EventMouseButton) ev;
      MouseButton b = e.getButton();
      int num = b==MouseButton.PRIMARY? Click.LEFT
              : b==MouseButton.MIDDLE? Click.CENTER
              : b==MouseButton.SECONDARY? Click.RIGHT
              : b==MouseButton.BACK? Click.BACK
              : b==MouseButton.FORWARD? Click.FORWARD : 99;
      if (num<=Click.FORWARD) {
        Click c = ww.btns[num];
        c.mod = mod(e::isModifierDown);
        c.down = e.isPressed();
        if (c.down) ww.mouseDown(c);
        else ww.mouseUp(ww.mx, ww.my, c);
      }
      w.requestTick();
    } else if (ev instanceof EventKey) {
      EventKey e = (EventKey) ev;
      Key k = e.getKey();
      boolean p = e.isPressed();
      KeyLocation l = e.getLocation();
      boolean handled = ww.key(new dzaima.ui.gui.io.Key(KeyVal.of(k), mod(e::isModifierDown) | (l==KeyLocation.KEYPAD? P_KP : l==KeyLocation.RIGHT? P_RIGHT : 0)), 0, e.isPressed()? KeyAction.PRESS : KeyAction.RELEASE);
      if (k==Key.ESCAPE && WindowImpl.ESC_EXIT) {
        if (p) escPressHandled = handled;
        else if (!handled && !escPressHandled) ww.closeOnNext();
      }
      w.requestTick();
    } else if (ev instanceof EventTextInput) {
      String s = ((EventTextInput) ev).getText();
      int i = 0;
      while (i < s.length()) {
        int p = s.codePointAt(i);
        ww.typed(p);
        i+= Character.charCount(p);
      }
      w.requestTick();
    } else if (ev instanceof EventWindowFocusIn) {
      w.w.focused();
      w.requestTick();
    } else if (ev instanceof EventWindowFocusOut) {
      w.w.unfocused();
      w.requestTick();
    } else if (ev instanceof EventWindowClose) {
      w.requestTick();
    } else {
      Log.info("JWM", "Unhandled JWM event "+(ev==null? "null" : ev.getClass().getSimpleName())+":\n"+ev);
    }
  }
  
  private boolean escPressHandled = false;
  public boolean dontPaint = true;
  public void paint() { // returns if keep running
    if (!running) return;
    if (dontPaint) return; // don't recursively paint
    dontPaint = true;
    try {
      if (w.shouldStop.get()) {
        running = false;
        w.stop();
        return;
      }
      w.nextFrame(false);
    } finally {
      dontPaint = false;
    }
  }
  
  
  
  
  private int mod(Predicate<KeyModifier> f) {
    int r = 0;
    if (f.test(KeyModifier.SHIFT)) r|= dzaima.ui.gui.io.Key.M_SHIFT;
    if (f.test(KeyModifier.CONTROL)) r|= dzaima.ui.gui.io.Key.M_CTRL;
    if (f.test(KeyModifier.ALT)) r|= dzaima.ui.gui.io.Key.M_ALT;
    if (f.test(KeyModifier.LINUX_META) || f.test(KeyModifier.LINUX_SUPER) || f.test(KeyModifier.WIN_LOGO)) r|= dzaima.ui.gui.io.Key.M_SUP;
    return r;
  }
}
