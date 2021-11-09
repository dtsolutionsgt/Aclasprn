package aclasprn.dts;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;

import cn.weipass.pos.sdk.IPrint;
import cn.weipass.pos.sdk.IPrint.OnEventListener;
import cn.weipass.pos.sdk.LatticePrinter;
import cn.weipass.pos.sdk.Weipos;
import cn.weipass.pos.sdk.impl.WeiposImpl;

public class AclasPrinter3066 implements IPrinter{

    private final String tag = "AclasPrinter3066";
    private LatticePrinter  mPrinter    = null;
    private Context mContext    = null;
    //private boolean mFlagOpen   = false;
    public AclasPrinter3066(Context context){
        mContext    = context;
    }

    public boolean openPrinter(int iType){
        if(mContext!=null){
            WeiposImpl.as().init(mContext, new Weipos.OnInitListener() {
                @Override
                public void onInitOk() {
                    try {
                        mPrinter    = WeiposImpl.as().openLatticePrinter();
                        mPrinter.setOnEventListener(new OnEventListener() {
                            @Override
                            public void onEvent(int i, String s) {

                            }
                        });
                        Log.d(tag,"onInitOk");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(String s) {

                }

                @Override
                public void onDestroy() {

                }
            });
        }
        return true;
    }

    public void closePrinter(){
        WeiposImpl.as().destroy();
        mPrinter    = null;
    }

    public int getLineWidthDot(){
        return 384;
    }


    public void sendBitmap(Bitmap bitmap){
        if(mPrinter!=null){
            mPrinter.printImage(bitmap2Bytes(bitmap),IPrint.Gravity.CENTER);
            mPrinter.submitPrint();
        }
    }

    public void sendEscPosData(byte[] bData,int iLen){
        if(mPrinter!=null){
            mPrinter.writeData(bData,iLen);
        }
    }
    /**
     * Get printer has paper or not.
     */
    public boolean getPrinterPaperStatus(){
        boolean bFlag = true;
//        if(mPrinter!=null){
//            WeiposImpl.as().getWeiposStatus()
//        }
        return bFlag;
    }

    /**
     * Feed paper
     * @param iStep: lines value
     */
    public void doFeed(int iStep){
        if(mPrinter!=null){
            iStep   = iStep>0?iStep:1;
            byte[] bData = new byte[iStep];
            for(int i=0;i<iStep;i++){
                bData[i]    = '\n';
            }
            mPrinter.writeData(bData,bData.length);
        }
    }

    /**
     * Cut paper, halfcut
     */
    public void doCut(){
        if(mPrinter!=null){
            byte[] cmd = new byte[] { 0x1D, 0x56, 0x42, 0x00};
            mPrinter.writeData(cmd,cmd.length);
        }
    }

    public static byte[] bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    public void doOpenDrawer(){

    }
    public void setContrast(int iVal){

    }


    public void printPDF(File strFile, int iType){

    }
}

