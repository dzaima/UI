package dzaima.ui.node.types.editable.code;

import dzaima.ui.gui.Font;
import dzaima.utils.Vec;

import java.util.HashMap;
import java.util.function.Function;

public class Langs {
  public final Vec<Language> langs = new Vec<>();
  public final HashMap<String, Language> extMap = new HashMap<>();
  public final HashMap<String, Language> nameMap = new HashMap<>();
  
  public Language defLang;
  
  public Langs() {
    addLang("Java",    JavaLang::new, "java");
    addLang("C",       CLang::new, "c", "h", "cc", "cpp", "hh", "hpp"); // temporarily includes c++ extensions too
    addLang("BQN",     BQNLang::new, "bqn");
    addLang("APL",     APLLang::new, "apl");
    addLang("Singeli", SingeliLang::new, "singeli");
    defLang = new Language("Text", new String[0], TextLang::new);
  }
  
  public void addLang(String name, Function<Font, Lang> gen, String... ext) {
    Language l = new Language(name, ext, gen);
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
