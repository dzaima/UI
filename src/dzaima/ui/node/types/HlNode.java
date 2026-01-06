package dzaima.ui.node.types;

import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Props;
import dzaima.utils.Tools;

public class HlNode extends HNode {
  public HlNode(Ctx ctx, Props props) {
    super(ctx, props);
  }
  protected int defMax(boolean w) { return w? 0 : Tools.BIG; }
}
