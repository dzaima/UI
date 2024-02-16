package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;

import java.util.function.Consumer;

public class RadioNode extends Node implements LabelNode.Labeled {
  public RadioNode(Ctx ctx, Props props) {
    super(ctx, props);
  }
  
  public boolean enabled, initialized;
  public Consumer<RadioNode> fn;
  
  public void setFn(Consumer<RadioNode> fn) { this.fn = fn; }
  
  short sz;
  private RadioNode base, currSelected;
  public void propsUpd() { mResize();
    sz = (short) prop("size").len();
    
    if (initialized) return;
    initialized = true;
    
    Prop e = getPropN("enabled");
    if (e!=null) enabled = e.b();
    if (enabled) base().currSelected = this;
  }
  
  public void setBase(RadioNode base) {
    this.base = base;
  }
  
  RadioNode base() {
    if (base==null) {
      Prop f = getPropN("for");
      base = f==null? this : (RadioNode) ctx.id(f.val());
    }
    return base;
  }
  
  
  public boolean quietSet() { // doesn't run any callbacks, returns if changed
    if (enabled) return false;
    RadioNode base = base();
    if (base.currSelected!=null) base.currSelected.quietSelfUnset();
    enabled = true;
    base.currSelected = this;
    changed();
    return true;
  }
  public void set() {
    if (!quietSet()) return;
    RadioNode base = base();
    if (base.fn!=null) base.fn.accept(this);
  }
  
  public void setTo(String id) { ((RadioNode) ctx.id(id)).set(); }
  public boolean quietSetTo(String id) { return ((RadioNode) ctx.id(id)).quietSet(); }
  
  public void quietSelfUnset() { // doesn't run any callbacks
    if (!enabled) return;
    enabled = false;
    RadioNode base = base();
    if (base.currSelected==this) base.currSelected = null;
    changed();
  }
  
  public void changed() {
    mRedraw();
  }
  
  public int maxW() { return sz; }
  public int minW() { return sz; }
  public int minH(int w) { return sz; }
  public int maxH(int w) { return sz; }
  
  public void mouseStart(int x, int y, Click c) { c.register(this, x, y); }
  public void mouseTick(int x, int y, Click c) { c.onClickEnd(); }
  public void mouseUp(int x, int y, Click c) { if (visible) set(); }
  public void labelClick() { set(); }
  
  byte hovered;
  public void hoverS() { hovered++; mRedraw(); }
  public void hoverE() { hovered--; mRedraw(); }
  public void labelHoverS() { hoverS(); }
  public void labelHoverE() { hoverE(); }
  
  public void drawC(Graphics g) {
    int bw = prop("borderW").len();
    float h = (float) sz/2;
    if (bw>0) g.circle(h, h, h, prop(enabled? "borderColOn" : "borderColOff").col());
    g.circle(h, h, h-bw, prop(enabled? "colOn" : hovered!=0? "colHover" : "colOff").col());
    if (enabled) g.circle(h, h, prop("dotSize").lenF(), 0xffd2d2d2);
  }
  Prop prop(String name) {
    Prop val = getPropN(name);
    if (val!=null) return val;
    return gc.getProp("radio."+name);
  }
}
