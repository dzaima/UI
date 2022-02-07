package dzaima.ui.apps.devtools;

public class RotBuf {
  public static int BUF_LEN = 600;
  public static int SUM_LEN = 60;
  public long sum;
  
  public final long[] ls = new long[BUF_LEN];
  public int i = 0;
  
  public void add(long n) {
    if (i >= ls.length) i = 0;
    
    int di = i - SUM_LEN;
    if (di < 0) di+= BUF_LEN;
    
    sum+= n - ls[di];
    
    ls[i] = n;
    i++;
  }
}
