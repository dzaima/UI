package dzaima.ui.node.types;

import dzaima.ui.gui.io.Click;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;

public class LabelNode extends WrapNode {
  public LabelNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  public Node getFor() {
    return ctx.id(vs[id("for")].val());
  }

  public void mouseStart(int x, int y, Click c) {
    super.mouseStart(x, y, c);
    if (c.bL()) c.register(this, x, y);
  }
  
  public void mouseTick(int x, int y, Click c) { c.onClickEnd(); }
  
  public void mouseUp(int x, int y, Click c) {
    if (c.bL()) {
      Node n = getFor();
      if (n instanceof CheckboxNode) ((CheckboxNode) n).toggle();
    }
  }
}
