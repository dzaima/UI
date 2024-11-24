package dzaima.ui.gui.undo;

import dzaima.utils.Vec;

import java.util.function.Consumer;

public class UndoFrame {
  public final Vec<UndoR<?>> is = new Vec<>();
  public final String id;
  public final int mode;
  public final long time;
  public boolean important;
  private Consumer<Boolean> onComplete; // called with false on completed undo, and true on redo
  
  public UndoFrame(String id, int mode, long time) {
    this.id = id;
    this.mode = mode;
    this.time = time;
    assert id!=null;
  }
  
  public void setAndRunOnComplete(Consumer<Boolean> onComplete) {
    setOnComplete(onComplete);
    onComplete.accept(true);
  }
  public void setOnComplete(Consumer<Boolean> onComplete) {
    assert this.onComplete == null;
    this.onComplete = onComplete;
  }
  
  public void undo() {
    for (int i = is.sz-1; i >= 0; i--) is.get(i).undo();
    if (onComplete!=null) onComplete.accept(false);
  }
  public void redo() {
    for (UndoR<?> c : is) c.redoR();
    if (onComplete!=null) onComplete.accept(true);
  }
}
