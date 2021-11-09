package aclasprn.dts;

import android.util.Log;

import CommDevice.USBPort;
import aclasdriver.AclasDevice;
import aclasdriver.AclasFactory;

public class AclasDrawer {
    private final String tag                    = "AclasDrawer";
    private AclasDevice devDrawer                   = null;

    public AclasDrawer(){
        devDrawer   = AclasFactory.getInstance().GetAclasDevice(AclasFactory.DEV_DRAWER);
    }

    public boolean openDrawer(){
        boolean bFlag = false;
        String str = USBPort.getDeviceName(0);
        bFlag = devDrawer.AclasOpen(str)>=0;
        Log.d(tag,"openDrawer:"+str+" "+bFlag);
        return bFlag;
    }

    public void closeDrawer(){
        devDrawer.AclasClose();
    }

    /**
     * The action of open drawer.
     */
    public void doOpen(int iIndex){
        byte[] bData = new byte[]{(byte)iIndex};
        devDrawer.AclasWrite(bData,1,AclasDevice.DRAWER_OPEN);
    }
}