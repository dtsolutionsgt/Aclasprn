package aclasprn.dts;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import aclasdriver.AclasBaseFunction;
import aclasdriver.AclasTool;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    
    private String mac="",fname="",QRCodeStr="";
    private int askprint,copies;

    private static final int REQUEST_PERMISSION = 100;

    //-------------------------------------------------------

    private final int iDevType = 6;// 0 AOBX ; 1 AO4; 2 AO5; 3 AOW; 4 AOA; 5 3066;
    private AclasTool tool = null;
    private Spinner spDeviceType  = null;
    private AclasBaseFunction mBase = null;

    private final Timer timer = new Timer();

    private boolean bFlagFileSelect = false;
    private Uri treeUri = null;
    private DocumentFile fileLog = null;
    private String strLogName = "";
    private final String strFileName = "setting.txt";
    private String[] listData = null;
    private int     iMagCnt     = 30;
    private Uri treeUriLog = null;

    private boolean bFlagReOpenPrn = false;
    private boolean bFlagReOpenDrawer = false;

    private static final int MESSAGE_TIMER = 0;
    private static final int MESSAGE_UARTREAD = 1;
    private static final int MESSAGE_PRINTFILE = 6;

    private static final int lineLen =32;

    //----------------------------printer---------------------------

    private IPrinter    devPrn                 = null;
    private Spinner     spPrnType              = null;
    private int         iPtnType               = 0;//0 - 2inch; 1 - 3inch
    private Button      btnPrnOpen             = null;
    private TextView    tvPrnStatus            = null;
    private String      strStatusPaperExist   = null;
    private String      strStatusPaperNoExist = null;
    private int         iLineWidth             = 576;
    private boolean     bFlagPrinterOpen        = false;

    //--------------------------------drawer------------------------

    private AclasDrawer   devDrawer                 = null;
    private boolean     bFlagDrawerOpen            = false;
    private Button      btnDrawerOpen               = null;
    //private boolean     bFlagDrawerClisk            = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestRuntimePermission();

        Bundle bundle = getIntent().getExtras();
        processBundle(bundle);

        if (fname.isEmpty()) fname=Environment.getExternalStorageDirectory().getAbsolutePath()+"/print.txt";

        createAclasDevices();
        initWidget();

        String strUri  = readUri();
        if(strUri.length()>0) treeUriLog = Uri.parse(strUri); else treeUriLog = null;

        showLog(Build.MODEL);

        Handler mtimer = new Handler();
        Runnable mrunner=new Runnable() {
            @Override
            public void run() {
                doPrint(null);
            }
        };
        mtimer.postDelayed(mrunner,1200);


    }

    protected void onActivityResult(int requestCode, int resultCode,Intent data) {
        if (resultCode == RESULT_OK) {
            treeUri = data.getData();

            getContentResolver().takePersistableUriPermission(treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if(treeUri!=null) {
                switch (requestCode) {
                    case 0:
                        String strPath	= treeUri.getPath();
                        if(strPath.contains("pdf")||strPath.contains(".bmp")||strPath.contains(".jpg")){
                            String path = tool.getFilePathFromURI(MainActivity.this,treeUri);
                            if(path!=null){
                                Message msg = mHandler.obtainMessage(MESSAGE_PRINTFILE,path);
                                mHandler.sendMessageDelayed(msg,100);
                            }
                        }else {
                            mHandler.sendEmptyMessageDelayed(MESSAGE_PRINTFILE,100);
                            //printBmp();
                        }
                        break;
                    case 1:
                        treeUriLog = treeUri;
                        saveUri(treeUri.toString());
                        break;
                }
            }

        }else{
            if(requestCode==0){
                mHandler.sendEmptyMessageDelayed(MESSAGE_PRINTFILE,100);//.sendEmptyMessage(MESSAGE_PRINTFILE);
            }
        }
        bFlagFileSelect = false;
    }

    //region Events

    public void doPrint(View v) {
        if(!bFlagPrinterOpen){
            iPtnType          = spPrnType.getSelectedItemPosition();
            bFlagPrinterOpen    = devPrn.openPrinter(iPtnType);
            iLineWidth          = devPrn.getLineWidthDot();
            showLog(bFlagPrinterOpen?("Open Printer Successfully:"+String.valueOf(iPtnType)):"Open Printer Failed");
            doPrint();
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.prn_btn_open:
                if(!bFlagPrinterOpen){
                    iPtnType          = spPrnType.getSelectedItemPosition();
                    bFlagPrinterOpen    = devPrn.openPrinter(iPtnType);
                    iLineWidth          = devPrn.getLineWidthDot();
                    showLog(bFlagPrinterOpen?("Open Printer Successfully:"+String.valueOf(iPtnType)):"Open Printer Failed");
                    doPrint();
                }
                break;
                /*
            case R.id.prn_btn_close:
                bFlagPrinterOpen = false;
                devPrn.closePrinter();
                showLog("Close Printer");
                uiShowPrinter(bFlagPrinterOpen);
                break;


            case R.id.prn_btn_feed:
                devPrn.doFeed(5);
                showLog("Printer Feed");
                break;
            case R.id.prn_btn_printbmp:
                onBtnSendBmpClick();
                break;
            case R.id.prn_btn_printesc:
                printEscData();
                showLog("Send ESCPOS Data");
                devPrn.doCut();
                showLog("Printer Cut Paper");

                bFlagPrinterOpen = false;
                devPrn.closePrinter();
                showLog("Close Printer");
                break;
                */
            case R.id.drawer_btn_open:
                bFlagDrawerOpen = devDrawer.openDrawer();
                uiShowDrawer(bFlagDrawerOpen);
                showLog("Open Drawer Device:"+bFlagDrawerOpen);
                break;
            default:
                break;
        }
    }

    private void onBtnSendBmpClick(){
        bFlagFileSelect = true;
        doPrintPDFBMP(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //endregion

    //region Main

    private void doPrint(){

        try {

            for (int i = 0; i <copies; i++) {

                printEscData();
                showLog("Send ESCPOS Data");

                try {
                    if(!QRCodeStr.isEmpty()) {
                        String qrname = Environment.getExternalStorageDirectory().getAbsolutePath()+"/qr.jpg";
                        printBmp(qrname);
                    }
                } catch (Exception e) {
                }

                devPrn.doCut();
                showLog("Printer Cut Paper");
            }

            devDrawer.openDrawer();

        } catch (Exception e) {
            showMsg(new Object(){}.getClass().getEnclosingMethod().getName()+" . "+e.getMessage());
        }


        exitApp();
    }

    private boolean CheckDevice3066(){
        boolean bFlag   = false;
        String strModel = Build.MODEL;
        if(strModel.contains("3066")){
            bFlag   = true;
        }
        return bFlag;
    }

    private void createAclasDevices(){
        tool    = AclasTool.getInstance();

        if(CheckDevice3066()){
            devPrn  = new AclasPrinter3066(MainActivity.this);
        }else{
            devPrn  = new AclasPrinter(MainActivity.this);
        }

        devDrawer = new AclasDrawer();

        //    devScale    = new ScaleManagerCus();
    }

    private void initWidget(){
        btnPrnOpen  = (Button)findViewById(R.id.prn_btn_open);
        btnPrnOpen.setOnClickListener(this);
        spPrnType       = (Spinner)findViewById(R.id.prn_sp_type);

        tvPrnStatus     = (TextView)findViewById(R.id.prn_tv_status);
        strStatusPaperExist = getResources().getString(R.string.prn_havepaper);
        strStatusPaperNoExist = getResources().getString(R.string.prn_nopaper);

        timer.schedule(task,1000,500);

        btnDrawerOpen  = (Button)findViewById(R.id.drawer_btn_open);
        btnDrawerOpen.setOnClickListener(this);

        spDeviceType    = (Spinner)findViewById(R.id.device_sp_type);
        spDeviceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0://AOBX
                        spPrnType.setSelection(0);
                        spPrnType.setEnabled(false);
                        break;
                    case 1://AO4X
                        spPrnType.setSelection(0);
                        spPrnType.setEnabled(false);
                        break;
                    case 2://AO5
                        spPrnType.setSelection(1);
                        spPrnType.setEnabled(false);
                        break;
                    case 3://AOW
                        break;
                    case 4://AOA
                        spPrnType.setSelection(1);
                        spPrnType.setEnabled(false);
                        break;
                    case 5://AOW3066
                        spPrnType.setSelection(0);
                        spPrnType.setEnabled(false);
                        break;
                    default:
                        spPrnType.setEnabled(true);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spDeviceType.setSelection(iDevType);
    }

    private void processBundle(Bundle b) {
        String flog=Environment.getExternalStorageDirectory()+"/logprint.txt";
        String ss="";
        String lf="\r\n";

        try {
            //FileOutputStream fos=new FileOutputStream(flog);

            try {
                mac=b.getString("mac");
                ss=mac;
            } catch (Exception e) {
                mac="BT:00:01:90:85:0D:8C";ss=e.getMessage();
            }
            //fos.write(ss.getBytes());fos.write(lf.getBytes());

            try {
                fname=b.getString("fname");ss=fname;
            } catch (Exception e) {
                fname="";ss=e.getMessage();
            }
            //fos.write(ss.getBytes());fos.write(lf.getBytes());

            try {
                QRCodeStr=b.getString("QRCodeStr");ss=QRCodeStr;
            } catch (Exception e) {
                QRCodeStr="";ss=e.getMessage();
            }
            //fos.write(ss.getBytes());fos.write(lf.getBytes());

            try {
                askprint=b.getInt("askprint");ss=""+askprint;
            } catch (Exception e) {
                askprint=0;ss=e.getMessage();
            }
            //fos.write(ss.getBytes());fos.write(lf.getBytes());

            try {
                copies=b.getInt("copies");ss=""+copies;
            } catch (Exception e) {
                copies=1;ss=e.getMessage();
            }
            //fos.write(ss.getBytes());fos.write(lf.getBytes());

            //fos.close();
        } catch (Exception e) {
            mac="";fname="";ss="ERR : "+e.getMessage();
        }

        if (mac.isEmpty()) mac="BT:00:01:90:85:0D:8C";
        if (fname.isEmpty()) fname=Environment.getExternalStorageDirectory()+"/print.txt";

    }

    //endregion

    //region Printer handling

    private void printEscData(){
        FileInputStream fIn;

        final byte Init[]={0x1B,0x40};
        byte[] nextLine={'\n'};

        try {

            //String fname = Environment.getExternalStorageDirectory().getAbsolutePath()+"/print.txt";
            File file = new File(fname);

            BufferedReader dfile = null;
            StringBuilder textData = new StringBuilder();
            String ss;

            try {
                fIn = new FileInputStream(file);
                dfile = new BufferedReader(new InputStreamReader(fIn));
            } catch (Exception e) {
                showMsg("No se puede leer archivo de impresión " + e.getMessage());
                return ;
            }

            textData.delete(0, textData.length());

            while ((ss = dfile.readLine()) != null) {
                if (!ss.isEmpty()) {
                    if (ss.length()>lineLen) ss=ss.substring(0,lineLen);
                }
                textData.append(ss).append("\n");
            }

            fIn.close();

            byte[] tmpData = new byte[textData.length()+128];
            int iPos=0;

            System.arraycopy(Init,0,tmpData,iPos,Init.length);iPos+=Init.length;
            System.arraycopy(nextLine,0,tmpData,iPos,nextLine.length);iPos+=nextLine.length;

            String tx=textData.toString();
            byte[] tbuff = tx.getBytes(StandardCharsets.UTF_8);
            int tlen=tx.length();

            System.arraycopy(tbuff, 0, tmpData, iPos, tlen);iPos+=tlen;
            System.arraycopy(nextLine, 0, tmpData, iPos, nextLine.length);iPos+=nextLine.length;

            devPrn.sendEscPosData(tmpData,iPos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printBmp(){
        Bitmap bitmapPrint = null;
        try {
            InputStream inputStream = getAssets().open(iPtnType==0?"sample1.bmp":"sample2.bmp");
            bitmapPrint = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            devPrn.sendBitmap(bitmapPrint);
        } catch (Exception e) {

        }
    }

    private void printBmp(String strPath){
        Bitmap map = null;
        try {
            FileInputStream inputStream	= new FileInputStream(strPath);
            map = BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {}
        if(map!=null){
            devPrn.sendBitmap(map);
        }
    }

    private void doPrintPDFBMP(String strPath){
        try {
            if(strPath!=null) printBmp(strPath); else printBmp();
            devPrn.doCut();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(MESSAGE_TIMER);
        }
    };

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_TIMER:
                    showPrinterPaperStatus();
                    break;
                case MESSAGE_UARTREAD:
                    showLog((String)(msg.obj));
                    break;
                case MESSAGE_PRINTFILE:
                    String strPath = null;
                    if(msg.obj!=null){
                        strPath = (String)(msg.obj);
                    }
                    doPrintPDFBMP(strPath);
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    private void showPrinterPaperStatus(){
        if(bFlagPrinterOpen){
            boolean bFlagPaper = devPrn.getPrinterPaperStatus();
            tvPrnStatus.setText(bFlagPaper?strStatusPaperExist:strStatusPaperNoExist);
        }
    }

    //endregion

    //region Aux

    public void showMsg(String msg) {
        Toast toast= Toast.makeText(this,msg,Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void requestRuntimePermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)  return;

        int permissionStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        List<String> requestPermissions = new ArrayList<>();

        if (permissionStorage == PackageManager.PERMISSION_DENIED) {
            requestPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionLocation == PackageManager.PERMISSION_DENIED) {
            requestPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (!requestPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, requestPermissions.toArray(new String[0]), REQUEST_PERMISSION);
        }

        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_PERMISSION_STORAGE = 100;
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(permissions, REQUEST_CODE_PERMISSION_STORAGE);
                    return;
                }
            }
        }
    }

    private void uiShowDrawer(boolean bOpen){
        btnDrawerOpen.setEnabled(!bOpen);
    }

    private void showLog(String str){
        /*
        int iSize = listLog.size();
        if(iSize>8){
            listLog.remove(iSize-1);
        }
        listLog.add(0,getTime()+" "+str);
        iSize   = listLog.size();
        String strLog = "";
        for(int i=0;i<iSize;i++){
            strLog += listLog.get(i);
            strLog += "\r\n";
        }
        tvUartRead.setText(strLog);
        */
        //saveLog(str+"\r\n");

    }

    private void saveUri(String strUri){
        String strPathFold	= MainActivity.this.getFilesDir().getAbsolutePath()+"/";
        File file = new File(strPathFold,strFileName);
        try {
            if(file.exists()){
                file.delete();
            }
            file.createNewFile();

            BufferedWriter write  = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file,true),"Unicode"));
            write.append(strUri);
            write.flush();
            write.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private String readUri(){
        String strUri = "";

        String strPathFold	= MainActivity.this.getFilesDir().getAbsolutePath()+"/";
        File file = new File(strPathFold,strFileName);
        try {
            if(file.exists()){
                BufferedReader reader  = new BufferedReader(new InputStreamReader(new FileInputStream(file),"Unicode"));
                strUri =  reader.readLine();
                reader.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return strUri;
    }

    private void exitApp() {
        Handler mtimer = new Handler();
        Runnable mrunner=new Runnable() {
            @Override
            public void run() {
                try {
                    finish();
                } catch (Exception e) {
                    showMsg(new Object(){}.getClass().getEnclosingMethod().getName()+" . "+e.getMessage());
                }
            }
        };
        mtimer.postDelayed(mrunner,1000);
    }

    public byte[] getFontSize(int iWidth,int iHeight){

        byte[] cmd = new byte[] { 0x1D, 0x21, 0x00};

        int iVal 	= (iWidth-1)<<4;
        iVal		+= (iHeight-1);
        cmd[2]	= (byte)iVal;

        return cmd;
    }

    //endregion

    //region Activity Events

    public void onPause(){
        super.onPause();
        showLog("onPause bFlagFileSelect:+"+bFlagFileSelect);
        if(!bFlagFileSelect){
            if(bFlagPrinterOpen){
                //btnPrnClose.performClick();
                bFlagReOpenPrn  = true;
            }
            if(bFlagDrawerOpen){
                //btnDrawerClose.performClick();
                bFlagReOpenDrawer   = true;
            }
        }
        showLog("onPause end bFlagFileSelect:"+bFlagFileSelect);
    }

    public void onResume(){
        super.onResume();
        showLog("onResume bFlagFileSelect:"+bFlagFileSelect);
        if(!bFlagFileSelect){
            if(bFlagReOpenPrn){
                bFlagReOpenPrn  = false;
                btnPrnOpen.performClick();
            }
            if(bFlagReOpenDrawer){
                bFlagReOpenDrawer  = false;
                btnDrawerOpen.performClick();
            }
        }
        showLog("onResume end bFlagFileSelect:"+bFlagFileSelect);
    }

    public void onDestroy() {
        try {
            devPrn.closePrinter();
        } catch (Exception e) {
        }
        super.onDestroy();
    }

    //endregion

}
