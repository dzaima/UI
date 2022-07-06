package dzaima.ui.node.prop;

public class NumProp extends PropI {
  private final double d;
  
  public NumProp(double d) {
    this.d = d;
  }
  
  public char type() { return '0'; }
  public double d() { return d; }
  
  public String toString() { return String.valueOf(d); }
  
  public boolean equals(Object o) { return o instanceof NumProp && d==((NumProp) o).d; }
}
