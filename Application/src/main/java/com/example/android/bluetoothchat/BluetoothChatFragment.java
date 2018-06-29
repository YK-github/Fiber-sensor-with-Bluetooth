/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.bluetoothchat;

//基本情報

//追加した関数や機能
//通信の確立後、MESSAGE_STATE_CONNECTEDになる。その際のcaseに#stのコマンドを送るコードを追加

//Destroy()アクティビティ起動時、#spコマンドを送るコードを追加

//handleMessage関数
////sendMessage(String message)の関数に、送る文字列の最後に改行コード”\r\n”を追加


////readMessageを分割、数値に変換し、保存用フォーマットに整列する

////readMessageの関数に、文字列を受信した際の処理を追加
////画面に表示する、findview使ってpickuptextやsensorviewに対応する画面領域に表示
////計測開始時刻の同期を簡便にするため、スタートボタンを追加し、それが押された後にSDへの保存を開始する使用にした

//readmessageをsavedata(String str)にかけて保存する関数を追加
////#DAを先頭とする計測値が複数まとめて送信されてくる場合が多い、計測サンプリング数の設定を上げると生じる。そのため、各計測値毎(65字)に区切るコードを追加、計測時間分解能が向上。
////readmessageをStringData_Change_To_Double(String s)にかけ、フォーマットする

//public String StringData_Change_To_Double(String s)を追加
//引数として受け取った文字列を、カンマ毎に切り取る。各Chの値をdBに計算し、日時を追加しフォーマットして返す関数
////PhysicalData_calculated_from_dB(dBValue)の呼び出し

////ストック保存モードなら計算結果を渡す部分を追加

//SDカードにstrを追加保存する関数savedata(String str)を追加

//ストックモード（任意のスイングを保存するモード）の適用時間をカウントする関数を追加
////一定時間経過後にストックモード解除

//2013/6/3..改良済みバグ
//計測器のバグ：Bluetooth接続後に、電圧値がゆらぐ案件があり
//→平均値にランダムな変動が生じる可能性あり

//2018.05.13
////Save_Dataを押したらSDカードに取得データの保存を開始する、saveFlagをtrueにする(アプリをリセットするまでFalseにはならない)
////Save_Textを押したらSDカードに入力した文字列を保存する
/// Startを押したら、monitorFlagをtrueにし、#stを送信する
/// Stopを押したら、monitorFlagをfalseにし、#spを送信する
/// Nbを押したら、#nbを送信する
///SENDは任意コマンド送信ボタン

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;


import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.logger.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.media.MediaPlayer;

