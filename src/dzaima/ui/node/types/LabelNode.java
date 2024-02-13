package dzaima.ui.node.types;

import dzaima.ui.gui.io.Click;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Props;

public class LabelNode extends WrapNode {
  public LabelNode(Ctx ctx, Props props) {
    super(ctx, props);
  }
  public Node getFor() {
    return ctx.id(getProp("for").val());
  }
  
  public void mouseStart(int x, int y, Click c) {
    super.mouseStart(x, y, c);
    if (c.bL()) c.register(this, x, y);
  }
  
  public void mouseTick(int x, int y, Click c) { c.onClickEnd(); }
  
  public void mouseUp(int x, int y, Click c) {
    if (c.bL()) {
      Node n = getFor();
      if (n instanceof Labeled) ((Labeled) n).labelClick();
    }
  }
  
  public void hoverS() { Node n = getFor(); if (n instanceof Labeled) ((Labeled) n).labelHoverS(); }
  public void hoverE() { Node n = getFor(); if (n instanceof Labeled) ((Labeled) n).labelHoverE(); }
  
  public interface Labeled {
    void labelClick();
    void labelHoverS();
    void labelHoverE();
  }
}
