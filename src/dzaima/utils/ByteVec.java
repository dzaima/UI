package dzaima.utils;

import java.util.Arrays;

public class ByteVec {
  private static final byte[] EMPTY = new byte[0];
  public byte[] arr = EMPTY;
  public int sz;
  
  public byte get(int i) {
    return arr[i];
  }
  
  public void add(byte t) {
    if (sz < arr.length) {
      arr[sz] = t;
      sz++;
    } else {
      dcap();
      arr[sz++] = t;
    }
  }
  public byte pop() {
    return arr[--sz];
  }
  
  
  
  
  
  public byte[] get(int s, int e) {
    return Arrays.copyOfRange(arr, s, e);
  }
  public void insert(int i, byte t) {
    if (sz >= arr.length) dcap();
    System.arraycopy(arr, i, arr, i+1, sz-i);
    sz++;
    arr[i] = t;
  }
  public void remove(int s, int e) {
    System.arraycopy(arr, e, arr, s, sz-e);
    sz-= e-s;
  }
  
  public void insertFill(int i, int am, byte fill) {
    while (sz+ am > arr.length) dcap();
    System.arraycopy(arr, i, arr, i+am, sz-i);
    Arrays.fill(arr, i, i+am, fill);
    sz+= am;
  }
  public void addAll(int i, byte[] t) {
    while (sz+t.length > arr.length) dcap();
    System.arraycopy(arr, i, arr, i+t.length, sz-i);
    System.arraycopy(t, 0, arr, i, t.length);
    sz+= t.length;
  }
  
  
  private void dcap() {
    byte[] narr = new byte[arr.length<<1 | 7];
    System.arraycopy(arr, 0, narr, 0, arr.length);
    arr = narr;
  }
}
