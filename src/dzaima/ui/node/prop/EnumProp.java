package dzaima.ui.node.prop;

public class EnumProp extends PropI {
  public final String s;
  
  public EnumProp(String s) {
    this.s = s;
  }
  
  public char type() { return 'a'; }
  
  public String     val() { return s; }
  public boolean isNull() { return s.equals("null"); }
  
  
  public String toString() { return s; }
  
  public static EnumProp bool(boolean b) {
    return b? TRUE : FALSE;
  }
  public static EnumProp TRUE = new EnumProp("true");
  public static EnumProp FALSE = new EnumProp("false");
}
