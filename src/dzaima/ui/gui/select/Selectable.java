package dzaima.ui.gui.select;

public interface Selectable {
  void selectS(Selection s);
  void selectE(Selection s);
  String selType();
  
  default boolean selectable() {
    return true;
  }
}
