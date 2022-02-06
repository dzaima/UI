package dzaima.ui.node.types;

import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;

public class WrapNode extends Node {
  public WrapNode(Ctx ctx, Node ch) {
    super(ctx, KS_NONE, VS_NONE);
    add(ch);
  }
  public WrapNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  public int minW() { return ch.get(0).minW(); }
  public int maxW() { return ch.get(0).maxW(); }
  public int minH(int w) { return ch.get(0).minH(w); }
  public int maxH(int w) { return ch.get(0).maxH(w); }
  public void resized() {
    ch.get(0).resize(w, h, 0, 0);
  }
}
