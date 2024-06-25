package dzaima.utils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.*;

@SuppressWarnings({"AssertWithSideEffects", "ConstantConditions"})
public class Tools {
  public static final int BIG = Integer.MAX_VALUE/2;
  public static Path RES_DIR;
  static {
    String res = System.getProperty("RES_DIR");
    if (res!=null) RES_DIR = Paths.get(res);
    else RES_DIR = Paths.get("res/");
  }
  
  // assertion stuff
  public static final boolean DBG;
  static {
    boolean a = false;
    assert a=true; // ha
    DBG = a;
  }
  
  public static String hexBytes(byte[] data) {
    char[] cs = new char[data.length*2];
    for (int i = 0; i < cs.length; i++) {
      int c = (data[i/2] >>> (4 - i%2*4)) & 0xf;
      cs[i] = (char)(c<10? '0'+c : 'a'+c-10);
    }
    return new String(cs);
  }
  public static String hexLong(long l) {
    return "0x"+Long.toHexString(l);
  }
  public static String sha256(byte[] data) {
    MessageDigest d;
    try { d = MessageDigest.getInstance("SHA-256"); }
    catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
    byte[] bs = d.digest(data);
    return hexBytes(bs);
  }
  
  
  public static byte[] get(String path) {
    return get(path, false);
  }
  public static byte[] get(String path, boolean useCaches) {
    try {
      URL u = new URL(path);
      HttpURLConnection c = (HttpURLConnection) u.openConnection();
      c.setRequestMethod("GET");
      c.setUseCaches(useCaches);
      
      byte[] b = new byte[1024];
      int i = 0, am;
      try (InputStream is = c.getResponseCode()>=400? c.getErrorStream() : c.getInputStream()) {
        while ((am = is.read(b, i, b.length-i))!=-1) {
          i+= am;
          if (i==b.length) b = Arrays.copyOf(b, b.length*2);
        }
      }
      return Arrays.copyOf(b, i);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  public static Integer firstMatch(int s, int e, Predicate<Integer> test) { // can return both s and e
    s--;
    while (s+1 < e) {
      int m = (s+e)/2;
      if (test.test(m)) e = m;
      else s = m;
    }
    return e;
  }
  
  public static <E, K> Vec<Pair<K, Vec<E>>> group(Vec<E> values, Function<E, K> by) {
    LinkedHashMap<K, Vec<E>> t = new LinkedHashMap<>();
    for (E v : values) {
      K k = by.apply(v);
      t.computeIfAbsent(k, k1 -> new Vec<>()).add(v);
    }
    return Vec.ofCollection(t.entrySet()).map(c -> new Pair<>(c.getKey(), c.getValue()));
  }
  
  
  public static int err() {
    throw new RuntimeException();
  }
  
  public static String repeat(char c, int i) {
    char[] cs = new char[i];
    Arrays.fill(cs, c);
    return new String(cs);
  }
  
  public static boolean isWs(int c) { return c==' ' | c=='\t'; }
  
  public static float map(float f, float s, float e, float ns, float ne) {
    return (f-s)/(e-s)*(ne-ns)+ns;
  }
  
  public static byte   constrain(byte   i, byte   s, byte   e) { assert s<=e; return (byte)  Math.min(Math.max(i, s), e); }
  public static short  constrain(short  i, short  s, short  e) { assert s<=e; return (short) Math.min(Math.max(i, s), e); }
  public static int    constrain(int    i, int    s, int    e) { assert s<=e; return Math.min(Math.max(i, s), e); }
  public static long   constrain(long   i, long   s, long   e) { assert s<=e; return Math.min(Math.max(i, s), e); }
  public static float  constrain(float  f, float  s, float  e) { assert s<=e; return Math.min(Math.max(f, s), e); }
  public static double constrain(double f, double s, double e) { assert s<=e; return Math.min(Math.max(f, s), e); }
  
  public static boolean vs(int col) { return col>>>24 != 0;   } // alpha≠0; is visible
  public static boolean st(int col) { return col>>>24 != 255; } // is see-through aka at least partly transparent
  public static int argb1  (float a, float r, float g, float b) { return csh(a,1,256,24) | csh(r,1,256,16) | csh(g,1,256,8) | csh(b,1,256,0); }
  public static int argb255(float a, float r, float g, float b) { return csh(a,255,1,24) | csh(r,255,1,16) | csh(g,255,1,8) | csh(b,255,1,0); }
  private static int csh(float c, int x, int m, int sh) {
    if (!(c>=0)) return 0; // incl. NaN check
    if (c>=x) return 0xff << sh;
    return (int)(c*m) << sh;
  }
  
  
  public static String[] split(String s, char c) {
    Vec<String> v = new Vec<>(1);
    int li = 0;
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i)==c) {
        v.add(s.substring(li, i));
        li = i+1;
      }
    }
    v.add(s.substring(li)); // java does no-op this if li==0
    return v.toArray(new String[0]);
  }
  
  public static char[] get(String s, int si, int ei) {
    char[] c = new char[ei-si];
    s.getChars(si, ei, c, 0);
    return c;
  }
  
  public static String readFile(Path p) {
    try {
      return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  public static void writeFile(Path p, String s) {
    try {
      Files.write(p, s.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static String readRes(String s) {
    return Tools.readFile(RES_DIR.resolve(s));
  }
  
  public static void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      throw makeQuietInterrupted(e); // eh whatever
    }
  }
  
  
  public static RuntimeException makeQuietInterrupted(InterruptedException t) {
    return new RuntimeException(t);
  }
  public static boolean isAnyInterrupted(Throwable t) {
    return t!=null && (t instanceof InterruptedException || t.getCause() instanceof InterruptedException);
  }
  
  public interface RunnableThread {
    void run() throws InterruptedException;
  }
  public static Thread thread(RunnableThread r) {
    return thread(r, false);
  }
  public static Thread thread(RunnableThread r, boolean daemon) {
    Thread t = new Thread(() -> {
      try {
        r.run();
      } catch (InterruptedException ignored) {
      } catch (RuntimeException e) {
        if (!isAnyInterrupted(e)) throw e;
      }
    });
    t.setDaemon(daemon);
    t.start();
    return t;
  }
  
  public static String prettyNum(Number d) {
    String s = d.toString();
    if (s.endsWith(".0")) return s.substring(0, s.length()-2);
    return s;
  }
  
  public static boolean ulongGT(long a, long b) { return (a+Long.MIN_VALUE) > (b+Long.MIN_VALUE); }
  public static boolean ulongLT(long a, long b) { return (a+Long.MIN_VALUE) < (b+Long.MIN_VALUE); }
  public static boolean ulongGE(long a, long b) { return (a+Long.MIN_VALUE) >= (b+Long.MIN_VALUE); }
  public static boolean ulongLE(long a, long b) { return (a+Long.MIN_VALUE) <= (b+Long.MIN_VALUE); }
  public static long ulongMax(long a, long b) { return ulongGT(a,b)? a : b; }
  public static long ulongMin(long a, long b) { return ulongLT(a,b)? a : b; }
  
  public static int ceil(double d) { return (int) Math.ceil(d); }
  public static int ceil( float f) { return (int) Math.ceil(f); }
  
  public static int   limit(int   v, int   min, int   max) { return Math.max(min, Math.min(v, max)); }
  public static float limit(float v, float min, float max) { return Math.max(min, Math.min(v, max)); }
  
  @Deprecated
  public static int ceil(int i) { throw new RuntimeException(); } // make sure noone does this
}