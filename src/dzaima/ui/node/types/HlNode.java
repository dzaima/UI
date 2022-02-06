package dzaima.ui.node.types;

import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.Tools;

public class HlNode extends HNode {
  public HlNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  int defMax(boolean w) { return w? 0 : Tools.BIG; }
}
