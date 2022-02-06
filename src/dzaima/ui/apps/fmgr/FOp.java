package dzaima.ui.apps.fmgr;

import dzaima.utils.Vec;

import java.io.*;
import java.nio.file.*;

public class FOp {
  static final boolean LINUX;
  static {
    LINUX = System.getProperty("os.name").equals("Linux");
  }
  
  public static Vec<Path> list(Path p) throws IOException, InterruptedException {
    linux: if (LINUX) {
      try {
        Process ls = new ProcessBuilder("ls", "-fbA").directory(p.toFile()).start();
        Vec<Path> res = new Vec<>();
        if (ls.waitFor()!=0) {
          System.err.println("ls gave exit code "+ls.exitValue());
          break linux;
        }
        BufferedReader b = new BufferedReader(new InputStreamReader(ls.getInputStream()));
        while (true) {
          String ln = b.readLine();
          if (ln==null) break;
          String d = decodeC(ln);
          if (d==null) break linux;
          res.add(p.resolve(d));
        }
        return res;
      } catch (Exception e) {
        if (e instanceof InterruptedException) throw e;
        System.err.println("Using ls failed:");
        e.printStackTrace();
      }
    }
    DirectoryStream<Path> ch = Files.newDirectoryStream(p);
    Vec<Path> res = new Vec<>();
    for (Path c : ch) res.add(c);
    return res;
  }
  
  public static String decodeC(String s) {
    if (s.indexOf('\\')==-1) return s;
    StringBuilder b = new StringBuilder();
    int l = s.length();
    for (int i = 0; i < l; i++) {
      char c = s.charAt(i);
      if (c=='\\') {
        if (i+1>=l) return null;
        char n = s.charAt(++i);
        int chr = "abtnvfr".indexOf(n);
        if (chr!=-1) b.append((char)(chr+7));
        else if (n==' ') b.append(' ');
        else if (n=='\\') b.append('\\');
        else if (n>='0' && n<='9') {
          if (i+2>=l) return null;
          int num = 0;
          for (int j = 0; j < 3; j++) {
            char k = s.charAt(i + j);
            if (k<'0'|k>'9') return null;
            num = num*8 + k-'0';
          }
          b.append((char)num);
          i+= 2;
        } else if (n=='x') {
          if (i+2>=l) return null;
          int num = hexi(s.charAt(i+1))*16 + hexi(s.charAt(i+2));
          if (num<0) return null;
          b.append((char)num);
          i+= 2;
        } else throw new RuntimeException("Unrecognized \\"+n);
      } else b.append(c);
    }
    return b.toString();
  }
  public static String encodeC(String s) {
    StringBuilder b = new StringBuilder();
    int l = s.length();
    for (int i = 0; i < l; i++) {
      char c = s.charAt(i);
      if (c<' ') {
        if (c==10) b.append("\\n");
        else if (c==9) b.append("\\t");
        else b.append('\\').append('x').append(hex(c/16)).append(hex(c%16));
      } else if (c==127) {
        b.append("\\x7F");
      } else b.append(c);
    }
    return b.toString();
  }
  private static char hex(int i) { return (char) (i<10? i+'0' : i-10+'A'); }
  private static int hexi(char c) { return c>='0'&c<='9'? c-'0' : c>='A'&c<='F'? c-'A'+10 : c>='a'&c<='f'? c-'a'+10 : -100; }
}
