package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;

public class HSepNode extends Node {
  private int color;
  private int px, py, iw;
  public HSepNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  public void propsUpd() { super.propsUpd();
    color = gc.col(this, "color", "hsep.color");
    px = gc.len(this, "x", "hsep.x");
    py = gc.len(this, "y", "hsep.y");
    iw = gc.len(this, "w", "hsep.w");
  }
  
  public int minW() { return iw+2*px; }
  public int maxW() { return iw+2*px; }
  public int minH(int w) { return py*2+1; }
  
  public void drawC(Graphics g) {
    g.rect(px, py, px+iw, h-py, color);
  }
}
