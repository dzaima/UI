package dzaima.ui.node.types.editable.code;

import dzaima.ui.node.types.editable.code.langs.*;
import dzaima.utils.Vec;

import java.util.HashMap;

public class Langs {
  public final Vec<Language> langs = new Vec<>();
  public final HashMap<String, Language> extMap = new HashMap<>();
  public final HashMap<String, Language> nameMap = new HashMap<>();
  
  public Language defLang;
  
  public Langs() {
    addLang("APL",     new APLLang(), "apl");
    addLang("Java",    new JavaLang(), "java");
    addLang("C",       new CLang(), "c", "h", "cc", "cpp", "hh", "hpp", "cxx", "hxx"); // temporarily includes c++ extensions too
    addLang("BQN",     new BQNLang(), "bqn");
    addLang("Singeli", new SingeliLang(), "singeli");
    addLang("assembly", AsmLang.GENERIC);
    addLang("generic assembly", AsmLang.GENERIC);
    addLang("x86 assembly", AsmLang.X86, "asm", "s");
    defLang = new Language("Text", new String[0], new TextLang());
  }
  
  public void addLang(String name, Lang lang, String... ext) {
    Language l = new Language(name, ext, lang);
    for (String c : ext) extMap.put(c, l);
    nameMap.put(name.toLowerCase(), l);
    langs.add(l);
  }
  
  public Language fromNameNullable(String name) {
    return name==null? null : nameMap.get(name.toLowerCase());
  }
  public Language fromName(String name) {
    Language r = fromNameNullable(name);
    if (r==null) return defLang;
    return r;
  }
  
  public Language fromFilename(String filename) {
    int d = filename.lastIndexOf('.');
    if (d!=-1) filename = filename.substring(d+1).toLowerCase();
    Language l = extMap.get(filename);
    if (l==null) return defLang;
    return l;
  }
}
