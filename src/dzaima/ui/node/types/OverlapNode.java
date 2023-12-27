package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Props;
import dzaima.utils.XY;

import java.util.function.*;

public class OverlapNode extends WrapNode {
  public OverlapNode(Ctx ctx, Props props) {
    super(ctx, props);
  }
  
  public BiConsumer<Node, Graphics> draw;
  public Consumer<Node> resized;
  
  public void drawCh(Graphics g, boolean full) {
    ch.get(0).draw(g, full);
    if (draw!=null) draw.accept(this, g);
  }
  
  public void over(Graphics g) {
    if (ch.sz==2) ch.get(1).draw(g, true);
  }
  
  public Node findCh(int x, int y) { // go in reverse order
    for (int i = ch.sz-1; i >= 0; i--) {
      Node c = ch.get(i);
      if (XY.inWH(x, y, c.dx, c.dy, c.w, c.h)) return c;
    }
    return null;
  }
  
  public void resized() {
    super.resized();
    if (resized!=null) resized.accept(this);
  }
}
