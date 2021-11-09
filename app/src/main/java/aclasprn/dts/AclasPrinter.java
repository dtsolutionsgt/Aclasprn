package aclasprn.dts;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Log;


import java.io.File;
import java.util.ArrayList;

import aclasdriver.AclasDevice;
import aclasdriver.AclasFactory;
import aclasdriver.AclasTool;



public class AclasPrinter implements IPrinter{

    private AclasDevice devPrn              = null;
    private int         iLineWidth             = 576;
    private final String tag = "AclasPrinter";

    public AclasPrinter(Context context){
        devPrn  = AclasFactory.getInstance().GetAclasDevice(AclasFactory.DEV_PRINTER);
    }


    /**
     *AOBX 2inch
     * AO4X 2inch
     * AO5  3inch
     * @param iType  0:2inch ; 1:3inch
     * @return successful:true; failed:false
     */
    public boolean openPrinter(int iType){

        boolean bFlag = false;
        if(devPrn!=null){
            String strPara = String.valueOf(iType==0?AclasDevice.PRN_TYPE_2INCH:AclasDevice.PRN_TYPE_3INCH);
            if(devPrn.AclasOpen(strPara)>=0){
                bFlag   = true;
                byte[] bWidth = new byte[2];
                int iRet = devPrn.AclasRead(bWidth,AclasDevice.PRN_GET_WIDTH);
                if(iRet==2){
                    iLineWidth  = AclasTool.getInstance().bytesToInt(bWidth,iRet);
                    Log.d(tag,"get width:"+iLineWidth);
                }
            }
        }
        return bFlag;
    }

    /**
     * Close printer
     */
    public void closePrinter(){
        if(devPrn!=null){
            devPrn.AclasClose();
        }
    }

    /**
     *
     * @return   dot number of a line
     */
    public int getLineWidthDot(){
        if(devPrn!=null){
            return iLineWidth;
        }
        return 384;
    }
    /**
     * Get printer has paper or not.
     */
    public boolean getPrinterPaperStatus(){
        if(devPrn!=null){
            boolean bFlag = false;
            byte[] bData = new byte[1];
            int iRet = devPrn.AclasRead(bData,AclasDevice.PRN_GET_STATUS);//Get printer paper status
            if(iRet>0){
                bFlag = bData[0]==1;
            }
            return bFlag;
        }
        return false;
    }

    /**
     * Feed paper
     * @param iStep: lines value
     */
    public void doFeed(int iStep){
        if(devPrn!=null){
            byte[] bData = new byte[1];
            bData[0]    = (byte)iStep;
            devPrn.AclasWrite(bData,1,AclasDevice.PRN_DATA_FEED);
        }
    }

    /**
     * Cut paper, halfcut
     */
    public void doCut(){
        if(devPrn!=null){
            byte[] cmd = new byte[] { 0x1D, 0x56, 0x42, 0x00};
            devPrn.AclasWrite(cmd,4,AclasDevice.PRN_DATA_ESC);
        }
    }


// EPSON_CMD_DRAWER    "\x1b\x43\x00\x00\x00"
// EPSON_CMD_DRAWER_1    "\x1b\x71\x00\x3c\xff"
    public void doOpenDrawer(){
        if(devPrn!=null){
            byte[] cmd1 = new byte[] { 0x1b, 0x71, 0x00, 0x3c,(byte)0xff};
            byte[] cmd2 = new byte[] { 0x1b, 0x43, 0x00, 0x00, 0x00};
            devPrn.AclasWrite(cmd1,cmd1.length,AclasDevice.PRN_DATA_ESC);
            devPrn.AclasWrite(cmd2,cmd2.length,AclasDevice.PRN_DATA_ESC);
        }
    }

    /**
     *
     * @param iVal 1<=iVal<=8
     */
    public void setContrast(int iVal){
        if(devPrn!=null){
            byte[] bData = new byte[1];
            bData[0]    = (byte)iVal;
            devPrn.AclasWrite(bData,1,AclasDevice.PRN_DATA_CONTRAST);
        }
    }

    /**
     * Send bitmap to printer
     * @param bitmap
     */
    public void sendBitmap(Bitmap bitmap){
        if(devPrn!=null){
          //  byte[] bData = AclasTool.getInstance().getBitmapDotData1(bitmap,iLineWidth);
            byte[] bData = AclasTool.getInstance().getBitmapDotData(bitmap,iLineWidth);
            devPrn.AclasWrite(bData,bData.length,AclasDevice.PRN_DATA_BMPDOT);
        }
    }

    /**
     * Send ESCPOS data to printer
     * @param bData  ESCPOS data
     * @param iLen   array's len
     */
    public void sendEscPosData(byte[] bData,int iLen){
        if(devPrn!=null){
            devPrn.AclasWrite(bData,iLen,AclasDevice.PRN_DATA_ESC);
        }
    }

    public void printPDF(File strPath,int iType){
        ArrayList<Bitmap> list = changePDFToImages(strPath,iType);
        if(list!=null){
            int iSize = list.size();
            Log.d("AclasPrinter","printPDF bit:"+iSize);
            for(int i=0;i<iSize;i++){
                sendBitmap(list.get(i));
            }
        }
    }
    @TargetApi(21)
    public ArrayList<Bitmap> changePDFToImages(File file, int iType){
        ArrayList<Bitmap> list = null;
        try{
           // Log.d("AclasPrinter","changePDFToImages path:"+strPath);
            ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(file,  ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);
            int iCnt = pdfRenderer.getPageCount();
            list = new ArrayList<>();
            int iLineDotWidth = iType==0?384:576;
            for(int i=0;i<iCnt;i++){
                PdfRenderer.Page page = pdfRenderer.openPage(i);
                int	iWidthPage = page.getWidth()*8;
                double dWidth = iWidthPage;
                double dHeight = page.getHeight()*8;
                Bitmap bitmap = Bitmap.createBitmap((int)dWidth, (int)dHeight, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null,  PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                Bitmap bit = changeBmp(bitmap,iLineDotWidth);
                page.close();
                list.add(bit);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }

    private Bitmap changeBmp(Bitmap bmp,int DotLineWidth){
        bmp =  Bitmap.createScaledBitmap(bmp, DotLineWidth, bmp.getHeight()*DotLineWidth/bmp.getWidth(), true);
        Bitmap bmpRet = Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(),Bitmap.Config.ARGB_8888);
		Log.d(tag," PDF changeBmp w:"+bmp.getWidth()+" h:"+bmp.getHeight());
//		Bitmap bmpRet = Bitmap.createBitmap(384,500,Bitmap.Config.ARGB_8888);
        Canvas cas = new Canvas(bmpRet);
        cas.drawColor(Color.WHITE);
        Paint paint	= new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        cas.drawBitmap(bmp,0,0, paint);
        return  bmpRet;
    }
}
