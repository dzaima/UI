package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;

public class HideNode extends Node {
  private boolean hidden;
  public HideNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  public void propsUpd() { mRedraw();
    hidden = vs[id("hide")].b();
  }
  
  public int minW(     ) { return ch.get(0).minW( ); }
  public int maxW(     ) { return ch.get(0).maxW( ); }
  public int minH(int w) { return ch.get(0).minH(w); }
  public int maxH(int w) { return ch.get(0).maxH(w); }
  
  public void drawCh(Graphics g, boolean full) {
    if (hidden) return;
    super.drawCh(g, full);
  }
  
  public void resized() {
    assert ch.sz==1;
    ch.get(0).resize(w, h, 0, 0);
  }
}
