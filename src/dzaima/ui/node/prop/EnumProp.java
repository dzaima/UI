package dzaima.ui.node.prop;

import java.util.HashMap;

public class EnumProp extends PropI {
  public final String s;
  
  public EnumProp(String s) {
    this.s = s;
  }
  
  public char type() { return 'a'; }
  
  public String     val() { return s; }
  public boolean isNull() { return s.equals("null"); }
  
  
  public String toString() { return s; }
  public boolean equals(Object o) { return o instanceof EnumProp && s.equals(((EnumProp) o).s); }
  
  public static EnumProp bool(boolean b) {
    return b? TRUE : FALSE;
  }
  private static final HashMap<String, EnumProp> cache = new HashMap<>();
  public static EnumProp cache(String name) {
    return cache.computeIfAbsent(name, EnumProp::new);
  }
  public static EnumProp TRUE = cache("true");
  public static EnumProp FALSE = cache("false");
}
