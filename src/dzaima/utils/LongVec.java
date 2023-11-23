package dzaima.utils;

import java.util.Arrays;

public class LongVec {
  private static final long[] EMPTY = new long[0];
  public long[] arr = EMPTY;
  public int sz;
  
  public long get(int i) {
    return arr[i];
  }
  
  public void add(long t) {
    if (sz < arr.length) {
      arr[sz] = t;
      sz++;
    } else {
      dcap();
      arr[sz++] = t;
    }
  }
  public long pop() {
    return arr[--sz];
  }
  
  
  
  
  
  public void insert(int i, long t) {
    if (sz >= arr.length) dcap();
    System.arraycopy(arr, i, arr, i+1, sz-i);
    sz++;
    arr[i] = t;
  }
  public long[] get(int s, int e) {
    return Arrays.copyOfRange(arr, s, e);
  }
  public long[] get() {
    return Arrays.copyOf(arr, sz);
  }
  public void remove(int s, int e) {
    System.arraycopy(arr, e, arr, s, sz-e);
    sz-= e-s;
  }
  public void addAll(int i, long[] t) {
    while (sz+t.length > arr.length) dcap();
    System.arraycopy(arr, i, arr, i+t.length, sz-i);
    System.arraycopy(t, 0, arr, i, t.length);
    sz+= t.length;
  }
  
  public void sort() {
    Arrays.sort(arr, 0, sz);
  }
  
  private void dcap() {
    long[] narr = new long[arr.length<<1 | 7];
    System.arraycopy(arr, 0, narr, 0, arr.length);
    arr = narr;
  }
}
