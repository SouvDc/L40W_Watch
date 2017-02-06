package bule.souv.com.l40w_watch;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 描述：
 * 作者：dc on 2016/9/28 09:24
 * 邮箱：597210600@qq.com
 */
public class BluetoothHelper {
    private static final String TAG = BluetoothHelper.class.getSimpleName();

    private Context context = null;

    /**
     * 对象定义
     **/
    //蓝牙管理类
    private BluetoothManager bluetoothManager = null;
    //蓝牙适配器：BluetoothAdapter是所有蓝牙交互的入口。使用这个类，你能够发现其他的蓝牙设备，查询已配对设备的列表
    private BluetoothAdapter bluetoothAdapter = null;
    private List<BluetoothDevice> bluetoothDeviceList = null;
    private SearchBooltherListener searchBooltherListener = null;

    /**
     * 属性定义
     **/
    private String lastBlueName = ""; //当前蓝牙名称，去重
    private boolean isSearch = false; //是否搜索到血压设备。
    public static String searchBlueName = ""; //需要搜索的的蓝牙名称

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public BluetoothHelper(Context ctx){
        context = ctx;
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        bluetoothDeviceList = new ArrayList<>();
    }
    /**
     * @descriptoin 检查设备是否支持蓝牙4.0
     * @author dc
     * @date 2016/9/8 15:23
     */
    public boolean TestDevicesSuportBlue4() {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return true;
        }
        return false;
    }

    /**
     * @descriptoin 检测蓝牙是否打开
     * @param
     * @return
     * @author
     * @date 2016/9/8 15:53
     */
    public boolean getBlueStatus() {
        if (bluetoothAdapter.isEnabled()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param status true:打开蓝牙   false:关闭蓝牙
     * @descriptoin 设置蓝牙状态
     * @author dc
     * @date 2016/9/8 16:00
     */
    public void setBlueStatus(boolean status) {
        if (!status) {
            //关闭蓝牙
            bluetoothAdapter.disable();
        } else {
            //打开蓝牙
            bluetoothAdapter.enable();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void scanLeDevice(final boolean enable) {
        if (enable) {
            bluetoothAdapter.startLeScan(mLeScanCallback); //开始搜索
        } else {
            if(!isSearch){
                //表示没有搜索到血压设备
                searchBooltherListener.searchBoolFail();
            }
            bluetoothAdapter.stopLeScan(mLeScanCallback);//停止搜索
        }
    }


    public void blueCancelDiscovery(){
        bluetoothAdapter.cancelDiscovery();
    }

    /**
     * @descriptoin 搜索蓝牙设备回调方法
     * @author dc
     * @date 2016/9/8 16:15
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            bluetoothDeviceList.add(device);
            Log.e(TAG,"搜索到的设备=" + device.getName());
            if(device.getName().equals(searchBlueName)){
                /**  2：搜索蓝牙设备，如果查到血压蓝牙设备之后立刻停止搜索并将血压设备device传入参数作为连接 **/
                isSearch = true;
                scanLeDevice(false);
                searchBooltherListener.searchBoolListData(device);
            }
        }
    };



    public void setSearchBooltherListener(SearchBooltherListener list){
        searchBooltherListener = list;
    }

    /**
     * 查找已配对的蓝牙设备
     */
    public Set<BluetoothDevice> getLocaltionDevices(){
        Set<BluetoothDevice> pairedDevices= bluetoothAdapter .getBondedDevices();
        if(pairedDevices.size() > 0){
            for (BluetoothDevice bluetoothDevice : pairedDevices) {
//                Log.e(TAG,"已配对的蓝牙设备name=" + bluetoothDevice.getName());
            }
        }
        return  pairedDevices;
    }

    /**
     * 搜索指令
     */
    public interface SearchBooltherListener{
        void searchBoolListData(BluetoothDevice d);
        void searchBoolFail();
    }
}
