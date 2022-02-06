package dzaima.ui.node.types;

import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.Tools;

public class PadCNode extends Node {
  public float u,l,d,r;
  protected int lI,uI,dwI,dhI;
  public PadCNode(Ctx ctx, Node ch, float l, float r, float u, float d) {
    super(ctx, KS_NONE, VS_NONE);
    add(ch);
    this.u = u; this.d = d;
    this.l = l; this.r = r;
  }
  public PadCNode(Ctx ctx, String[] ks, Prop[] vs, float l, float r, float u, float d) {
    super(ctx, ks, vs);
    this.u = u; this.d = d;
    this.l = l; this.r = r;
  }
  public void propsUpd() { super.propsUpd();
    lI=Tools.ceil(l*gc.em); dwI=lI+Tools.ceil(r*gc.em);
    uI=Tools.ceil(u*gc.em); dhI=uI+Tools.ceil(d*gc.em);
  }
  
  public int minW() { return ch.get(0).minW()+dwI; }
  public int maxW() { return ch.get(0).maxW()+dwI; }
  public int minH(int w) { return ch.get(0).minH(w-dwI)+dhI; }
  public int maxH(int w) { return ch.get(0).maxH(w-dwI)+dhI; }
  public void resized() {
    ch.get(0).resize(w-dwI, h-dhI, lI, uI);
  }
}
