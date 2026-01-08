package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import io.github.humbleui.skija.Path;

import java.util.function.Consumer;

public class CheckboxNode extends Node implements LabelNode.Labeled {
  public CheckboxNode(Ctx ctx, Props props) {
    super(ctx, props);
    Prop e = getPropN("enabled");
    if (e!=null) enabled = e.b();
  }
  
  public boolean enabled;
  public Consumer<Boolean> fn;
  public void setFn(Consumer<Boolean> fn) { this.fn = fn; }
  
  short sz;
  Path p;
  public void propsUpd() { mResize();
    sz = (short) prop("size").len();
    p = null;
  }
  
  public void toggle() {
    set(!enabled);
  }
  public void set(boolean value) {
    if (enabled==value) return;
    quietSet(value);
    if (fn!=null) fn.accept(enabled);
  }
  public void quietSet(boolean value) {
    enabled = value;
    mRedraw();
  }
  
  public int maxW() { return sz; }
  public int minW() { return sz; }
  public int minH(int w) { return sz; }
  public int maxH(int w) { return sz; }
  
  public void drawC(Graphics g) {
    int round = prop("round").len();
    int bw = prop("borderW").len();
    if (bw>0) g.rrect(0, 0, sz, sz, round, prop("borderCol").col());
    g.rrect(bw, bw, sz-bw, sz-bw, round-bw, prop(enabled? "colOn" : hovered!=0? "colHover" : "colOff").col());
    if (enabled) {
      if (p==null) p = Path.makeFromSVGString(prop("path").str());
      g.push();
      g.scaleLocal(sz, sz);
      g.canvas.drawPath(p, g.paintO(prop("pathCol").col()));
      g.pop();
    }
  }
  
  
  Prop prop(String name) {
    Prop val = getPropN(name);
    if (val!=null) return val;
    return gc.getProp("checkbox."+name);
  }
  
  public void mouseStart(int x, int y, Click c) { c.register(this, x, y); }
  public void mouseTick(int x, int y, Click c) { c.onClickEnd(); }
  public void mouseUp(int x, int y, Click c) { if (visible) toggle(); }
  public void labelClick() { toggle(); }
  
  byte hovered;
  public void hoverS() { hovered++; mRedraw(); }
  public void hoverE() { hovered--; mRedraw(); }
  public void labelHoverS() { hoverS(); }
  public void labelHoverE() { hoverE(); }
}
