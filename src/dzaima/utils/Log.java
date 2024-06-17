package dzaima.utils;

import java.io.*;
import java.time.Instant;
import java.util.function.Supplier;

public class Log {
  public static LogConsumer _logger = new StdoutLogger();
  
  
  public enum Level {
    FINE(0), INFO(1), WARN(2), ERROR(3);
    public final int i;
    Level(int i) { this.i = i; }
  }
  public static Level level = Level.WARN;
  
  public static abstract class LogConsumer {
    public abstract void log(Level l, String component, String msg);
  }
  
  public static void log(Level l,                   String msg)           { if (l.i>=level.i) _logger.log(l, null,      msg); }
  public static void log(Level l, String component, String msg)           { if (l.i>=level.i) _logger.log(l, component, msg); }
  public static void log(Level l, String component, Supplier<String> msg) { if (l.i>=level.i) _logger.log(l, component, msg.get()); }
  
  public static void info(String msg) { log(Level.INFO,  msg); }
  public static void warn(String msg) { log(Level.WARN,  msg); }
  
  public static void fine (String component, String msg) { log(Level.FINE,  component, msg); }
  public static void info (String component, String msg) { log(Level.INFO,  component, msg); }
  public static void warn (String component, String msg) { log(Level.WARN,  component, msg); }
  public static void error(String component, String msg) { log(Level.ERROR, component, msg); }
  
  public static void fine (String component, Supplier<String> msg) { log(Level.FINE,  component, msg); }
  public static void info (String component, Supplier<String> msg) { log(Level.INFO,  component, msg); }
  public static void warn (String component, Supplier<String> msg) { log(Level.WARN,  component, msg); }
  public static void error(String component, Supplier<String> msg) { log(Level.ERROR, component, msg); }
  
  public static void stacktrace(String component, Throwable e) {
    StringWriter w = new StringWriter();
    e.printStackTrace(new PrintWriter(w));
    error(component, w.toString());
  }
  public static void stacktraceHere(String component) {
    stacktrace(component, new Throwable());
  }
  
  private static Vec<Runnable> onLogLevelChange;
  public static void setLogLevel(Level l) {
    level = l;
    if (onLogLevelChange!=null) for (Runnable r : onLogLevelChange) r.run();
  }
  public static void onLogLevelChanged(Runnable r) {
    if (onLogLevelChange==null) onLogLevelChange = new Vec<>();
    onLogLevelChange.add(r);
    r.run();
  }
  
  public static class StdoutLogger extends LogConsumer {
    public void log(Level l, String component, String msg) {
      StringBuilder b = new StringBuilder("[").append(Instant.now());
      
      if (l==Level.WARN) b.append('!');
      else if (l==Level.ERROR) b.append("!!!");
      
      if (component!=null) b.append(' ').append(component);
      b.append("] ").append(msg);
      
      System.err.println(b);
    }
  }
}
