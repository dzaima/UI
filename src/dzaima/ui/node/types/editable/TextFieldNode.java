package dzaima.ui.node.types.editable;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Props;

public class TextFieldNode extends EditNode {
  private int bgCol, padY;
  private float radius;
  
  public TextFieldNode(Ctx ctx, Props props) {
    super(ctx, props, false);
  }
  public void propsUpd() { super.propsUpd();
    drawOffX = gc.len(this, "padX", "textfield.padX");
    drawOffY = padY = gc.len(this, "padY", "textfield.padY");
    radius = gc.lenF(this, "radius", "textfield.radius");
    bgCol = gc.col(this, "bg", "textfield.bg");
  }
  
  public void bg(Graphics g, boolean full) { pbg(g, full);
    g.rrect(0,0, w,h, radius, bgCol);
  }
  
  public int minH(int w) { return 2*padY+f.hi; }
  public int maxH(int w) { return 2*padY+f.hi; }
}
