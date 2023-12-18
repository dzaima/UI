package dzaima.ui.node.types.editable.code;

import dzaima.ui.gui.Font;

public class Language {
  public final String name;
  public final String[] extensions;
  public final Lang gen;
  
  public Language(String name, String[] extensions, Lang gen) {
    this.name = name;
    this.extensions = extensions;
    this.gen = gen;
  }
  
  public Lang.LangInst inst(Font f) {
    return gen.forFont(f);
  }
}
