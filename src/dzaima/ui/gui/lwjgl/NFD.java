package dzaima.ui.gui.lwjgl;

import dzaima.ui.gui.io.FileFilter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.*;
import org.lwjgl.util.nfd.*;

import java.nio.file.*;
import java.util.function.Consumer;

import static org.lwjgl.system.MemoryStack.stackPush;

public class NFD {
  static { NativeFileDialog.NFD_Init(); }
  private static NFDFilterItem.Buffer loadFilter(FileFilter filter, MemoryStack stack) {
    // NFDFilterItem.Buffer b = NFDFilterItem.malloc(1, stack);
    // b.get(0).name(stack.UTF8("images (jpg)")).spec(stack.UTF8("jpg"));
    return null;
  }
  private static String loadPath(Path path) {
    return path==null? null : path.toAbsolutePath().toString();
  }
  
  public static void openFileStatic(FileFilter filter, Path initialPath, Consumer<Path> onResult) {
    try (MemoryStack stack = stackPush()) {
      PointerBuffer r = stack.mallocPointer(1);
      int x = NativeFileDialog.NFD_OpenDialog(r, loadFilter(filter, null), loadPath(initialPath));
      if (x==NativeFileDialog.NFD_OKAY) onResult.accept(Paths.get(r.getStringUTF8(0)));
      else onResult.accept(null);
    }
  }
  
  public static void openFolderStatic(Path initialPath, Consumer<Path> onResult) {
    try (MemoryStack stack = stackPush()) {
      PointerBuffer r = stack.mallocPointer(1);
      int x = NativeFileDialog.NFD_PickFolder(r, loadPath(initialPath));
      if (x==NativeFileDialog.NFD_OKAY) onResult.accept(Paths.get(r.getStringUTF8(0)));
      else onResult.accept(null);
    }
  }
  public static void saveFileStatic(FileFilter filter, Path initialPath, String initialName, Consumer<Path> onResult) {
    try (MemoryStack stack = stackPush()) {
      PointerBuffer r = stack.mallocPointer(1);
      int x = NativeFileDialog.NFD_SaveDialog(r, loadFilter(filter, stack), loadPath(initialPath), initialName);
      if (x==NativeFileDialog.NFD_OKAY) onResult.accept(Paths.get(r.getStringUTF8(0)));
      else onResult.accept(null);
    }
  }
}
