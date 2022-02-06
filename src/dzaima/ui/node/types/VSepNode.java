package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;

public class VSepNode extends Node {
  private int color;
  private int px, py, ih;
  public VSepNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  public void propsUpd() { super.propsUpd();
    color = gc.col(this, "color", "vsep.color");
    px = gc.len(this, "x", "vsep.x");
    py = gc.len(this, "y", "vsep.y");
    ih = gc.len(this, "h", "vsep.h");
  }
  
  public int minW() { return px*2+1; }
  public int minH(int w) { return ih+2*py; }
  public int maxH(int w) { return ih+2*py; }
  
  public void drawC(Graphics g) {
    g.rect(px, py, w-px, py+ih, color);
  }
}
