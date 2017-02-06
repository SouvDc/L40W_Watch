/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bule.souv.com.l40w_watch;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import bule.souv.com.l40w_watch.bean.DataTypeBean;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@SuppressLint("NewApi")
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED           = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED        = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE           = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA                      = "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID UUID_HEART_RATE_MEASUREMENT       = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    private byte[] watchByte = new byte[1024]; //接收的数据
    private int unDisposeLen = 0; //未处理完的数据长度
    boolean isSend = false; //是否发送

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) { ////当连接上设备或者失去连接时会回调该函数
                //连接成功
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.e(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.e(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());  //发现服务会调用onServicesDiscovered()方法

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.e(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {  //当设备是否找到服务时，会回调该函数
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //找到服务
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                List<BluetoothGattService> gattServices = getSupportedGattServices();
                Log.e(TAG,"service size = " + gattServices.size());
                for (BluetoothGattService gattService : gattServices) {
                    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                    Log.e(TAG,"gattCharacteristics size = " + gattCharacteristics.size());
                    for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        Log.e(TAG, "---->char uuid:" + gattCharacteristic.getUuid());
                    }
                }
                //可通过getSupportedGattServices()方法获取服务列表
            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            Log.e(TAG,"当读取设备时会回调该函数 " + status );
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {  //当向设备Descriptor中写数据时，会回调该函数
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.e(TAG,"收到BLE终端写入数据回调  " + status );
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) { //设备发出通知时会调用到该接口
            Log.e(TAG,"设备发出通知时会调用到该接口");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) { //当向Characteristic写数据时会回调该函数
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.e(TAG, "当向onDescriptorWrite写数据时会回调该函数  ");

        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
//            final StringBuilder stringBuilder = new StringBuilder(data.length);
//            for(byte byteChar : data)
//                stringBuilder.append(String.format("%02X ", byteChar));
//            Log.e(TAG,"------"  + stringBuilder.toString());

            if (data != null && data.length > 0) {
                System.arraycopy(data,0,watchByte,unDisposeLen,data.length);
                //判断最后一个字节是否为8f
                if(data[data.length-1] == Constan.ENDBYTE){
                    //得到数据长度
                    int dataLen = (watchByte[4] << 8) | watchByte[3];
                    //一条完整数据的长度
                    byte[] contentByte = new byte[dataLen + Constan.MINDATA];
                    //获取一条完整的数据保存到contentByte的buffer中
                    System.arraycopy(watchByte, 0, contentByte, 0, contentByte.length);
                    //先判断命令字
                    DataTypeBean dataTypeBean = new DataTypeBean();
                    if(watchByte[1] == Constan.DATARESPONSE || watchByte[1] == Constan.DELECTWATCHSPORTRECORD ){
                        dataTypeBean.setDataType(3);  //设置数据类型
                    } else if(watchByte[1] == Constan.WATCHID){
                        //查询设备id
                        dataTypeBean.setDataType(0);  //设置数据类型
                    } else if(watchByte[1] == Constan.WATCHTIME){
                        //设置手环时间
                        dataTypeBean.setDataType(1);
                    } else if(watchByte[1] == Constan.WATCHSPORT){
                        // 获取运动信息
                        dataTypeBean.setDataType(2);
                    } else if(watchByte[1] == Constan.WATCHSPORTDATACOUNT){
                        // 获取运动总数数据
                        dataTypeBean.setDataType(4);
                    } else if(watchByte[1] == Constan.WATCHDISPLAYSPORTDATA){
                        // 获取显示运动数据
                        dataTypeBean.setDataType(5);
                    }
                    dataTypeBean.setDataBuffer(contentByte);  //设置数据内容
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(EXTRA_DATA,dataTypeBean);
                    intent.putExtras(bundle);
                    //通过广播发送数据
                    sendBroadcast(intent);

                    //清空buffer
                    byte[] ret = new byte[dataLen + Constan.MINDATA];
                    System.arraycopy(ret,0,watchByte,0,dataLen + Constan.MINDATA);
                    unDisposeLen = 0;
                } else {
                    unDisposeLen = data.length;
                    return;
                }
            }
        }

    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG,"service绑定成功");
        return mBinder;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        Log.e(TAG,"service解绑成功");
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * @descriptoin	根据serviceUUid返回该uuid的BluetoothGattService对象
     * @author	dc
     * @param uuid uuid
     * @date 2017/1/5 15:37
     * @return BluetoothGattService
     */
    public BluetoothGattService writeServices(String uuid){
        BluetoothGattService service =mBluetoothGatt.getService(UUID.fromString(uuid));
        return service;
    }

    /**
     * @descriptoin	写数据到characteristic
     * @author	dc
     * @param
     * @date 2017/1/5 15:40
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(mBluetoothAdapter == null || mBluetoothGatt == null){
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic,true);

        byte byteInfo[] = characteristic.getValue();
        final StringBuilder stringBuilder = new StringBuilder(byteInfo.length);
        for(byte byteChar : byteInfo)
            stringBuilder.append(String.format("%02X ", byteChar));
        Log.e(TAG, "写入手环的数据：" + stringBuilder.toString());

        boolean isWeite =  mBluetoothGatt.writeCharacteristic(characteristic);
        if(!isWeite){
            Log.e(TAG,"写入失败" + characteristic.getUuid());
        }
    }

    /**
     * @descriptoin 写体征写描述
     * @author  dc
     * @param   characteristic 体征
     * @date 2017/1/5 15:42
     */

    public void writeBluetoothGattDescriptor(BluetoothGattCharacteristic characteristic) {
        mBluetoothGatt.setCharacteristicNotification(characteristic,true);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID
                .fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));

        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean iswrite = mBluetoothGatt.writeDescriptor(descriptor);
            if(!iswrite){
                Log.e(TAG,"写入BluetoothGattDescriptor失败");
            } else {
                Log.e(TAG,"写入BluetoothGattDescriptor成功");
            }
        }
    }


    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }


}
