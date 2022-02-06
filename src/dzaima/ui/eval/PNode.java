package dzaima.ui.eval;

public abstract class PNode {
  public abstract String toString(String pad);
  
  public String toString() {
    return toString("");
  }
}
