package dzaima.ui.node.types.inputs;

import dzaima.ui.gui.*;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.utils.*;

import java.util.function.Consumer;

public class ColorPickerNode extends Node {
  public ColorValue value = ColorValue.ofARGB(0xffFF00FF);
  
  public enum Event { OPEN, CHANGE, CLOSE }
  public void setEventHandler(Consumer<Event> e) { onEvent = e; }
  
  
  public ColorPickerNode(Ctx ctx, Props props) {
    super(ctx, props);
    value.onSet.add(() -> {
      event(Event.CHANGE);
      mRedraw();
    });
  }
  
  public void update(int col) {
    value.setARGB(col);
  }
  
  private Consumer<Event> onEvent;
  private void event(Event e) {
    if (onEvent!=null) onEvent.accept(e);
  }
  
  private int sz;
  public void propsUpd() {
    super.propsUpd();
    sz = prop("sz").len();
  }
  
  public int sz() { return sz; }
  public int minW() { return sz(); } public int minH(int w) { return sz(); }
  public int maxW() { return sz(); } public int maxH(int w) { return sz(); }
  
  public void drawC(Graphics g) {
    g.rect(0, 0, w, h, prop("borderCol").col());
    int b = prop("borderW").len();
    g.rect(b, b, w-b, w-b, value.argb);
  }
  
  public Vec<Prop> getProps() {
    return addRootProps(super.getProps(), "sz", "borderCol", "borderW");
  }
  
  public Prop prop(String name) {
    Prop p = getPropN(name);
    return p!=null? p : gc.getProp("colorpicker."+name);
  }
  
  public Vec<Prop> addRootProps(Vec<Prop> props, String root, String... keys) {
    for (String k : keys) if (getPropN(k)==null) props.add(gc.getCfgProp(root+"."+k));
    return props;
  }
  
  public void mouseStart(int x, int y, Click c) { if (c.bL()) c.register(this, x, y); }
  public void mouseTick(int x, int y, Click c) { c.onClickEnd(); }
  public void mouseUp(int x, int y, Click c) {
    event(Event.OPEN);
    new Popup(ctx.win()) {
      FullColorPickerNode m;
      final Runnable onSet = () -> m.onSet();
      
      public void stopped() { value.onSet.remove(onSet); event(Event.CLOSE); }
      protected void unfocused() { close(); }
      protected void setup() { }
      protected void preSetup() {
        m = new FullColorPickerNode(node.ctx, value);
        node.add(m);
        value.onSet.add(onSet);
      }
      
      protected boolean key(Key key, KeyAction a) {
        switch (gc.keymap(key, a, "colorpicker")) {
          case "exit": close(); return true;
          default: return super.key(key, a);
        }
      }
    }.openVW(gc, ctx, gc.getProp("colorpicker.full").gr(), true);
  }
  
  public void hoverS() { ctx.vw().pushCursor(Window.CursorType.HAND); }
  public void hoverE() { ctx.vw().popCursor(); }
  
  
  public static class ColorValue {
    public int argb;
    float h, x, y;
    private ColorValue(int argb) {
      setARGB(argb);
    }
    
    public Vec<Runnable> onSet = new Vec<>();
    public void setARGB(int argb) {
      this.argb = argb;
      ColorUtils.RGBtoHSB(argb, buf);
      h = buf[0];
      x = buf[1];
      y = 1-buf[2];
      for (Runnable r : onSet) r.run();
    }
    public void setFromXY(float h0, float x0, float y0) {
      setFromXY(ColorUtils.alpha(argb), h0, x0, y0);
    }
    public void setFromXY(int alpha, float h0, float x0, float y0) {
      h = Tools.constrain(h0, 0, 1);
      x = Tools.constrain(x0, 0, 1);
      y = Tools.constrain(y0, 0, 1);
      argb = getForXY(Tools.constrain(alpha, 0, 255), x, y);
      for (Runnable r : onSet) r.run();
    }
    
    public int getForXY(int alpha, float x, float y) {
      return ColorUtils.HSBtoRGB(alpha, h, x, 1-y);
    }
    
    private static final float[] buf = new float[3];
    public static ColorValue ofARGB(int argb) {
      return new ColorValue(argb);
    }
  }
}
