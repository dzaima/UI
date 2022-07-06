package dzaima.ui.eval;

import dzaima.utils.ColorUtils;

public class Token {
  public final char type; // v-var/name; s-string; n-number; t-typed; #-color; \0 - EOF
  public final int off;
  
  public Token(int off, char type) {
    this.type = type;
    this.off = off;
  }
  
  public String toString() {
    return getClass().getSimpleName()+"('"+type+"')";
  }
  public String expl() {
    return "'"+type+"'";
  }
  
  
  
  public static class ColorTok extends Token {
    public final String s;
    public final int c;
    
    public ColorTok(int o, String s) {
      super(o, '#');
      this.s = s;
      Integer got = ColorUtils.parse(s);
      if (got==null) throw new RuntimeException("Bad color: #"+s);
      this.c = got;
    }
    
    public String toString() { return "Col(#\""+c+"\")"; }
    public String expl() { return "string"; }
  }
  
  public static class NameTok extends Token {
    public final String s;
    public NameTok(int o, String s) { super(o, 'v'); this.s=s; }
    public String toString() { return "Name("+s+")"; }
  }
  
  public static class NumTok extends Token {
    public final double num;
    public final String s;
    
    public NumTok(int o, double num, String s) {
      super(o, 'n');
      this.num = num;
      this.s = s;
    }
    
    public String toString() { return "typed("+num+" "+s+")"; }
    public String expl() { return s.length()==0? "number '"+num+"'" : "unit "+num+s+"'"; }
  }
  
  public static class StrTok extends Token {
    public final String s;
    
    public StrTok(int o, String s) {
      super(o, 's');
      this.s = s;
    }
    
    public String toString() { return "Str(\""+s+"\")"; }
    public String expl() { return "string"; }
  }
  
  public static class EOFTok extends Token {
    public EOFTok(int o) { super(o, '\0'); }
    public String toString() { return "EOF"; }
    public String expl() { return "EOF"; }
  }
}
