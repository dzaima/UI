package dzaima.ui.node.types;

import dzaima.ui.gui.*;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.Tools;

public class WeighedNode extends Node {
  public WeighedNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
    weight = gc.fD(this, "weight", 0.5f);
    enabled = gc.boolD(this, "enabled", true);
  }
  
  protected float weight;
  protected boolean v;
  protected int handleWidth, pad, padColor;
  public void propsUpd() {
    super.propsUpd();
    switch (vs[id("dir")].val()) { default: throw new RuntimeException("Bad uw \"dir\" value "+vs[id("dir")]);
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
  public void mouseDown(int x, int y, Click c) { if (c.bL()) dragging = false; }
  public void mouseTick(int x, int y, Click c) {
    if (c.bL()) {
      if (!enabled) c.unregister();
      if (dragging || !gc.isClick(c)) {
        int cw = (v? h : w) - pad;
        if (!dragging) {
          weight = handlePos()/cw;
          dragging = true;
        }
        weight+= (v? c.dy : c.dx) * (1f/cw);
        if (weight < 0) weight = 0;
        if (weight > 1) weight = 1;
        mResize();
      }
    }
  }
  
  public void mouseUp(int x, int y, Click c) { if (c.bL() && visible && !dragging) c.unregister(); }
  
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
    mResize();
  }
  
  private short cPos;
  public void hoverS() { cPos = ctx.vw().pushCursor(null); }
  public void hoverT(int mx, int my) { ctx.vw().replaceCursor(cPos, withinHandle(mx, my) && enabled? (v? Window.CursorType.NS_RESIZE : Window.CursorType.EW_RESIZE) : null); }
  public void hoverE() { ctx.vw().popCursor(); }
  
  private int left(int tot) { return tot-pad; } // size left for children
  private int c0f(int tot, int left, Node c0, Node c1) { // child 0 fill
    int o = Math.round(left*weight); // optimal divider position
    if (v) {
      return Tools.constrain(o, c0.minH(tot), left - c1.minH(tot));
    } else {
      return Tools.constrain(o, c0.minW(), left - c1.minW());
    }
  }
  
  public int minW() {
    return v? Solve.vMinW(ch) : Solve.hMinW(ch)+pad; 
  }
  public int minH(int w) {
    if (v) return Solve.vMinH(ch, w) + pad;
    Node c0 = ch.get(0);
    Node c1 = ch.get(1);
    int l = left(w);
    int c0f = c0f(w, l, c0, c1);
    return Math.max(c0.minH(c0f), c1.minH(l-c0f));
  }
  
  protected void resized() {
    assert ch.sz==2;
    Node c0 = ch.get(0);
    Node c1 = ch.get(1);
    int tot = v? h : w;
    int left = left(tot);
    int c0f = c0f(tot, left, c0, c1);
    if (v) {
      c0.resize(w, c0f, 0, 0);
      c1.resize(w, left-c0f, 0, c0f+pad);
    } else {
      c0.resize(c0f, h, 0, 0);
      c1.resize(left-c0f, h, c0f+pad, 0);
    }
    if (pad!=0) mRedraw(); // TODO this is very bad
  }
}
