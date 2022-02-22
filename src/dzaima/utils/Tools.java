package dzaima.utils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.security.*;
import java.time.*;
import java.util.Arrays;

@SuppressWarnings({"AssertWithSideEffects", "ConstantConditions"})
public class Tools {
  public static final int BIG = Integer.MAX_VALUE/2;
  public static Path RES_PATH = Paths.get("res/");
  
  // assertion stuff
  public static final boolean DBG;
  static {
    boolean a = false;
    assert a=true; // ha
    DBG = a;
  }
  
  private static MessageDigest d;
  public static String sha256(byte[] data) {
    if (d==null) try {
      d = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    byte[] bs = d.digest(data);
    char[] cs = new char[64];
    for (int i = 0; i < cs.length; i++) {
      int c = (bs[i/2] >>> (4 - i%2*4)) & 0xf;
      cs[i] = (char)(c<10? '0'+c : 'a'+c-10);
    }
    return new String(cs);
  }
  
  
  public static Path cachePath = Paths.get("cache");
  public static int cacheDays = 5;
  public static void purgeOldCache() {
    if (!Files.isDirectory(cachePath)) return;
    try {
      for (Path p : Files.newDirectoryStream(cachePath)) {
        Duration d = Duration.between(Files.getLastModifiedTime(p).toInstant(), Instant.now());
        if (d.toDays() > cacheDays) Files.deleteIfExists(p);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public static byte[] get(String path, boolean cache) {
    Path p;
    if (cache) {
      p = cachePath.resolve(sha256(path.getBytes(StandardCharsets.UTF_8)));
      if (Files.exists(p)) {
        try {
          Files.setLastModifiedTime(p, FileTime.from(Instant.now()));
          purgeOldCache();
          return Files.readAllBytes(p);
        } catch (IOException e) { System.out.println("Failed reading cache:"); e.printStackTrace(); }
      }
      purgeOldCache();
    } else p=null;
    try {
      URL u = new URL(path);
      HttpURLConnection c = (HttpURLConnection) u.openConnection();
      c.setRequestMethod("GET");
      c.setUseCaches(cache);
      
      byte[] b = new byte[1024];
      int i = 0, am;
      try (InputStream is = c.getResponseCode()>=400? c.getErrorStream() : c.getInputStream()) {
        while ((am = is.read(b, i, b.length-i))!=-1) {
          i+= am;
          if (i==b.length) b = Arrays.copyOf(b, b.length*2);
        }
      }
      byte[] r = Arrays.copyOf(b, i);
      if (cache) {
        Files.createDirectories(p.getParent());
        Files.write(p, r);
      }
      return r;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  
  public static int err() {
    throw new RuntimeException();
  }
  
  public static String fmtColor(int c) {
    String s = Integer.toUnsignedString(c, 16);
    if (s.length()<8) s = repeat('0', 8-s.length()) + s;
    else if (s.startsWith("ff")) s = s.substring(2);
    return "#"+s;
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
  
  public static boolean vs(int col) { return col>>>24 != 0;   } // alpha≠0; is visible
  public static boolean st(int col) { return col>>>24 != 255; } // is see-through
  
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
    return Tools.readFile(RES_PATH.resolve(s));
  }
  
  public static void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      throw new QInterruptedException(e); // eh whatever
    }
  }
  
  
  
  static class QInterruptedException extends RuntimeException {
    public QInterruptedException(InterruptedException e) { super(e); }
  }
  public interface RunnableThread {
    void run() throws InterruptedException;
  }
  public static Thread thread(RunnableThread r) {
    Thread t = new Thread(() -> {
      try {
        r.run();
      } catch (QInterruptedException | InterruptedException ignored) { }
    });
    t.start();
    return t;
  }
  
  public static int ceil(double d) { return (int) Math.ceil(d); }
  public static int ceil( float f) { return (int) Math.ceil(f); }
  
  public static int   limit(int   v, int   min, int   max) { return Math.max(min, Math.min(v, max)); }
  public static float limit(float v, float min, float max) { return Math.max(min, Math.min(v, max)); }
  
  @Deprecated
  public static int ceil(int i) { throw new RuntimeException(); } // make sure noone does this
}