package dzaima.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class Time {
  private final static ZoneId tz = ZoneId.systemDefault();
  private final static DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm:ss");
  private final static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  public static String format(Instant t) { // TODO special node type with auto-updating
    ZonedDateTime z = t.atZone(tz);
    return z.format(z.toLocalDate().equals(LocalDate.now())? tf : df);
  }
}
