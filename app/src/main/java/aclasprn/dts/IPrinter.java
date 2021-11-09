package aclasprn.dts;

import android.graphics.Bitmap;

import java.io.File;

public interface IPrinter {
    public boolean openPrinter(int iType);
    public void closePrinter();
    public int getLineWidthDot();
    public void sendBitmap(Bitmap bitmap);
    public void sendEscPosData(byte[] bData,int iLen);
    public void printPDF(File strFile, int iType);
    public boolean getPrinterPaperStatus();
    public void doFeed(int iStep);
    public void doCut();
    public void doOpenDrawer();
    public void setContrast(int iVal);
}
