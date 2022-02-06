package dzaima.utils;

import java.util.*;

public class JSON {
  public static final Null NULL = new Null();
  public static final Bool FALSE = new Bool(false);
  public static final Bool TRUE = new Bool(true);
  
  public static String quote(String s) {
    StringBuilder b = new StringBuilder();
    quote(b, s);
    return b.toString();
  }
  public static void quote(StringBuilder b, String s) {
    b.append("\"");
    escape(b, s);
    b.append("\"");
  }
  
  public static Obj parseObj(String s) { return parse(s).obj(); }
  public static Arr parseArr(String s) { return parse(s).arr(); }
  public static Val parse(String s) {
    JSON p = new JSON(s);
    p.skip();
    Val r = p.get();
    p.skip();
    if (p.i != s.length()) throw new JSONException("Input contained text after end");
    return r;
  }
  
  
  public static void escape(StringBuilder b, String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"':  b.append("\\\""); break;
        case '\\': b.append("\\\\"); break;
        case '\b': b.append("\\b"); break;
        case '\f': b.append("\\f"); break;
        case '\n': b.append("\\n"); break;
        case '\r': b.append("\\r"); break;
        case '\t': b.append("\\t"); break;
        default:
          if (Character.isSurrogate(c) || Character.isISOControl(c)) {
            b.append("\\u");
            for (int j = 0; j < 4; j++) {
              int v = (c>>(12-j*4)) & 15;
              b.append((char)(v>=10? v-10+'A' : v+'0'));
            }
          } else {
            b.append(c);
          }
          break;
      }
    }
  }
  
  public static String format(Val v) {
    StringBuilder r = new StringBuilder();
    v.format(r);
    return r.toString();
  }
  
  public static abstract class Val {
    public double num() { throw new RuntimeException("This isn't a number!"); }
    public String str() { throw new RuntimeException("This isn't a string!"); }
    public Arr arr() { throw new RuntimeException("This isn't an array!"); }
    public Obj obj() { throw new RuntimeException("This isn't an object!"); }
    public boolean bool() { throw new RuntimeException("This isn't a boolean!"); }
    
    public double num(double def) { throw new RuntimeException("This isn't a number!"); }
    public String str(String def) { throw new RuntimeException("This isn't a string!"); }
    public Arr arr(Arr def) { throw new RuntimeException("This isn't an array!"); }
    public Obj obj(Obj def) { throw new RuntimeException("This isn't an object!"); }
    public boolean bool(boolean def) { throw new RuntimeException("This isn't a boolean!"); }
    
    public boolean isNum() { return this instanceof Num; }
    public boolean isStr() { return this instanceof Str; }
    public boolean isArr() { return this instanceof Arr; }
    public boolean isObj() { return this instanceof Obj; }
    public boolean isBool(){ return this instanceof Bool; }
    public boolean isNull() { return false; }
    
    public int asInt() { return (int)num(); }
    public int asInt(int def) { return (int)num(def); }
    
    public long asLong() { return (long)num(); }
    public long asLong(long def) { return this instanceof Num? (long)((Num)this).val : def; }
    
    abstract void format(StringBuilder b);
    public String toString() { return JSON.format(this); }
    public String toString(int indent) { return toString(); } // TODO formatting
  }
  
  public static class Num extends Val {
    public final double val;
    public Num(double val) { this.val = val; }
    
    public double num() { return val; }
    public double num(double def) { return val; }
    void format(StringBuilder b) { b.append(val); }
  }
  
  public static class Str extends Val {
    public final String val;
    public Str(String val) { this.val = val; }
    
    public String str() { return val; }
    public String str(String def) { return val; }
    void format(StringBuilder b) { quote(b, val); }
  }
  
  public static class Obj extends Val {
    public static final Obj E = new Obj(new String[0], new Val[0]);
    private String[] ks;
    private Val[] vs;
    private HashMap<String, Val> map;
    
    public Obj(String[] ks, Val[] vs) {
      this.ks = ks;
      this.vs = vs;
    }
    public Obj(HashMap<String, Val> map) {
      this.map = map;
    }
  
  
    public Obj obj() { return this; }
    public Obj obj(Obj def) { return this; }
    
    public void toMap() {
      if (map!=null) return;
      map = new HashMap<>();
      for (int i = 0; i < ks.length; i++) if (map.put(ks[i], vs[i]) != null) throw new RuntimeException("Duplicate entry for "+quote(ks[i]));
    }
    
    public Val get(String k, Val def) {
      if (ks!=null && ks.length<10) {
        for (int i = 0; i < ks.length; i++) if (k.equals(ks[i])) return vs[i];
        return def;
      }
      toMap();
      return map.getOrDefault(k, def);
    }
    public Val get(String k) {
      Val r = get(k, NOTFOUND);
      if (r == NOTFOUND) throw new RuntimeException("Key "+quote(k)+" not found");
      return r;
    }
  
    public double num(String k            ) { return get(k).num(); }
    public int getInt(String k            ) { return get(k).asInt(); }
    public String str(String k            ) { return get(k).str(); }
    public Arr    arr(String k            ) { return get(k).arr(); }
    public Obj    obj(String k            ) { return get(k).obj(); }
    
    public double num(String k, double def) { Val r = get(k, NOTFOUND); return r==NOTFOUND? def : r.num(def); }
    public int getInt(String k, int    def) { Val r = get(k, NOTFOUND); return r==NOTFOUND? def : r.asInt(def); }
    public String str(String k, String def) { Val r = get(k, NOTFOUND); return r==NOTFOUND? def : r.str(def); }
    public Arr    arr(String k, Arr    def) { return get(k, def).arr(); }
    public Obj    obj(String k, Obj    def) { return get(k, def).obj(); }
    
    public boolean bool(String k) { return get(k).bool(); }
    public boolean bool(String k, boolean def) { Val r = get(k, NOTFOUND); return r==NOTFOUND? def : r.bool(def); }
    
    public boolean has(String k) { return get(k, NOTFOUND)!=NOTFOUND; }
    public boolean hasBool(String k) { return get(k, null) instanceof Bool; }
    public boolean hasNum (String k) { return get(k, null) instanceof Num; }
    public boolean hasStr (String k) { return get(k, null) instanceof Str; }
    public boolean hasArr (String k) { return get(k, null) instanceof Arr; }
    public boolean hasObj (String k) { return get(k, null) instanceof Obj; }
    
    public Val put(String k, Val v) { // calling will discard ordering
      toMap(); ks=null; vs=null;
      return map.put(k, v);
    }
    
    public static Obj fromKV(Object... objs) {
      if (objs.length%2 != 0) throw new IllegalArgumentException();
      String[] ks = new String[objs.length/2];
      Val[] vs = new Val[objs.length/2];
      for (int i = 0; i < objs.length/2; i++) {
        ks[i] = (String) objs[2*i];
        vs[i] = (Val)objs[2*i + 1];
      }
      return new Obj(ks, vs);
    }
    
    public static Val path(Val c, Val def, String... path) {
      for (String k : path) {
        Val n = ((Obj)c).get(k, NOTFOUND);
        if (n == NOTFOUND) return def;
        c = n;
      }
      return c;
    }
    public static Obj objPath(Val c, Obj def, String... path) { return path(c, def, path).obj(def); }
    public static Arr arrPath(Val c, Arr def, String... path) { return path(c, def, path).arr(def); }
    
    void format(StringBuilder b) {
      b.append('{');
      if (ks!=null) {
        for (int i = 0; i < ks.length; i++) {
          if (i != 0) b.append(',');
          quote(b, ks[i]);
          b.append(':');
          vs[i].format(b);
        }
      } else {
        boolean first = true;
        for (Map.Entry<String, Val> e : map.entrySet()) {
          if (first) first = false;
          else b.append(",");
          quote(b, e.getKey());
          b.append(':');
          e.getValue().format(b);
        }
      }
      b.append('}');
    }
    
    // must be immediately used in a foreach and the entry object must not be stored, as the iterable, iterator, and entry are all the same object
    public Iterable<Entry> entries() {
      if (map!=null) {
        final Iterator<Map.Entry<String,Val>> es = map.entrySet().iterator();
        return new Entry() {
          public boolean hasNext() { return es.hasNext(); }
          public Entry next() {
            Map.Entry<String,Val> e = es.next();
            k = e.getKey();
            v = e.getValue();
            return this;
          }
        };
      }
      return new Entry() {
        int i = 0;
        public boolean hasNext() { return i<ks.length; }
        public Entry next() {
          k = ks[i];
          v = vs[i];
          i++;
          return this;
        }
      };
    }
    
  }
  
  public static abstract class Entry implements Iterable<Entry>,Iterator<Entry> {
    public String k;
    public Val v;
    public Iterator<Entry> iterator() { return this; }
  }
  
  public static class Arr extends Val implements Iterable<Val> {
    public static final Arr E = new Arr(new Val[0]);
    public final Val[] items;
    public Arr(Val[] items) { this.items = items; }
    
    public Arr arr() { return this; }
    public Arr arr(Arr def) { return this; }
    
    public int size() { return items.length; }
    public Val get(int i) { return items[i]; }
  
    public double   num(int i) { return items[i].num(); }
    public int   getInt(int i) { return items[i].asInt(); }
    public boolean bool(int i) { return items[i].bool(); }
    public Arr      arr(int i) { return items[i].arr(); }
    public Obj      obj(int i) { return items[i].obj(); }
    public String   str(int i) { return items[i].str(); }
    
    void format(StringBuilder b) {
      b.append('[');
      for (int i = 0; i < items.length; i++) {
        if (i!=0) b.append(',');
        items[i].format(b);
      }
      b.append(']');
    }
  
    public Iterator<Val> iterator() {
      return new Iterator<Val>() {
        int i = 0;
        public boolean hasNext() { return i<items.length; }
        public Val next() { return items[i++]; }
      };
    }
  
    // these must be used immediately in a foreach
    public Iterable<Double> nums() { return new ArrNumIter(); }
    public Iterable<String> strs() { return new ArrStrIter(); }
    public Iterable<Obj> objs() { return new ArrObjIter(); }
    public Iterable<Arr> arrs() { return new ArrArrIter(); }
    
    class ArrNumIter implements Iterable<Double>,Iterator<Double> {
      int i = 0;
      public Iterator<Double> iterator() { return this; }
      public boolean hasNext() { return i<items.length; }
      public Double next() { return items[i++].num(); }
    }
    class ArrStrIter implements Iterable<String>,Iterator<String> {
      int i = 0;
      public Iterator<String> iterator() { return this; }
      public boolean hasNext() { return i<items.length; }
      public String next() { return items[i++].str(); }
    }
    class ArrObjIter implements Iterable<Obj>,Iterator<Obj> {
      int i = 0;
      public Iterator<Obj> iterator() { return this; }
      public boolean hasNext() { return i<items.length; }
      public Obj next() { return items[i++].obj(); }
    }
    class ArrArrIter implements Iterable<Arr>,Iterator<Arr> {
      int i = 0;
      public Iterator<Arr> iterator() { return this; }
      public boolean hasNext() { return i<items.length; }
      public Arr next() { return items[i++].arr(); }
    }
  }
  
  private static final Null NOTFOUND = new Null();
  public static class Null extends Val {
    private Null() { }
    public double num(double def) { return def; }
    public String str(String def) { return def; }
    public Arr    arr(Arr    def) { return def; }
    public Obj    obj(Obj    def) { return def; }
    public boolean bool(boolean def) { return def; }
    public boolean isNull() { return true; }
  
    void format(StringBuilder b) { b.append("null"); }
  }
  
  public static class Bool extends Val {
    boolean val;
    private Bool(boolean val) { this.val = val; }
    
    public boolean bool() { return val; }
    public boolean bool(boolean def) { return val; }
    
    void format(StringBuilder b) { b.append(val); }
    
    public static Bool of(boolean val) { return val? TRUE : FALSE; }
  }
  
  
  public static class JSONException extends RuntimeException {
    public JSONException(String m) { super(m); }
  }
  
  
  
  
  
  
  
  
  
  
  int i = 0;
  final String s;
  JSON(String s) {
    this.s = s;
  }
  void skip() {
    char c;
    while (i<s.length() && ((c=s.charAt(i))==' ' || c=='\t' || c=='\n' || c=='\r')) i++;
  }
  char peek() { if (i>=s.length()) throw new JSONException("Input ended early"); return s.charAt(i); }
  char next() { if (i>=s.length()) throw new JSONException("Input ended early"); return s.charAt(i++); }
  Val get() {
    if (i>=s.length()) throw new JSONException("Input ended early");
    switch (s.charAt(i)) {
      case '{': {
        i++;
        skip();
        if (peek()=='}') { i++; return Obj.E; }
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<Val> vals = new ArrayList<>();
        while (true) {
          Val k = get(); skip();
          if (!(k instanceof Str)) throw new JSONException("Invalid key at "+i);
          if (next()!=':') throw new JSONException("Expected ':' at "+i);
          skip();
          Val v = get(); skip();
          keys.add(k.str());
          vals.add(v);
          char n = next(); skip();
          if (n=='}') break;
          else if (n!=',') throw new JSONException("Expected ',' at "+i);
        }
        return new Obj(keys.toArray(new String[0]), vals.toArray(new Val[0]));
      }
      case '[': {
        i++;
        skip();
        if (peek()==']') { i++; return Arr.E; }
        ArrayList<Val> vals = new ArrayList<>();
        while (true) {
          skip();
          vals.add(get());
          skip();
          char n = next();
          if (n==']') break;
          else if (n!=',') throw new JSONException("Expected ',' at "+i);
        }
        return new Arr(vals.toArray(new Val[0]));
      }
      case '"':
        i++;
        StringBuilder r = new StringBuilder();
        while (true) {
          char c = next();
          if (c=='"') break;
          if (c=='\\') {
            char n = next();
            if (n=='"') r.append("\"");
            else if (n=='\\')r.append("\\");
            else if (n=='/') r.append("/");
            else if (n=='b') r.append("\b");
            else if (n=='f') r.append("\f");
            else if (n=='n') r.append("\n");
            else if (n=='r') r.append("\r");
            else if (n=='t') r.append("\t");
            else if (n=='u') {
              if (i+4 > s.length()) throw new JSONException("Unfinished string");
              int v = 0;
              for (int j = 0; j < 4; j++) {
                char d = s.charAt(i++);
                v<<= 4;
                if (d>='0' & d<='9') v|= d-'0';
                else if (d>='a' & d<='f') v|= d-'a' + 10;
                else if (d>='A' & d<='F') v|= d-'A' + 10;
                else throw new JSONException("Bad \\u value at "+i);
              }
              r.append((char)v);
            } else throw new JSONException("Unknown escape \\"+n);
          } else r.append(c);
        }
        return new Str(r.toString());
      case 'f': if (!s.startsWith("false", i)) throw new JSONException("Invalid token at "+i); i+= 5; return FALSE;
      case 't': if (!s.startsWith("true",  i)) throw new JSONException("Invalid token at "+i); i+= 4; return TRUE;
      case 'n': if (!s.startsWith("null",  i)) throw new JSONException("Invalid token at "+i); i+= 4; return NULL;
      case'0': case'1': case'2': case'3': case'4': case'5': case'6': case'7': case'8': case'9': case'.': case'-':
        int si = i;
        while(i<s.length()) {
          char c = s.charAt(i);
          if (!(c>='0'&c<='9' | c=='.' | c=='e' | c=='E' | c=='+' | c=='-')) break;
          i++;
        }
        return new Num(Double.parseDouble(s.substring(si, i)));
      default:
        throw new JSONException("Unknown character "+s.charAt(i)+" at "+i);
    }
  }
}
