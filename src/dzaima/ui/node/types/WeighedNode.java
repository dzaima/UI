package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.Window.CursorType;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Props;
import dzaima.ui.node.utils.*;
import dzaima.utils.Tools;

public class WeighedNode extends Node {
  public WeighedNode(Ctx ctx, Props props) {
    super(ctx, props);
    weight = gc.fD(this, "weight", 0.5f);
    enabled = gc.boolD(this, "enabled", true);
  }
  
  protected float weight;
  protected boolean v;
  protected int handleWidth, pad, padColor;
  public void propsUpd() {
    super.propsUpd();
    switch (getProp("dir").val()) { default: throw new RuntimeException("Bad uw \"dir\" value "+getProp("dir"));
      case "v": v=true; break;
      case "h": v=false; break;
    }
    handleWidth = gc.len(this, "handleWidth", "uw.handleWidth");
    pad = gc.len(this, "pad", "uw.pad");
    padColor = gc.col(this, "padCol", "uw.padCol");
  }
  
  private float handlePos() { return (v? ch.get(1).dy : ch.get(1).dx) - pad; }
  
  public boolean wantClick(Click c) { return c.bL(); }
  public void mouseStart(int x, int y, Click c) {
    if (wantClick(c) && withinHandle(x, y) && enabled) c.register(this, x, y);
    super.mouseStart(x, y, c);
  }
  
  private boolean withinHandle(int x, int y) {
    return Math.abs((v? y : x) - (handlePos()+pad/2f)) < handleWidth;
  }
  
  boolean dragging;
  public void mouseDown(int x, int y, Click c) {
    if (c.bL()) {
      dragging = false;
      ctx.vw().forceCursor(cursorType());
    }
  }
  public void mouseTick(int x, int y, Click c) {
    if (c.bL()) {
      if (!enabled) c.unregister();
      if (dragging || !gc.isClick(c)) {
        int space = (v? h : w) - pad;
        float w;
        if (dragging) {
          w = weight;
        } else {
          w = handlePos()/space;
          dragging = true;
        }
        w+= (v? c.dy : c.dx) * (1f/space);
        setWeight(Tools.constrain(w, 0, 1));
      }
    }
  }
  
  public void mouseUp(int x, int y, Click c) {
    if (c.bL()) {
      if (visible && !dragging) c.unregister();
      ctx.vw().unforceCursor();
    }
  }
  
  public void drawC(Graphics g) {
    if (pad!=0 && Tools.vs(padColor)) {
      Node c1 = ch.get(1);
      if (v) g.rect(0, c1.dy-pad, w, c1.dy, padColor);
      else   g.rect(c1.dx-pad, 0, c1.dx, h, padColor);
    }
  }
  
  protected boolean enabled = true;
  public boolean isModifiable() { return enabled; }
  public boolean isVertical() { return v; }
  public float getWeight() { return weight; }
  public void setModifiable(boolean enabled) {
    this.enabled = enabled;
  }
  public void setWeight(float w) {
    this.weight = w;
  }
  
  public CursorType cursorType() { return v? CursorType.NS_RESIZE : CursorType.EW_RESIZE; }
  
  private short cPos;
  public void hoverS() { cPos = ctx.vw().pushCursor(null); }
  public void hoverT(int mx, int my) { ctx.vw().replaceCursor(cPos, withinHandle(mx, my) && enabled? cursorType() : null); }
  public void hoverE() { ctx.vw().popCursor(); }
  
  private int left(int tot) { return tot-pad; } // size left for children
  private int c0f(int left, int w, Node c0, Node c1) { // child 0 fill
    int o = Math.round(left*weight); // optimal divider position
    if (v) {
      return Tools.constrain(o, c0.minH(w), left - c1.minH(w));
    } else {
      return Tools.constrain(o, c0.minW(), left - c1.minW());
    }
  }
  
  public int minW() {
    return v? ListUtils.vMinW(ch) : ListUtils.hMinW(ch)+pad; 
  }
  public int minH(int w) {
    if (v) return ListUtils.vMinH(ch, w) + pad;
    Node c0 = ch.get(0);
    Node c1 = ch.get(1);
    int l = left(w);
    int c0f = c0f(l, -1, c0, c1);
    return Math.max(c0.minH(c0f), c1.minH(l-c0f));
  }
  
  private short prevC0f = -1;
  protected void resized() {
    assert ch.sz==2;
    Node c0 = ch.get(0);
    Node c1 = ch.get(1);
    int tot = v? h : w;
    int left = left(tot);
    int c0f = c0f(left, w, c0, c1);
    if (c0f != prevC0f) { // always not equal if c0f doesn't fit in short, but redrawing too much is fine
      mRedraw(); // redraw padding bar
      prevC0f = (short) c0f;
    }
    if (v) {
      c0.resize(w, c0f, 0, 0);
      c1.resize(w, left-c0f, 0, c0f+pad);
    } else {
      c0.resize(c0f, h, 0, 0);
      c1.resize(left-c0f, h, c0f+pad, 0);
    }
  }
}
