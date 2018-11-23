package com.ndzl.rfid_android_ndzl;

import android.app.Activity;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.READER_EXCEPTION_EVENT_TYPE;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;
import java.util.ArrayList;

public class MainActivity extends Activity{
    public static Readers readers;
    private static ArrayList<ReaderDevice> availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    private static RFIDReader reader;
    private static String TAG = "DEMO";
    private EventHandler eventHandler;
    TextView textView;
    Button btDiscover;
    Button btConnect;
    Button btDisconnect;

    Button bt_50;
    Button bt_90;
    int min_rssi = -90;

    ToggleButton tbIL;
    boolean isLocate=false;

    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
// UI
        textView =  findViewById(R.id.TagText);
        btDiscover = findViewById(R.id.btDiscover);
        btDiscover.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View view) {
                                              Discover();
                                          }
                                      }
        );

        btConnect = findViewById(R.id.btConnect);
        btConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Connect();
            }
        });

        btDisconnect = findViewById(R.id.btDisc);
        btDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Disconnect();
            }
        });

        bt_50 = findViewById(R.id.bt50);
        bt_50.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                min_rssi=-50;
                PrintOnScreen("min RSSI set to -50dBm");
            }
        });

        bt_90 = findViewById(R.id.bt90);
        bt_90.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                min_rssi=-90;
                PrintOnScreen("min RSSI set to -90dBm");
            }
        });

        tbIL = findViewById(R.id.toggleIL);
        tbIL.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    isLocate=b;
                    if(b){
                        PrintOnScreen("-=INVENTORY MODE SELECTED=-");
                    }
                    else{
                        PrintOnScreen("-=LOCATE MODE SELECTED=-");
                    }
            }
        });

// SDK
        if (readers == null) {
            readers = new Readers(this, ENUM_TRANSPORT.SERVICE_SERIAL);
        }

    }

    void soundBeep() {
        toneG.startTone(ToneGenerator.TONE_CDMA_HIGH_SS, 50);
    }


    void Discover(){
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    if (readers != null) {
                        if (readers.GetAvailableRFIDReaderList() != null) {
                            availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                            if (availableRFIDReaderList.size() != 0) {
                                readerDevice = availableRFIDReaderList.get(0);
                                reader = readerDevice.getRFIDReader();
                                return true;
                            }
                        }
                    }
                } catch (InvalidUsageException e) {
                    e.printStackTrace();

                } /*catch (OperationFailureException e) {
                    e.printStackTrace();
                    Log.d(TAG, "OperationFailureException " + e.getVendorMessage());
                }*/
                return false;
            }
            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if (aBoolean) {
                    //Toast.makeText(getApplicationContext(), "Discovered reader ", Toast.LENGTH_LONG).show();
                    PrintOnScreen( "Discovered reader="+reader.getHostName() );
                }
            }
        }.execute();
    }

    void Connect(){
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {

                    if (!reader.isConnected()) {
// Establish connection to the RFID Reader
                        reader.connect();
                        ConfigureReader(isLocate);
                        return true;
                    }

                } catch (InvalidUsageException e) {
                    e.printStackTrace();
                } catch (OperationFailureException e) {
                    e.printStackTrace();
                    Log.d(TAG, "OperationFailureException " + e.getVendorMessage());
                }
                return false;
            }
            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if (aBoolean) {
                    PrintOnScreen( "Reader Connected" );
                }
            }
        }.execute();
    }

    void Disconnect(){
        try {
            if (reader != null) {
                reader.Events.removeEventsListener(eventHandler);
                reader.disconnect();
                reader.Actions.reset();

                PrintOnScreen("Reader disconnected");

                reader = null;
                readers.Dispose();
                readers = null;
            }
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ConfigureReader(boolean singulate) {
        if (reader.isConnected()) {
            TriggerInfo triggerInfo = new TriggerInfo();
            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
            triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
            try {
                // receive events from reader
                if (eventHandler == null)
                    eventHandler = new EventHandler();
                reader.Events.addEventsListener(eventHandler);
                // HH event
                reader.Events.setHandheldEvent(true);
                // tag event w  08 ith tag data
                reader.Events.setTagReadEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(true);
                // set trigger mode as rfid so scanner beam will not come
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);

                reader.Config.setUniqueTagReport(singulate);

                // set sta    rt and stop triggers
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Disconnect();
    }

    String toBePrinted = "x";
    void PrintOnScreen(String _s){
        toBePrinted = _s;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String _old = textView.getText().toString();
                textView.setText(toBePrinted+"\n"+_old);
            }
        });
    }


    // Read/Status Notify handler
// Implement the RfidEventsLister class to receive event notifications
    public class EventHandler implements RfidEventsListener {
        // Read Event Notification
        public void eventReadNotify(RfidReadEvents e) {
            // Recommended to use new method getReadTagsEx for better performance
            // in case of large tag population
            TagData[] myTags = reader.Actions.getReadTags(100);
            TagData td = e.getReadEventData().tagData;

            StringBuilder _sb = new StringBuilder();
            _sb.append("ID="+td.getTagID());
            try { _sb.append("\tDISTANCE="+td.LocationInfo.getRelativeDistance()); } catch (Exception e1) { e1.printStackTrace(); }
            try { _sb.append("\tRSSI="+td.getPeakRSSI()); } catch (Exception e1) { e1.printStackTrace(); }

            if(td.getPeakRSSI()>=min_rssi) {
                soundBeep();
                PrintOnScreen(_sb.toString());
            }

            /*
            if (myTags != null) {
                PrintOnScreen("tags read: "+myTags.length);
                for (int index = 0; index < myTags.length; index++) {
                    Log.d(TAG, "Tag ID " + myTags[index].getTagID());

                    PrintOnScreen("<"+myTags[index].getTagID()+">");

                    if (myTags[index].getOpCode() ==
                            ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ &&
                            myTags[index].getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
                        if (myTags[index].getMemoryBankData().length() > 0) {
                            Log.d(TAG, " Mem Bank Data " + myTags[index].getMemoryBankData());
                        }
                    }
                }
            }
            */
        }
        // Status Event Notification
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            Log.d(TAG, "Status Notification: " +
                    rfidStatusEvents.StatusEventData.getStatusEventType());
            if (rfidStatusEvents.StatusEventData.getStatusEventType() ==
                    STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if(rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() ==
                        HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    PrintOnScreen("trigger pressed");
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            try {
                                reader.Actions.Inventory.perform();
                            } catch (InvalidUsageException e) {
                                e.printStackTrace();
                            } catch (OperationFailureException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    }.execute();
                }

                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() ==
                        HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
                    PrintOnScreen("trigger released");
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            try {
                                reader.Actions.Inventory.stop();
                            } catch (InvalidUsageException e) {
                                e.printStackTrace();
                            } catch (OperationFailureException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    }.execute();
                }
            }
        }
    }
}