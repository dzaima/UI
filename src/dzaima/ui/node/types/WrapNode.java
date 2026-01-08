package dzaima.ui.node.types;

import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Props;

public class WrapNode extends Node {
  public WrapNode(Ctx ctx, Node ch) {
    super(ctx, Props.none());
    add(ch);
  }
  public WrapNode(Ctx ctx, Props props) {
    super(ctx, props);
  }
  
  public int minW() { return ch.get(0).minW(); }
  public int maxW() { return ch.get(0).maxW(); }
  public int minH(int w) { return ch.get(0).minH(w); }
  public int maxH(int w) { return ch.get(0).maxH(w); }
  
  protected boolean allowMoreChildren() { return false; }
  
  protected void resized() {
    assert ch.sz == 1 || allowMoreChildren();
    ch.get(0).resize(w, h, 0, 0);
  }
}
