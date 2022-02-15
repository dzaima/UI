package dzaima.ui.gui.select;

public class Selection {
  public final int depth;
  public final Position a, b;
  public final PosPart aS, bS;
  public final Selectable c;
  public PosPart sS, eS;
  
  public Selection(int depth, Position a, Position b, PosPart aS, PosPart bS) {
    this.depth = depth;
    this.a = a;
    this.b = b;
    this.aS = aS;
    this.bS = bS;
    c = aS.sn;
    assert aS.sn == bS.sn;
  }
  
  public void start() {
    boolean rev = c.selectS(this);
    if (rev) { sS=bS; eS=aS; }
    else     { sS=aS; eS=bS; }
  }
  
  public void end() {
    c.selectE(this);
  }
}
