package com.example.android.bluetoothchat;

import android.os.Environment;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
//import java.util.Calendar;
import java.util.Date;

public class BluetoothFiber {
    //------↓↓追加した変数-------------

    //変数初期化時、calibrationフラグはtrue
    //private boolean calibration = true;
    //private int calibrationCount = 0;
    //public Double[] averageValue={0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
    private String average_dB;
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd.kk.mm.ss");
    //String FilePath = Environment.getExternalStorageDirectory().getPath() + "/BTdata/" + sdf.format(date)+ "_memo.txt";
    private Double[] dBValue= {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};

    //------追加した変数ここまで-------------

    //引数として受け取った文字列を、カンマ毎に切り取る。各Chの値をdBに計算し、日時を追加、フォーマットして返す関数
    public Double[] StringData_Calibration(String s, Double[] averageValue){

        String[] new_str = new String[8];
        Double[] stringToValue= new Double[8];

        //#DA, 1:   0, 2:   0, 3:   0, 4:   0, 5:   0, 6:   0, 7:   0, 8:   0(計測データ1セット：67文字)
        //最初にキャリブレーションを行う。averageValue[1-7]に20サンプル分の平均値を格納する。これをdBに変換する際の基準値として用いる。
        //変数初期化時、calibrationフラグはtrueなので必ず最初にキャリブレーションが行われる
        //calibrationのフラグがtrueの時、averagevalueに加算

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
        return averageValue;
    }//void StringData_Calibration(String s)の最終部分


    public void StringData_Average(Double[] averageValue,String Path){
        String sr =new String();
        //#DA, 1:   0, 2:   0, 3:   0, 4:   0, 5:   0, 6:   0, 7:   0, 8:   0(計測データ1セット：67文字)

        //calibrationCountが23の時、averagevalueを20で除算、計算結果は平均値として格納される
        for(int i=0; i<=7; i++)averageValue[i]=averageValue[i]/20;

        average_dB="Value"+ "," + String.valueOf(averageValue[0]) + "," + String.valueOf(averageValue[1]) + "," + String.valueOf(averageValue[2]) + "," + String.valueOf(averageValue[3]) + "," + String.valueOf(averageValue[4]) + "," + String.valueOf(averageValue[5]) + "," + String.valueOf(averageValue[6]) + "," + String.valueOf(averageValue[7]);
        //SDカードに保存されるデータの先頭に、以下の文字列が保存される※Exel上での処理の際に重要
        sr ="Average"+ "," + "avrg1"+ "," + "avrg2"+ "," + "avrg3"+ "," + "avrg4"+ "," + "avrg5"+ "," + "avrg6"+ "," + "avrg7"+ "," + "avrg8"+"\n";
        sr= sr + average_dB +"\n";
        sr = sr + "Date" + "," + "Ch1" + "," + "Ch2" + "," + "Ch3" +  "," + "Ch4" + "," + "Ch5" + "," + "Ch6" + "," + "Ch7" + "," + "Ch8"
                + "\n";
        savedata(sr,Path);
    }//void StringData_Average(String s)の最終部分


    public String StringData_Change_To_Double(String s, Double[] averageValue, String Path, Boolean saveFlag){
        String[] new_str = new String[8];
        String dBtoString = new String();
        String sr =new String();
        String SaveString =new String();
        Double[] stringToValue= new Double[8];
        //#DA, 1:   0, 2:   0, 3:   0, 4:   0, 5:   0, 6:   0, 7:   0, 8:   0(計測データ1セット：67文字)

        //キャリブレーションが終わっている場合
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("kk':'mm':'ss'.'SSS");

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
        //saveFlagがtrueならsavedata関数起動、
        if(saveFlag)savedata(SaveString,Path);
        //Log.d(TAG,"FilePath: "+FilePath);
        //android.util.Log.d("savedata", "OK");
        return sr;
    }//String StringData_Change_To_Double(String s)の最終部分


    //保存する関数、SDカードにstrを追加保存
    private void savedata(String str, String Path){
        String state = Environment.getExternalStorageState();
        /* Checks if external storage is available for read and write */
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




}