import static android.media.AudioTrack.getMaxVolume;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    //private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private Button mStartButton;
    private Button mStopButton;
    private EditText mOutSaveText;
    private Button mSaveTextButton;
    private Button mNbButton;
    private Button mSaveDataButton;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    //private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;


    //------↓↓追加した変数-------------
    private BluetoothFiber mBluetoothFiber;


    private TextView PickUpText;
    //private Button StartButton;

    //変数初期化時、calibrationフラグはtrue
    private boolean calibration = true;
    private int calibrationCount = 0;
    private Double[] averageValue={0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
    private String average_dB;
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd.kk.mm.ss");
    String FilePath = Environment.getExternalStorageDirectory().getPath() + "/BTdata/" + sdf.format(date)+ "_memo.txt";
    private Double[] dBValue= {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
    private long StartTime = 0;
    private boolean actFlag = false;
    //private SoundPool soundPool;
    //private int soundOne;
    private MediaPlayer mediaPlayer;

    //private Context applicationContext;

    private boolean saveFlag = false;
    private boolean monitorFlag = false;
    //private static final int SamplingRate = 32000;
    private static final int SamplingRate = 11000;

    //------追加した変数ここまで-------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }


    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        //sendMessage("#sp");
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //mConversationView = (ListView) view.findViewById(R.id.in);
        //List表示からText表示に変更
        PickUpText = (TextView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);
        mStartButton = (Button) view.findViewById(R.id.button_start);
        mStopButton = (Button) view.findViewById(R.id.button_stop);

        mOutSaveText = (EditText) view.findViewById(R.id.edit_text_save);
        mSaveTextButton = (Button) view.findViewById(R.id.button_savetext);
        mNbButton = (Button) view.findViewById(R.id.button_nb);
        mSaveDataButton = (Button) view.findViewById(R.id.button_savedata);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        //mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        //mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);
        mOutSaveText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message);
                }
            }
        });

        mStartButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                monitorFlag=true;
                sendMessage("#st");
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                monitorFlag=false;
                sendMessage("#sp");
            }
        });

        mSaveTextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                wavPlay();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_save);
                    String message = textView.getText().toString();
                    savedata(message +"\n",FilePath);
                    //MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.comp);
                    //mediaPlayer.start();
                    actFlag=true;
                    StartTime = System.currentTimeMillis();
                    //android.util.Log.d("mediaPlayer.start()","mediaPlayer.start()");
                }
            }
        });

        mNbButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("#nb");
            }
        });

        mSaveDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveFlag=true;//SAVE_DATAボタンを押したらSDカードに取得データの保存を開始する、saveFlagをtrueにする
                mSaveDataButton.setTextColor(0xffff0000);
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        Log.i(TAG,"sendMessage:" + message);
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            message = message + "\r\n";
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            //BluetoothChatFragment.this.sendMessage("#st");
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    PickUpText.setText("Me:  " + writeMessage);
                    break;
                ////ここから文字列読み込み部分////
                case Constants.MESSAGE_READ:
                    //Log.i(TAG,"READ");
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);

                    //monitoring中ならreadMessageを分割、数値に変換し、保存用フォーマットに整列する
                    if(monitorFlag){
                        readMessageCalculation(readMessage);
                        if(actFlag && System.currentTimeMillis()-StartTime>140000){
                            savedata("End\n",FilePath);
                            wavPlay();
                            actFlag=false;
                        }
                    }else{
                        PickUpText.setText(readMessage);
                    }

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    //readMessageをを分割、数値に変換し、保存用フォーマットに整列するための関数

    public String readMessageCalculation(String CutReadMessage){

        mBluetoothFiber = new BluetoothFiber();
        //readMessageが67文字以上（一回分の計測データの文字数）の場合、1セット分ごとに分割してから数値に変換する
        if(CutReadMessage.length()>66)
        {
            //readParts[]に一回分のデータをそれぞれ保存、splitメソッドは区切り文字ごとの文字列の組分の配列を生成
            String[] readParts=CutReadMessage.split("#DA", 0);

            //配列の個数を取得
            int jj=readParts.length;
            //Log.d("-----readPartsi",String.valueOf(jj));
            //Log.d("-----readParts",readParts[1]);
            //Log.d("-----readParts",readParts[2]);
            //Log.d("-----readParts",readParts[3]);

            //配列の個数分、つまり#DAの個数分、文字列データの数値化処理を行う
            for(int j=1;j<=jj-1;j++)
            {
                //Log.d("-----readParts[j]",String.valueOf((readParts[j].length())));
                //Log.d("-----readParts[j]",readParts[j]);

                //配列一個分の文字列が、ちょうど一回の計測分の文字数（65）だったら
                if(readParts[j].length()==65)
                {
                    //ミリ秒単位の時刻を取得
                    //デバッグに使う
                    //long start2 = System.currentTimeMillis();
                    //Log.d("-----read",String.valueOf(j));
                    //Log.d("-----readParts",readParts[i]);

                    //calibrationのフラグがtrueの時、averagevalueに加算
                    if(calibration)
                    {//キャリブレーションが終わってない場合
                        //calibrationCountに加算
                        calibrationCount++;
                        if(calibrationCount>3)averageValue=mBluetoothFiber.StringData_Calibration(readParts[j],averageValue);
                        android.util.Log.d("----AVERAGE------",String.valueOf(calibrationCount));

                        //calibrationCountが23の時、averagevalueを20で除算、計算結果は平均値として格納される
                        if(calibrationCount>22)
                        {
                            mBluetoothFiber.StringData_Average(averageValue, FilePath);
                            calibrationCount=0;
                        }

                    }else{//キャリブレーションが終わっている場合,処理関数StringData_Change_To_Doubleで文字列を数値に変換して画面に表示
                        PickUpText.setText(mBluetoothFiber.StringData_Change_To_Double(readParts[j], averageValue, FilePath, saveFlag));
                    }
                    //calibrationCountが0なら、calibrationフラグをfalseに変更
                    if(calibrationCount==0)calibration=false;

                    //デバッグに使う
                    //long stop2 = System.currentTimeMillis();
                    //stopとstartのミリ秒時刻差をデバッグで表示
                    //Log.d(TAG, "------PickUpText OK----");
                }//if終了
            }//for終了
        }//readMessageの数値変換処理終了
        return "ok";

    }//readMessageCalculation(String CutReadMessage)終了

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    //引数として受け取った文字列を、カンマ毎に切り取る。各Chの値をdBに計算し、日時を追加、フォーマットして返す関数
    /*
    public String StringData_Change_To_Double(String s){

        //String str1 = new String(s);
        String[] new_str = new String[8];
        //String new_str1= new String();
        String dBtoString = new String();
        String sr =new String();
        String CalcResult =new String();
        String SaveString =new String();

        Double[] stringToValue= new Double[8];
        //Double[] dBValue= new Double[8];

        Calendar calendar = Calendar.getInstance();

        final int minute = calendar.get(Calendar.MINUTE);
        final int second = calendar.get(Calendar.SECOND);
        final int ms = calendar.get(Calendar.MILLISECOND);

        //#DA, 1:   0, 2:   0, 3:   0, 4:   0, 5:   0, 6:   0, 7:   0, 8:   0(計測データ1セット：67文字)

        //最初にキャリブレーションを行う。averageValue[1-7]に20サンプル分の平均値を格納する。これをdBに変換する際の基準値として用いる。
        //変数初期化時、calibrationフラグはtrueなので必ず最初にキャリブレーションが行われる
        //calibrationのフラグがtrueの時、averagevalueに加算
        if(calibration)
        {//キャリブレーションが終わってない場合
            //calibrationCountに加算
            calibrationCount++;
            if(calibrationCount>3)
            {
                for(int i=0; i<=7; i++)
                {
                    //このフォーマットで送信されてくる、#DA, 1:   0, 2:   0, 3:   0, 4:   0, 5:   0, 6:   0, 7:   0, 8:   0
                    //フォーマットにしたがって値をチャンネルごとに切り取り、double型に変換してstringToValue[0-7]に格納
                    new_str[i]=s.substring(4+8*i, 8+8*i).trim();
                    //Log.d("OK",new_str[i]);
                    stringToValue[i]=Double.valueOf(new_str[i]);
                    //Log.d("OK","OK13");
                    averageValue[i]= averageValue[i]+ stringToValue[i];
                }
            }

            android.util.Log.d("----AVERAGE------",String.valueOf(calibrationCount));

            //calibrationCountが23の時、averagevalueを20で除算、計算結果は平均値として格納される
            if(calibrationCount>22)
            {
                for(int i=0; i<=7; i++)
                {
                    averageValue[i]=averageValue[i]/20;
                }

                average_dB="Value"+ "," + String.valueOf(averageValue[0]) + "," + String.valueOf(averageValue[1]) + "," + String.valueOf(averageValue[2]) + "," + String.valueOf(averageValue[3]) + "," + String.valueOf(averageValue[4]) + "," + String.valueOf(averageValue[5]) + "," + String.valueOf(averageValue[6]) + "," + String.valueOf(averageValue[7]);
                //SDカードに保存されるデータの先頭に、以下の文字列が保存される※Exel上での処理の際に重要
                sr ="Average"+ "," + "avrg1"+ "," + "avrg2"+ "," + "avrg3"+ "," + "avrg4"+ "," + "avrg5"+ "," + "avrg6"+ "," + "avrg7"+ "," + "avrg8"+"\n";
                sr= sr + average_dB +"\n";
                sr = sr + "Date" + "," + "Ch1" + "," + "Ch2" + "," + "Ch3" +  "," + "Ch4" + "," + "Ch5" + "," + "Ch6" + "," + "Ch7" + "," + "Ch8"// + "," + "dB1" + "," + "dB2" + "," + "dB3" + "," + "dB4" + "," + "dB5" + "," + "dB6" + "," + "dB7" + "," + "dB8"
                        //+ "," + "S1" + "," + "S2" + "," + "P3" +  "," + "P4" + "," + "P5" + "," + "P6" + "," + "P7" + "," + "P8"
                        + "\n";
                savedata(sr,FilePath);
                //最後にcalibrationCountを0にする
                calibrationCount=0;
            }

        }else{//キャリブレーションが終わっている場合
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("kk':'mm':'ss'.'SSS");
            //sr = sr + sdf.format(date)+ " Ch1~8\n"+ "m.s.ms."+ calendar.get(Calendar.MINUTE) + "." + calendar.get(Calendar.SECOND) + "." + ms + " Ch1~8";
            //android.util.Log.d("-------TimeDATE-------",sdf.format(date));

            for(int i=0; i<=7; i++)
            {
                new_str[i]=s.substring(4+8*i, 8+8*i).trim();
                stringToValue[i]=Double.valueOf(new_str[i]);
                dBValue[i]=-10.0*Math.log10(stringToValue[i]/averageValue[i]);
            }

            dBtoString="dB1: " +  String.valueOf(dBValue[0]) + "\n"
                    + ", dB2: " + String.valueOf(dBValue[1]) + "\n"
                    + ", dB3: " + String.valueOf(dBValue[2]) + "\n"
                    + ", dB4: " + String.valueOf(dBValue[3]) + "\n"
                    + ", dB5: " + String.valueOf(dBValue[4]) + "\n"
                    + ", dB6: " + String.valueOf(dBValue[5]) + "\n"
                    + ", dB7: " + String.valueOf(dBValue[6]) + "\n"
                    + ", dB8: " + String.valueOf(dBValue[7]);

            //画面に表示する形式　および　保存する形式
            sr = sdf.format(date) +", ch1: " + new_str[0] + ", ch2: " + new_str[1] + ", ch3: " + new_str[2] + ", ch4: " + new_str[3] + ", ch5: " + new_str[4] + ", ch6: " + new_str[5] + ", ch7: " + new_str[6] + ", ch8: " + new_str[7]
                    +"\n"+dBtoString+"\n";
            //保存する形式
            SaveString = sdf.format(date) +", " + new_str[0] + ", " + new_str[1] + ", " + new_str[2] + ", " + new_str[3] + ", " + new_str[4] + ", " + new_str[5] + ", " + new_str[6] + ", " + new_str[7]
                    + "\n";

            //startFlagがtrueならsavedata関数起動、
            if(startFlag)savedata(SaveString,FilePath);
            //Log.d(TAG,"FilePath: "+FilePath);
            //android.util.Log.d("savedata", "OK");
        }//ここまでで処理終了

        //android.util.Log.d("calibration=false","OK");
        //calibrationCountが0なら、calibrationフラグをfalseに変更
        if(calibrationCount==0)calibration=false;

        //android.util.Log.d("return sr","OK");
        return sr;
    }//String StringData_Change_To_Double(String s)の最終部分
*/

    //保存する関数、SDカードにstrを追加保存

    private void savedata(String str, String Path){
        String state = Environment.getExternalStorageState();
        // Checks if external storage is available for read and write
        if(Environment.MEDIA_MOUNTED.equals(state)){
            File file = new File(Path);

            try(FileOutputStream fileOutputStream =
                        new FileOutputStream(file, true);
                OutputStreamWriter outputStreamWriter =
                        new OutputStreamWriter(fileOutputStream, "UTF-8");
                BufferedWriter bw =
                        new BufferedWriter(outputStreamWriter);
            ) {
                bw.write(str);
                bw.flush();
                //android.util.Log.d("Save","Saved");
            } catch (Exception e) {
                android.util.Log.d("error: FileOutputStream","error: FileOutputStream");
                e.printStackTrace();
            }
        }else{android.util.Log.d("No media to Save","No media to Save");
        }
    }

    private void wavPlay() {
        InputStream input = null;
        byte[] wavData = null;

        try {
            // wavを読み込む
            input = getResources().openRawResource(R.raw.comp);
            wavData = new byte[input.available()];

            // input.read(wavData)
            String readBytes = String.format(
                    Locale.US, "read bytes = %d",input.read(wavData));
            // input.read(wavData)のwarning回避のためだけ
            Log.d("debug",readBytes);
            input.close();
        } catch (FileNotFoundException fne) {
            fne.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Log.d("debug", "error");
        } finally{
            try{
                if(input != null) input.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        // バッファサイズの計算
        int bufSize = android.media.AudioTrack.getMinBufferSize(
                SamplingRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        // AudioTrack.Builder API level 26より
        AudioTrack audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SamplingRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(bufSize)
                .build();
        //audioTrack.setVolume(getMaxVolume());
        // 再生
        audioTrack.play();

        //audioTrack.write(wavData, 0, wavData.length);
        // ヘッダ44byteをオミット
        audioTrack.write(wavData, 44, wavData.length-44);

    }
}
