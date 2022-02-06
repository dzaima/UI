package dzaima.ui.node.prop;

public class ObjProp extends PropI {
  private final Object o;
  
  public ObjProp(Object o) {
    this.o = o;
  }
  
  public char type() { return '*'; }
  @SuppressWarnings("unchecked")
  public <T> T obj() { return (T)o; }
  
  public String toString() {
    return o.toString();
  }
}