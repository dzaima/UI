package dzaima.ui.eval;

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
      Integer got = parse(s);
      if (got==null) throw new RuntimeException("Bad color: #"+s);
      this.c = got;
    }
    
    public static Integer parse(String s) { // doesn't expect a starting '#'
      try {
        int p = Integer.parseUnsignedInt(s, 16);
        if (s.length()==8) return p;
        if (s.length()==7) return p|p>>>24<<28;
        if (s.length()==6) return p|0xff000000;
        if (s.length()==3) p|= 0xf000;
        if (s.length()==3 || s.length()==4) return ((p>>12&15)<<24 | (p>>8&15)<<16 | (p>>4&15)<<8 | (p&15)) * 0x11;
        if (s.length()==2) return p*0x010101 | 0xff000000;
        if (s.length()==1) return p*0x111111 | 0xff000000;
        return null;
      } catch (Throwable t) {
        return null;
      }
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
