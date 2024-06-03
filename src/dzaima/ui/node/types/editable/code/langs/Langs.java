package dzaima.ui.node.types.editable.code.langs;

import dzaima.utils.Vec;

import java.util.HashMap;

public class Langs {
  public Lang defLang;
  public final Vec<Lang> langs = new Vec<>();
  public final HashMap<String, Lang> extMap = new HashMap<>();
  public final HashMap<String, Lang> fromNameMap = new HashMap<>();
  public final HashMap<Lang, String> toNameMap = new HashMap<>();
  
  public Langs() {
    addLang("APL",     new APLLang(), "apl");
    addLang("Java",    new JavaLang(), "java");
    addLang("Rust",    new RustLang(), "rs");
    addLang("C",       new CLang(), "c", "h", "cc", "cpp", "hh", "hpp", "cxx", "hxx"); // temporarily includes c++ extensions too
    addLang("BQN",     new BQNLang(), "bqn");
    addLang("Singeli", new SingeliLang(), "singeli");
    addLang("assembly", AsmLang.GENERIC);
    addLang("generic assembly", AsmLang.GENERIC);
    addLang("x86 assembly", AsmLang.X86, "asm", "s");
    addLang("risc-v assembly", AsmLang.RISCV);
    addLang("aarch64 assembly", AsmLang.AARCH64);
    defLang = new TextLang();
  }
  
  public void addLang(String name, Lang lang, String... ext) {
    for (String c : ext) extMap.put(c, lang);
    fromNameMap.put(name.toLowerCase(), lang);
    toNameMap.put(lang, name);
    langs.add(lang);
  }
  
  public Lang fromNameNullable(String name) {
    return name==null? null : fromNameMap.get(name.toLowerCase());
  }
  public Lang fromName(String name) {
    Lang r = fromNameNullable(name);
    if (r==null) return defLang;
    return r;
  }
  
  public Lang fromFilename(String filename) {
    int d = filename.lastIndexOf('.');
    if (d!=-1) filename = filename.substring(d+1).toLowerCase();
    Lang l = extMap.get(filename);
    if (l==null) return defLang;
    return l;
  }
  
  public String nameOf(Lang l) {
    return toNameMap.get(l);
  }
}
