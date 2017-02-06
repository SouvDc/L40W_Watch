package bule.souv.com.l40w_watch;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

import bule.souv.com.l40w_watch.bean.DataTypeBean;
import bule.souv.com.l40w_watch.utils.ByteUtil;
import bule.souv.com.l40w_watch.utils.DateTool;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class Blue2MainActivity extends AppCompatActivity implements View.OnClickListener, BluetoothHelper.SearchBooltherListener {
    private String TAG = Blue2MainActivity.class.getSimpleName();

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.activity_blue2_scan_watch_btn)
    Button activityBlue2ScanWatchBtn;
    @Bind(R.id.activity_blue2_disconnect_watch_btn)
    Button activityBlue2DisconnectWatchBtn;
    @Bind(R.id.activity_blue2_query_deviceid_btn)
    Button activityBlue2QueryDeviceidBtn;
    @Bind(R.id.activity_blue2_query_step_btn)
    Button activityBlue2QueryStepBtn;
    @Bind(R.id.activity_blue2_set_time_btn)
    Button activityBlue2SetTimeBtn;
    @Bind(R.id.fab)
    FloatingActionButton fab;
    @Bind(R.id.activity_blue2_query_display_sport_btn)
    Button activityBlue2QueryDisplaySportBtn;

    /**
     * 对象定义
     **/
    private BluetoothAdapter mBtAdapter = null;
    private static BluetoothHelper bluetoothHelper = null;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;


    private StartMesaureThread startMeasureThread = null; //正在测量线程
    private SendDataTimeoutThread sendDataTimeoutThread = null; //数据上传超时线程
    /**
     * 属性定义
     */
    private static final long SCAN_PERIOD = 30000; //10 seconds
    private boolean mScanning;
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected = false; //是否连接成功

    private String SERVICE_UUID = ""; //6606的uuid
    private String CHAR8001_UUID = "";
    private String CHAR8002_UUID = "";

    private int totaltodaySportData = 0; //每次需要获取的的运动数据的总数
    private long totalStep = 0; //总共的跑步数
    private long totalColirie = 0; //总共的卡路里
    private long totalDistance = 0; //总共的距离
    private long totalSportTime = 0; //总共的运动时间

    private int sendDataTime = 0;//数据上传超时时间
    private boolean isConnectTimeout = false; //连接超时操作
    private boolean isDataSendTimeout = false; //数据上传超时操作

    private Handler handler = new Handler() {
        @Override
        public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
            return super.sendMessageAtTime(msg, uptimeMillis);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue2_main);
        ButterKnife.bind(this);

        init();

        initBluetooth();

        search("ZeWatch4 HR#00068");
    }

    private void init() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * @descriptoin 初始化蓝牙血压设备
     * @author dc
     * @date 2016/10/5 14:28
     */
    private void initBluetooth() {
        bluetoothHelper = new BluetoothHelper(Blue2MainActivity.this);
        bluetoothHelper.setSearchBooltherListener(this);//注册搜索血压蓝牙设备接口
        //判断设备是否支持蓝牙4.0
        if (!bluetoothHelper.TestDevicesSuportBlue4()) {
            Toast.makeText(Blue2MainActivity.this, "该设备不支持蓝牙4.0设备", Toast.LENGTH_SHORT).show();
            finish();
        }
        /**  1：根据蓝牙状态打开蓝牙设备 **/
        //获取蓝牙状态
        if (!bluetoothHelper.getBlueStatus()) {
            //打开蓝牙
            bluetoothHelper.setBlueStatus(true);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @OnClick({R.id.activity_blue2_scan_watch_btn, R.id.activity_blue2_disconnect_watch_btn, R.id.activity_blue2_query_deviceid_btn,
            R.id.activity_blue2_query_step_btn, R.id.activity_blue2_set_time_btn,R.id.activity_blue2_query_display_sport_btn})
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.activity_blue2_scan_watch_btn:
                search("ZeWatch4 HR#00068");
                break;
            case R.id.activity_blue2_query_step_btn:
                sendQuerySportData();
                break;
            case R.id.activity_blue2_query_display_sport_btn:
                queryDisplaySportData();
                break;
        }
    }

    /**
     * @descriptoin 搜索蓝牙
     * @author dc
     * @date 2016/9/29 14:54
     */
    private void search(String searchName) {
        BluetoothHelper.searchBlueName = searchName;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //
                mScanning = false;
                Log.e(TAG, "停止搜索");
                bluetoothHelper.scanLeDevice(false);
            }
        }, SCAN_PERIOD);
        Log.e(TAG, "开始搜索");
        mScanning = true;
        bluetoothHelper.scanLeDevice(true);
    }


    @Override
    public void searchBoolListData(BluetoothDevice d) {
        //搜索蓝牙成功
        Log.e(TAG, "name     " + d.getName() + "   address=" + d.getAddress());
        mDeviceName = d.getName();
        mDeviceAddress = d.getAddress();

        //连接
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }


    @Override
    public void searchBoolFail() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.e(TAG, "Connect request result=" + result);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService.close();
        mBluetoothLeService = null;

    }

    /**
     * ------------------------------- 蓝牙开发    ------------------------------
     **/

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            String newUuid = uuid.split("-")[0].substring(4, uuid.split("-")[0].length());
            if (newUuid.equalsIgnoreCase("6006")) {
                SERVICE_UUID = uuid;
                Log.e(TAG, "SERVICE_UUID=" + SERVICE_UUID);

                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();

                Log.e(TAG, "gattCharacteristics is size = " + gattCharacteristics.size());
                if (gattService.getCharacteristics().size() > 0) {
                    // Loops through available Characteristics.
                    String charUuid = null;
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        charUuid = gattCharacteristic.getUuid().toString();
                        String newCharUuid = charUuid.split("-")[0].substring(4, charUuid.split("-")[0].length());
                        if (newCharUuid.equalsIgnoreCase("8001")) {
                            CHAR8001_UUID = charUuid;
                            Log.e(TAG, "CHAR8001_UUID=" + CHAR8001_UUID);
                        }
                        if (newCharUuid.equalsIgnoreCase("8002")) {
                            CHAR8002_UUID = charUuid;
                            Log.e(TAG, "CHAR8002_UUID=" + CHAR8002_UUID);
                        }
                    }
//                    sendQuerySportData();
                    queryDisplaySportData();
                } else {
                    Log.e(TAG, "gattService.getCharacteristics 异常");
                }
                break;
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.e(TAG, "action = " + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Log.e(TAG, "连接状态=" + mConnected);
                //计时，如果10s之内没有数据，就结束所有工作
                //计时测量时间
                startMeasureThread = new StartMesaureThread();
                startMeasureThread.start();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                Log.e(TAG, "连接状态=" + mConnected);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //获取数据
                DataTypeBean dataTypeBean = (DataTypeBean) intent.getSerializableExtra(BluetoothLeService.EXTRA_DATA);
                byte[] dataByte = dataTypeBean.getDataBuffer();//intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                //打印数据
                final StringBuilder stringBuilder = new StringBuilder(dataByte.length);
                for (byte byteChar : dataByte)
                    stringBuilder.append(String.format("%02X ", byteChar));
                Log.e(TAG, "------" + stringBuilder.toString() + "  type = " + dataTypeBean.getDataType());

                //获取数据长度
                int len = (dataByte[4] << 8) | dataByte[3];
//                Log.e(TAG, "数据长度=" + len);
                //获取整条数据的内容
                byte[] contentByte = new byte[len];
                System.arraycopy(dataByte, (Constan.MINDATA) - 1, contentByte, 0, len);

                //判断数据类型
                int dataType = dataTypeBean.getDataType();
                if (0 == dataType) {
                    //查询设备id
                } else if (1 == dataType) {
                    //设置手环时间
                } else if (2 == dataType) {
                    isConnectTimeout = true;

                    sendDataTime = 0;
                    //启动数据上传超时线程
                    if (null == sendDataTimeoutThread) {
                        sendDataTimeoutThread = new SendDataTimeoutThread();
                        sendDataTimeoutThread.start();
                    }


                    // 获取运动信息
                    byte[] dateByte = new byte[4]; // 时间
                    byte[] stepByte = new byte[4]; //步数
                    byte[] calorieByte = new byte[4];  //卡路里
                    byte[] distanceByte = new byte[4];  //距离
                    byte[] sportByte = new byte[4];  //运动时间

                    System.arraycopy(contentByte, 2, dateByte, 0, dateByte.length);
                    System.arraycopy(contentByte, 6, stepByte, 0, stepByte.length);
                    System.arraycopy(contentByte, 10, calorieByte, 0, calorieByte.length);
                    System.arraycopy(contentByte, 14, distanceByte, 0, distanceByte.length);
                    System.arraycopy(contentByte, 18, sportByte, 0, sportByte.length);

                    long dateStr = ByteUtil.byteToLong(dateByte);
                    long stepStr = ByteUtil.byteToLong(stepByte);
                    long colirieStr = ByteUtil.byteToLong(calorieByte);
                    long distanceStr = ByteUtil.byteToLong(distanceByte);
                    long sportStr = ByteUtil.byteToLong(sportByte);

                    String timeStr = DateTool.TimeStamp2Date(dateStr + "");
                    Log.e(TAG, "时间=" + timeStr + "  步数=" + totalStep + "   卡路里=" + totalColirie + "  距离=" + totalDistance + "  运动时间=" + totalSportTime);
                    //将今天的数据统计出来并相加
                    if (DateTool.isOnTheToDay(timeStr)) {
                        Log.e(TAG, "是当天");
                        totalStep = stepStr;
                        totalColirie = colirieStr;
                        totalDistance = distanceStr;
                        totalSportTime = sportStr;
                    } else {
                        Log.e(TAG, "不是当天");
                    }
                    int index = (int) contentByte[0];
                    Log.e(TAG, "当前数据index = " + index);
                    //判断数据总数是否与当前数量一致，如果一致证明数据已上传完毕
                    if (totaltodaySportData == index) {
//                        Log.e(TAG,"运动数据全部上传成功");
                        Log.e(TAG, "时间=" + timeStr + "  步数总数=" + totalStep + "   卡路里总数=" + totalColirie + "  距离总数=" + totalDistance + "  运动时间总数=" + totalSportTime);
                        //删除设备端运动记录
                        sendDeleteWatchSportRecord();
                        //停止超时线程
                        sendDataTime = 0;
                        isDataSendTimeout = true;

                    }
                } else if (3 == dataType) {
                    //基本响应  6F 01 80 02 00 54 01 8F
                    //如果基本相应返回的是失败，则判断是那条指令失败
                    if (contentByte[1] == 0x01) {
                        if (contentByte[0] == Constan.WATCHID) {
                            Log.e(TAG, "获取watchId数据失败");
                        } else if (contentByte[0] == Constan.WATCHTIME) {
                            Log.e(TAG, "设置时间失败");
                        } else if (contentByte[0] == Constan.WATCHSPORT) {
                            Log.e(TAG, "获取运动数据失败");
                        } else if (contentByte[0] == Constan.DELECTWATCHSPORTRECORD) {
                            Log.e(TAG, "删除运动数据失败");
                            //发送设置设备日期时间
                            sendSetWatchTimeData();
                        }
                    } else if (contentByte[1] == 0x00) {
                        if (contentByte[0] == Constan.WATCHTIME) {
                            Log.e(TAG, "设置时间成功");
                        } else if (contentByte[0] == Constan.DELECTWATCHSPORTRECORD) {
                            Log.e(TAG, "删除运动数据成功");
                            //发送设置设备日期时间
                            sendSetWatchTimeData();
                        }
                    }
                } else if (4 == dataType) {
                    if (dataByte[2] == Constan.WATCHBREAKBYTE) {
                        //数据数据总数
                        byte[] sportCountDataByte = new byte[4];  //运动数据总数
                        System.arraycopy(contentByte, 0, sportCountDataByte, 0, sportCountDataByte.length);
                        totaltodaySportData = (int) ByteUtil.byteToLong(sportCountDataByte);
                        Log.e(TAG, "数据总数=" + totaltodaySportData);
                        if (totaltodaySportData == 0) {
                            //提示用户当前数据为0
                            Log.e(TAG, "暂无数据");
                            //发送设置设备日期时间
                            sendSetWatchTimeData();
                        } else {
                            //发送请求步数数据
                            sendQueryWatchSportData();
                        }
                    }
                } else if(5 == dataType){
                    // 获取显示运动数据
                    byte[] stepByte = new byte[4]; //步数
                    byte[] calorieByte = new byte[4];  //卡路里
                    byte[] sleepDateByte = new byte[4]; // 时间
                    byte[] distanceByte = new byte[4];  //距离
                    byte[] sportByte = new byte[4];  //运动时间

                    System.arraycopy(contentByte, 0, stepByte, 0, stepByte.length);
                    System.arraycopy(contentByte, 4, calorieByte, 0, calorieByte.length);
                    System.arraycopy(contentByte, 8, distanceByte, 0, distanceByte.length);
                    System.arraycopy(contentByte, 12, sleepDateByte, 0, sleepDateByte.length);
                    System.arraycopy(contentByte, 16, sportByte, 0, sportByte.length);

                    long dateStr = ByteUtil.byteToLong(sleepDateByte);
                    long stepStr = ByteUtil.byteToLong(stepByte);
                    long colirieStr = ByteUtil.byteToLong(calorieByte);
                    long distanceStr = ByteUtil.byteToLong(distanceByte);
                    long sportStr = ByteUtil.byteToLong(sportByte);

                    String timeStr = DateTool.TimeStamp2Date(dateStr + "");
                    Log.e(TAG, "时间=" + timeStr + "  步数=" + stepStr + "   卡路里=" + colirieStr + "  距离=" + distanceStr + "  运动时间=" + sportStr);
                }
            }
        }
    };


    /** ------------------------------------  方法体 -------------------------- **/
    /**
     * @param byteInfo 数据内容
     * @descriptoin 向手环发送数据
     * @author dc
     * @date 2016/12/29 15:18
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void sendWatchData(BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] byteInfo) {
        bluetoothGattCharacteristic.setValue(byteInfo);
        mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristic);  //写入数据
    }

    /**
     * @param notifCharacteristic 8002发送描述到设备的体征
     * @return
     * @descriptoin 往8002发送03，并启动通知
     * @author dc
     * @date 2016/12/29 15:14
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void sendNotificationData(final BluetoothGattCharacteristic notifCharacteristic) {
        byte[] data = new byte[1];  //查询设备信息
        data[0] = 0x03;
        notifCharacteristic.setValue(data);
        mBluetoothLeService.writeCharacteristic(notifCharacteristic);  //写入数据
    }

    /**
     * @descriptoin 发送请求运动数据:因请求前需要获取运动数据总数才能保持数据到手环
     * @author dc
     * @date 2016/12/30 10:58
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void sendQueryWatchSportData() {
        BluetoothGattService stepService = mBluetoothLeService.writeServices(SERVICE_UUID);
        BluetoothGattCharacteristic stepCharacteristic = stepService.getCharacteristic(UUID.fromString(CHAR8001_UUID));
        final BluetoothGattCharacteristic notifCharacteristic = stepService.getCharacteristic(UUID.fromString(CHAR8002_UUID));
        byte[] stepData = new byte[Constan.MINDATA + 2];
        stepData[0] = Constan.STARTBYTE;
        stepData[1] = Constan.WATCHSPORT;
        stepData[2] = Constan.CLIENTQUERYBYTE;
        stepData[3] = 0x02;
        stepData[4] = 0x00;
        stepData[5] = 0x00;
        stepData[6] = 0x00;
        stepData[7] = Constan.ENDBYTE;
        sendWatchData(stepCharacteristic, stepData);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendNotificationData(notifCharacteristic);
            }
        }, 500);
    }

    /**
     * @descriptoin 设置设备系统时间
     * @author dc
     * @date 2017/1/5 19:25
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void sendSetWatchTimeData() {
        //获取当前时间
        String currentTime = DateTool.getSystemTime();
        int year = Integer.valueOf(currentTime.split(" ")[0].toString().split("-")[0]);
        int month = Integer.valueOf(currentTime.split(" ")[0].toString().split("-")[1]);
        int day = Integer.valueOf(currentTime.split(" ")[0].toString().split("-")[2]);

        int hour = Integer.valueOf(currentTime.split(" ")[1].toString().split(":")[0]);
        int minute = Integer.valueOf(currentTime.split(" ")[1].toString().split(":")[1]);
        int second = Integer.valueOf(currentTime.split(" ")[1].toString().split(":")[2]);
        Log.e(TAG, year + "年" + month + "月" + day + "日" + hour + "时" + minute + "分" + second + "秒");

        byte[] lb = new byte[2];
        lb[0] = (byte) ((year >> 8) & 0xFF);
        lb[1] = (byte) year;

        BluetoothGattService setTimeService = mBluetoothLeService.writeServices(SERVICE_UUID);
        BluetoothGattCharacteristic setTimeCharacteristic = setTimeService.getCharacteristic(UUID.fromString(CHAR8001_UUID));
        final BluetoothGattCharacteristic notifCharacteristic = setTimeService.getCharacteristic(UUID.fromString(CHAR8002_UUID));
        byte[] setTimeByte = new byte[(Constan.MINDATA) + 7];  //设置手环时间信息
        setTimeByte[0] = Constan.STARTBYTE;
        setTimeByte[1] = Constan.WATCHTIME;
        setTimeByte[2] = Constan.CLIENTSETBYTE;
        setTimeByte[3] = 0x07;
        setTimeByte[4] = 0x00;
        setTimeByte[5] = lb[1];
        setTimeByte[6] = lb[0];
        setTimeByte[7] = (byte) month;
        setTimeByte[8] = (byte) day;
        setTimeByte[9] = (byte) hour;
        setTimeByte[10] = (byte) minute;
        setTimeByte[11] = (byte) second;
        setTimeByte[12] = Constan.ENDBYTE;
        sendWatchData(setTimeCharacteristic, setTimeByte);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendNotificationData(notifCharacteristic);
            }
        }, 500);
    }

    /**
     * @descriptoin 获取手环运动数据
     * @author dc
     * @date 2017/1/5 19:32
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void sendQuerySportData() {
        totaltodaySportData = 0; //每次需要获取的的运动数据的总数
        totalStep = 0; //总共的跑步数
        totalColirie = 0; //总共的卡路里
        totalDistance = 0; //总共的距离
        totalSportTime = 0; //总共的运动时间
        isConnectTimeout = false; //超时状态

        //发送“获取运动条数”
        BluetoothGattService getCountDataService = mBluetoothLeService.writeServices(SERVICE_UUID);
        final BluetoothGattCharacteristic getCountCharacteristic = getCountDataService.getCharacteristic(UUID.fromString(CHAR8001_UUID));
        final BluetoothGattCharacteristic notifCharacteristic = getCountDataService.getCharacteristic(UUID.fromString(CHAR8002_UUID));

        mBluetoothLeService.writeBluetoothGattDescriptor(notifCharacteristic);

        //获取当天运动总数据
        long currentUnixTime = DateTool.unixTimeStamp();//获取当天unix时间戳

        //得到当16进制时间戳
        String curUnixTime = Integer.toHexString((int) currentUnixTime);//ByteUtil.hexStrToByte(currentUnixTime+"");
        byte[] curUnixTimeByte = ByteUtil.hexStrToByte(curUnixTime);
        Log.e(TAG, "当前时间戳 = " + curUnixTime);

        byte[] getCountData = new byte[Constan.MINDATA + curUnixTimeByte.length + 1];
        getCountData[0] = Constan.STARTBYTE;
        getCountData[1] = Constan.WATCHSPORTDATACOUNT;
        getCountData[2] = Constan.CLIENTQUERYBYTE;
        getCountData[3] = (byte) (curUnixTimeByte.length + 1);
        getCountData[4] = 0x00;
        getCountData[5] = 0x01;
        for (int j = 0; j <= (curUnixTimeByte.length) - 1; j++) {
            getCountData[6 + j] = curUnixTimeByte[(curUnixTimeByte.length) - (j + 1)];
        }

        getCountData[getCountData.length - 1] = Constan.ENDBYTE;
        sendWatchData(getCountCharacteristic, getCountData);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendNotificationData(notifCharacteristic);
            }
        }, 500);

        //打印数据
        final StringBuilder stringBuilder = new StringBuilder(getCountData.length);
        for (byte byteChar : getCountData)
            stringBuilder.append(String.format("%02X ", byteChar));
        Log.e(TAG, "当前时间戳byte = " + stringBuilder.toString());
    }

    /**
     * @descriptoin 删除设备端运动记录
     * @author dc
     * @date 2017/1/12 10:09
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void sendDeleteWatchSportRecord() {
        BluetoothGattService stepService = mBluetoothLeService.writeServices(SERVICE_UUID);
        BluetoothGattCharacteristic delectSportRecordCharacteristic = stepService.getCharacteristic(UUID.fromString(CHAR8001_UUID));
        final BluetoothGattCharacteristic notifCharacteristic = stepService.getCharacteristic(UUID.fromString(CHAR8002_UUID));
        byte[] delectSportRecordData = new byte[Constan.MINDATA + 1];
        delectSportRecordData[0] = Constan.STARTBYTE;
        delectSportRecordData[1] = Constan.DELECTWATCHSPORTRECORD;
        delectSportRecordData[2] = Constan.CLIENTSETBYTE;
        delectSportRecordData[3] = 0x01;
        delectSportRecordData[4] = 0x00;
        delectSportRecordData[5] = 0x00;
        delectSportRecordData[6] = Constan.ENDBYTE;
        sendWatchData(delectSportRecordCharacteristic, delectSportRecordData);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendNotificationData(notifCharacteristic);
            }
        }, 500);
    }

    /**
     * @descriptoin	获取手表显示运动数据
     * @author	dc
     * @date 2017/1/12 14:36
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void queryDisplaySportData(){
        BluetoothGattService stepService = mBluetoothLeService.writeServices(SERVICE_UUID);
        BluetoothGattCharacteristic delectSportRecordCharacteristic = stepService.getCharacteristic(UUID.fromString(CHAR8001_UUID));
        final BluetoothGattCharacteristic notifCharacteristic = stepService.getCharacteristic(UUID.fromString(CHAR8002_UUID));
        mBluetoothLeService.writeBluetoothGattDescriptor(notifCharacteristic);

        byte[] delectSportRecordData = new byte[Constan.MINDATA + 1];
        delectSportRecordData[0] = Constan.STARTBYTE;
        delectSportRecordData[1] = Constan.WATCHDISPLAYSPORTDATA;
        delectSportRecordData[2] = Constan.CLIENTQUERYBYTE;
        delectSportRecordData[3] = 0x01;
        delectSportRecordData[4] = 0x00;
        delectSportRecordData[5] = 0x00;
        delectSportRecordData[6] = Constan.ENDBYTE;
        sendWatchData(delectSportRecordCharacteristic, delectSportRecordData);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendNotificationData(notifCharacteristic);
            }
        }, 500);
    }


    /**
     * @descriptoin 测量倒计时线程
     * @author dc
     * @date 2016/12/21 16:58
     */
    private class StartMesaureThread extends Thread {
        @Override
        public void run() {
            super.run();
            int jTime = 0;
            while (jTime < 30) {
                if (!isConnectTimeout) {
                    try {
                        Thread.sleep(1000);//超时时间为10s
                        jTime++;
//                        Log.e(TAG, jTime + "-计时器");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else
                    return;
            }
            //发送结束测量
            Log.e(TAG, "停止所有工作");
//            finish();
        }
    }

    /**
     * @author dc
     * @descriptoin 超时判断数据上传是否结束
     * @date 2016/12/21 16:58
     */
    private class SendDataTimeoutThread extends Thread {
        @Override
        public void run() {
            super.run();

            while (sendDataTime < 20) {
                if (!isDataSendTimeout) {
                    try {
                        Thread.sleep(1000);//超时时间为10s
                        sendDataTime++;
                        Log.e(TAG, sendDataTime + "超时上次数据计时器");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    //停止超时操作
                    Log.e(TAG, "数据正确，停止超时操作");
                    return;
                }
            }
            Log.e(TAG, "上次数据超时，显示数据");
            Log.e(TAG, "时间=" + "  步数=" + totalStep + "   卡路里=" + totalColirie + "  距离=" + totalDistance + "  运动时间=" + totalSportTime);
            //发送设置设备日期时间
            sendSetWatchTimeData();
        }
    }
}
