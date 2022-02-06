package dzaima.ui.node.types.editable.code;

import dzaima.ui.gui.Font;

import java.util.function.Function;

public class Language {
  public final String name;
  public final String[] extensions;
  public final Function<Font, Lang> gen;
  
  public Language(String name, String[] extensions, Function<Font, Lang> gen) {
    this.name = name;
    this.extensions = extensions;
    this.gen = gen;
  }
  
  public Lang inst(Font f) {
    return gen.apply(f);
  }
}
