package dzaima.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class Time {
  public final static ZoneId tz = ZoneId.systemDefault();
  public static ZonedDateTime localDateTime(Instant t) {
    return t.atZone(tz);
  }
  
  private final static DateTimeFormatter dt0 = DateTimeFormatter.ofPattern("HH:mm:ss");
  private final static DateTimeFormatter dt1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  public static String localTimeStr(Instant t) { // TODO special node type with auto-updating
    ZonedDateTime z = t.atZone(tz);
    return z.format(z.toLocalDate().equals(LocalDate.now())? dt0 : dt1);
  }
}
