package dzaima.ui.gui.undo;

public abstract class Undo extends UndoR<Void> {
  public abstract void redo();
  public final Void redoR() {
    redo();
    return null;
  }
}
