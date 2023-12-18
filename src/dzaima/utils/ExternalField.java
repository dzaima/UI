package dzaima.utils;

import java.util.WeakHashMap;
import java.util.function.*;

public class ExternalField<O, F> {
  private static final Vec<ExternalField<?, ?>> ALL_EXT_FLDS = new Vec<>();
  public final String name;
  public ExternalField(String name) { // name is only a visual thing
    ALL_EXT_FLDS.add(this);
    this.name = name;
  }
  private final WeakHashMap<O, F> vals = new WeakHashMap<>();
  
  public void set(O obj, F val) {
    assert obj!=null;
    if (val==null) vals.remove(obj);
    else vals.put(obj, val);
  }
  
  public void clear(O obj) {
    if (obj!=null) vals.remove(obj);
  }
  
  public F get(O obj) {
    assert obj!=null;
    return vals.get(obj);
  }
  
  public F getOrSet(O obj, Supplier<F> fn) {
    return vals.computeIfAbsent(obj, o2 -> fn.get());
  }
  public F getOrInit(O obj, Function<O, F> fn) {
    return vals.computeIfAbsent(obj, fn);
  }
  
  public F getAndClear(O obj) {
    F r = get(obj);
    clear(obj);
    return r;
  }
  
  public boolean has(O obj) {
    return vals.containsKey(obj);
  }
  
  public F getAndClearOrDefault(O obj, Supplier<F> def) {
    F v = getAndClear(obj);
    if (v==null) return def.get();
    return v;
  }
  
  public static void logStats() {
    for (ExternalField<?, ?> c : ALL_EXT_FLDS) {
      System.err.println(c.name+": "+c.vals.size());
    }
  }
}
