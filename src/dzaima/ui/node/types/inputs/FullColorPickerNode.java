package dzaima.ui.node.types.inputs;

import dzaima.ui.gui.*;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.WrapNode;
import dzaima.utils.*;
import io.github.humbleui.skija.*;

public class FullColorPickerNode extends WrapNode {
  public final ColorPickerNode.ColorValue val;
  private final Node place;
  
  public FullColorPickerNode(Ctx ctx, Props props) {
    this(ctx, props, ColorPickerNode.ColorValue.ofARGB(0xffFF00FF), true);
  }
  
  public FullColorPickerNode(Ctx ctx, ColorPickerNode.ColorValue val) {
    this(ctx, Props.none(), val, false);
  }
  
  FullColorPickerNode(Ctx ctx, Props props, ColorPickerNode.ColorValue val, boolean owned) {
    super(ctx, props);
    this.val = val;
    Node n = ctx.make(gc.getProp("colorpicker.fullUI").gr());
    place = n.ctx.id("boardPlace");
    place.add(new ColorBoard(ctx, this));
    place.add(new ColorSlider(ctx, this, false));
    place.add(new ColorSlider(ctx, this, true));
    add(n);
    if (owned) val.onSet.add(this::onSet);
  }
  
  static void drawSelected(PickerExtra n, Graphics g, int over, float x, float y) {
    float br = ColorUtils.brightness(over);
    Paint color = new Paint().setColor(ColorUtils.bw(255, br<0.06? 0.16f : 0f)).setStroke(true).setStrokeWidth(Tools.ceil(n.gc.em/20f));
    g.push();
    g.clip(0, 0, n.w, n.h);
    g.circle(x, y, n.gc.em*0.3f, color);
    g.pop();
  }
  
  public void onSet() {
    mRedraw();
    for (Node c : place.ch) ((PickerExtra) c).newColor();
  }
  
  static class ColorSlider extends PickerExtra {
    private final boolean a;
    private Image img;
    public ColorSlider(Ctx ctx, FullColorPickerNode m, boolean a) { super(ctx, m); this.a=a; }
    public void hoverS() { ctx.vw().pushCursor(Window.CursorType.EW_RESIZE); }
    public void hoverE() { ctx.vw().popCursor(); }
    
    public int minH(int w) { return gc.em&~1; } // odd height results in the center row color fighting
    public int maxH(int w) { return gc.em&~1; }
    
    public void mouseTick(float x, float y) {
      if (a) m.val.setFromXY((int) (x*255f/w), m.val.h, m.val.x, m.val.y);
      else m.val.setFromXY(x/w, m.val.x, m.val.y);
    }
    protected void resized() { newColor(); }
    private float selX() { return a? ColorUtils.alpha(m.val.argb)/255f*w : m.val.h*w; }
    public void mouseDown(int x, int y, Click c) { super.mouseDown(x, y, c); px=selX(); }
    public void drawC(Graphics g) {
      g.rect(0, 0, w, h, 0xffff00ff);
      g.image(img, 0, 0, w, h, Graphics.Sampling.NEAREST);
      drawSelected(this, g, a? bwCol(ColorUtils.alpha(m.val.argb)/255f, true) : 0xff000000, selX(), h/2f);
    }
    private int bwCol(float f, boolean v) {
      return ColorUtils.lerp(ColorUtils.bw(255, v? 255 : 200), m.val.argb, f);
    }
    protected void newColor() {
      int ih = a? 2 : 1;
      byte[] bs = new byte[w*ih*4];
      int o = 0;
      for (int y = 0; y < ih; y++) {
        for (int x = 0; x < w; x++) {
          float xf = x*1f/w;
          int col = a? bwCol(xf, y>0 ^ x%h>=h/2) : ColorUtils.HSBtoRGB(255, xf, 1, 1);
          bs[o  ] = ColorUtils.blueB (col);
          bs[o+1] = ColorUtils.greenB(col);
          bs[o+2] = ColorUtils.redB  (col);
          bs[o+3] = -1; // a
          o+= 4;
        }
      }
      img = Image.makeRasterFromBytes(ImageInfo.makeN32(w, ih, ColorAlphaType.OPAQUE, ColorSpace.getSRGB()), bs, w*4L);
    }
  }
  
  static class ColorBoard extends PickerExtra {
    Image img;
    public ColorBoard(Ctx ctx, FullColorPickerNode m) { super(ctx, m); }
    
    private final float hf = 0.7f;
    public int minW() { return 10; }
    public int minH(int w) { return Tools.ceil(w*hf); }
    public int maxH(int w) { return Tools.ceil(w*hf); }
    
    protected void newColor() {
      byte[] bs = new byte[w*h*4];
      int o = 0;
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          float xf = x*1f/w;
          float yf = y*1f/h;
          int col = m.val.getForXY(255, xf, yf);
          bs[o  ] = ColorUtils.blueB (col);
          bs[o+1] = ColorUtils.greenB(col);
          bs[o+2] = ColorUtils.redB  (col);
          bs[o+3] = -1; // a
          o+= 4;
        }
      }
      img = Image.makeRasterFromBytes(ImageInfo.makeN32(w, h, ColorAlphaType.OPAQUE, ColorSpace.getSRGB()), bs, w*4L);
    }
    
    public void mouseDown(int x, int y, Click c) { super.mouseDown(x, y, c); px=m.val.x*w; py=m.val.y*h; }
    public void mouseTick(float x, float y) { m.val.setFromXY(m.val.h, x/w, y/h); }
    
    public void hoverS() { ctx.vw().pushCursor(Window.CursorType.CROSSHAIR); }
    public void hoverE() { ctx.vw().popCursor(); }
    
    protected void resized() { newColor(); }
    public void drawC(Graphics g) {
      g.image(img, 0, 0);
      drawSelected(this, g, m.val.argb, m.val.x*w, m.val.y*h);
    }
  }
  
  static abstract class PickerExtra extends Node {
    FullColorPickerNode m;
    public PickerExtra(Ctx ctx, FullColorPickerNode m) { super(ctx, Props.none()); this.m = m; }
    protected abstract void newColor();
    public void mouseStart(int x, int y, Click c) { if (c.bL()) c.register(this, x, y); }
    boolean anyShift, first;
    float px, py;
    public void mouseDown(int x, int y, Click c) { anyShift=false; first=false; }
    public void mouseTick(int x, int y, Click c) {
      boolean shift = Key.shift(ctx.win().keyMod);
      if (first) first = false;
      else if (shift) anyShift = true;
      float f = shift? 0.2f : 1f;
      px = anyShift? (px+(c.dx)*f) : x;
      py = anyShift? (py+(c.dy)*f) : y;
      mouseTick(px, py);
    }
    protected abstract void mouseTick(float x, float y);
  }
}
