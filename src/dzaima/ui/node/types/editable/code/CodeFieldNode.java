package dzaima.ui.node.types.editable.code;

import dzaima.ui.gui.Graphics;
import dzaima.ui.gui.undo.UndoManager;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Props;

public class CodeFieldNode extends CodeAreaNode {
  private int bgCol, padY;
  private float radius;
  
  public CodeFieldNode(Ctx ctx, Props props) {
    this(ctx, props, new UndoManager(ctx.gc));
  }
  public CodeFieldNode(Ctx ctx, Props props, UndoManager um) {
    super(ctx, props, false, um);
  }
  
  public void propsUpd() { super.propsUpd();
    drawOffX2 = gc.len(this, "padX", "textfield.padX");
    drawOffY = padY = gc.len(this, "padY", "textfield.padY");
    radius = gc.lenF(this, "radius", "textfield.radius");
    bgCol = gc.col(this, "bg", "textfield.bg");
  }
  
  public void bg(Graphics g, boolean full) { pbg(g, full);
    g.rrect(0,0, w,h, radius, bgCol);
  }
  
  public int minH(int w) { return 2*padY+(wrap? super.minH(w) : f.hi); }
  public int maxH(int w) { return 2*padY+(wrap? super.maxH(w) : f.hi); }
}
