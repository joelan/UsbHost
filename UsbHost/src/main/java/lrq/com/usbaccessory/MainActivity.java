package lrq.com.usbaccessory;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements Runnable{
    private static final String ACTION_USB_PERMISSION ="lrq.com.usbaccessory.USB_PERMISSION";
    private UsbManager manager;
    static final  String GLOBAL_CONTEXT="MainActivity";
    private ParcelFileDescriptor mFileDescriptor;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;
    private PendingIntent mPermissionIntent;
    UsbAccessory accessory;
    private UsbDeviceConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
         manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);


        UsbDevice device = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if(device!=null)
        {
            opendevice(device);
        }


        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


              /*  UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                UsbAccessory[] accessoryList = manager.getAccessoryList();
                if(accessoryList!=null&&accessoryList.length>0)
                  manager.requestPermission(accessoryList[0], mPermissionIntent);
                else
                {
                    Toast.makeText(MainActivity.this,"this is no accessory",Toast.LENGTH_SHORT).show();
                }*/
                UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

                HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                Log.i("MainActivity", "get device list  = " + deviceList.size());
                Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
                UsbDevice device = null;
                while(deviceIterator.hasNext()){
                  device = deviceIterator.next();
                    //your code

                }
                if(device!=null)
                manager.requestPermission(device, mPermissionIntent);
                else
                {
                    Toast.makeText(MainActivity.this,"device is null",Toast.LENGTH_SHORT).show();
                }



            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mUsbReceiver);

    }

    /***********USB broadcast receiver*******************************************/
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action))
            {
                synchronized (this)
                {
                    //mPermissionRequestPending = false;
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            opendevice(device);
                        }
                    }


                }
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    // call your method that cleans up and closes communication with the device

                    closeConnectiontoDedeive();


                }
            }else{
                Log.d("LED", "....");
            }
        }
    };

    private void closeConnectiontoDedeive() {
        if(connection!=null)
        connection.close();
        if(endpoint!=null)
            endpoint=null;

        bytes=null;
        TIMEOUT = 0;
        forceClaim = true;
    }

    private void openAccessory(UsbAccessory accessory) {
        Log.d("openAccessory", "openAccessory: " + accessory);
        mFileDescriptor = manager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);

            Thread thread = new Thread(null, this, "AccessoryThread");
            thread.start();
        }
    }
    private byte[] bytes;
    private static int TIMEOUT = 0;
    private boolean forceClaim = true;
    UsbEndpoint endpoint;
    private  void opendevice(UsbDevice device)
    {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbInterface intf = device.getInterface(0);
         endpoint = intf.getEndpoint(0);
         connection = manager.openDevice(device);
        connection.claimInterface(intf, forceClaim);

       //do in another thread
        Thread mthread=new Thread(this);
        mthread.start();

    }


    @Override
    public void run() {
       /* bytes=new byte[10*1024];*/
        connection.bulkTransfer(endpoint, bytes, bytes.length, TIMEOUT);
     Log.i("run","this is thread running");

    }
}
