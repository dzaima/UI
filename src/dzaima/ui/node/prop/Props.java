package dzaima.ui.node.prop;

import dzaima.utils.*;

import java.util.Arrays;

public abstract class Props {
  public static Props ofKV(String[] ks, Prop[] vs) {
    if (ks.length==0) return Props0.EMPTY;
    if (ks.length==1) return of(ks[0], vs[0]);
    return new PropsKV(ks, vs);
  }
  public static Props none() {
    return Props0.EMPTY;
  }
  public static Props of(String k, Prop v) {
    return new Props1(k, v);
  }
  
  public static Gen keys(String... ks) {
    return new Gen(ks);
  }
  
  
  public abstract Prop get(String name);
  public abstract Prop getNullable(String name);
  public abstract boolean has(String name);
  public abstract Props with(String name, Prop val);
  public abstract Vec<Prop> values();
  public abstract Vec<Pair<String, Prop>> entries();
  
  
  
  public static class Gen {
    private final String[] ks;
    public Gen(String[] ks) { this.ks = ks; }
    public Props values(Prop... vs) {
      assert ks.length == vs.length;
      return ofKV(ks, vs);
    }
  }
  
  
  static class Props0 extends Props {
    public static final Props EMPTY = new Props0();
    
    public Prop get(String name) { throw new RuntimeException("Getting "+name+" from empty props list"); }
    public Prop getNullable(String name) { return null; }
    public boolean has(String name) { return false; }
    public Props with(String name, Prop val) { throw new RuntimeException("Mutating empty props list"); }
    public Vec<Prop> values() { return Vec.of(); }
    public Vec<Pair<String, Prop>> entries() { return Vec.of(); }
  }
  
  
  
  static class Props1 extends Props {
    public final String k;
    public final Prop v;
    Props1(String k, Prop v) {
      this.k = k;
      this.v = v;
    }
    public Prop get(String name) {
      assert k.equals(name) : badGet(name);
      return v;
    }
    public Props with(String name, Prop v2) {
      assert k.equals(name) : badWith(name);
      return toMutable(new String[]{k}, new Prop[]{v2});
    }
    public Prop getNullable(String name) { return name.equals(k)? v : null; }
    public boolean has(String name) { return name.equals(k); }
    public Vec<Prop> values() { return Vec.of(v); }
    public Vec<Pair<String, Prop>> entries() { return Vec.of(new Pair<>(k, v)); }
  }
  
  
  
  static class PropsKV extends Props {
    private final String[] ks;
    private final Prop[] vs;
    PropsKV(String[] ks, Prop[] vs) {
      this.ks = ks;
      this.vs = vs;
    }
    int idx(String name) {
      for (int i = 0; i < ks.length; i++) if (ks[i].equals(name)) return i;
      return -1;
    }
    public Prop get(String name) {
      int i = idx(name);
      assert i != -1 : badGet(name);
      return vs[i];
    }
    
    public Prop getNullable(String name) {
      int i = idx(name);
      return i==-1? null : vs[i];
    }
    
    public boolean has(String name) {
      return idx(name) != -1;
    }
    
    public Props with(String name, Prop val) {
      int i = idx(name);
      assert i!=-1 : badWith(name);
      Prop[] nvs = Arrays.copyOf(vs, vs.length);
      nvs[i] = val;
      return toMutable(ks, nvs);
    }
    
    public Vec<Prop> values() {
      return Vec.of(vs);
    }
    
    public Vec<Pair<String, Prop>> entries() {
      Vec<Pair<String, Prop>> r = new Vec<>(ks.length);
      for (int i = 0; i < ks.length; i++) r.add(new Pair<>(ks[i], vs[i]));
      return r;
    }
  }
  
  protected Props toMutable(String[] ks, Prop[] vs) {
    return new PropsKV(ks, vs);
  }
  
  protected String badGet(String name) {
    return "Property "+name+" not found";
  }
  protected String badWith(String name) {
    return "Property "+name+" to replace not found";
  }
}
