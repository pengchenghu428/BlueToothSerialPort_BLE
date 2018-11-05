package com.pengchenghu.bluetoothserialport.tools;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.pengchenghu.bluetoothserialport.domain.Label;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by pengchenghu on 2018/11/4.
 * Author Email: 15651851181@163.com
 * Describe:
 */
public class ParseBLEFrame {
    // 数据帧长度
    public static final int DATA_LENGTH = 224;

    //转化为具体数据
    public static List<Integer> ECG = new ArrayList<>();    //ECG ——心电数据
    public static List<Integer> PPG = new ArrayList<>();    //PPG ——数据
    public static List<Integer> GSR = new ArrayList<>();    //GSR数据
    public static List<Integer> SpO2 = new ArrayList<>();   //血氧数据
    public static List<Integer> Pulse = new ArrayList<>();  //脉搏数据
    public static List<Float> TEM = new ArrayList<>();      //温度数据
    public static List<Float> INFRARED_TEM = new ArrayList<>(); //红外温度

    // 表头变量
    public static String parseBleDataHeader = String.format("%-7s%-7s%-7s%-10s%-10s%-8s%-8s",
            "ECG","PPG","GSR","SpO2","Pulse","DS18B20","MLX904");

    // 其他变量
    private static int num_PEG = 0;
    private static int num_SP = 0;

    // 解析数据
    public static void parseBLEDataFrame(String str){
        Log.d("ParseBLE", new Date().toString() + " "+ " PEG:" + num_PEG + " SP:" +num_SP);
        Log.d("ParseBLE", " ECG:" + ECG.size() + " TEM:" + TEM.size() + " SP:" + SpO2.size());
        if (str.length() != DATA_LENGTH){
            return;
        }
        //判断生理数据的类型
        if (str.charAt(DATA_LENGTH - 1) == '1') {
            num_PEG++;
            getCardiogramData(str); //提取心电数据
            getPPGData(str); //提取心电数据
            getGSRData(str); //提取皮电数据
            if(num_PEG > num_SP) {
                getDS18B20Data(str); //提取DS18B20数据
                getInfraredTem(str); //提取红外数据
            }
        } else if (str.charAt(DATA_LENGTH - 1) == '4') {
            for(int i = num_SP; i < num_PEG-1; i++){  // 补充SpO2和Pulse数据帧
                for(int j = 0; j < 17; j++){
                    SpO2.add(0);
                    Pulse.add(0);
                }
                num_SP++;
            }
            num_SP++;
            getSpO2(str);
            getPulse(str);
            if(num_PEG < num_SP){
                getDS18B20Data(str); //提取DS18B20数据
                getInfraredTem(str); //提取红外数据
            }
        }
    }

    // 得到当前解析的数据字符串
    public static String getParseDataFrame(String str){
        String returnStr = "";
        if(str.length() < DATA_LENGTH){
            return returnStr;
        }
        if(str.charAt(DATA_LENGTH-1) == '1'){
            List<Integer> ecg = ECG.subList((num_PEG-1)*17, num_PEG*17);
            returnStr += ecg.toString();
            List<Integer> ppg = PPG.subList((num_PEG-1)*17, num_PEG*17);
            returnStr += ppg.toString();
            List<Integer> gsr = GSR.subList((num_PEG-1)*17, num_PEG*17);
            returnStr += gsr.toString();
            returnStr += TEM.get(TEM.size()-1).toString();
            returnStr += INFRARED_TEM.get(INFRARED_TEM.size()-1).toString();
        }else{
            List<Integer> spo2 = SpO2.subList((num_SP-1)*17, num_SP*17);
            returnStr += spo2.toString();
            List<Integer> pulse = Pulse.subList((num_SP-1)*17, num_SP*17);
            returnStr += pulse.toString();
            returnStr += TEM.get(TEM.size()-1).toString();
            returnStr += INFRARED_TEM.get(INFRARED_TEM.size()-1).toString();
        }
        return returnStr;
    }

    // 得到当前解析的数据字符串分类后的数据
    public static List<String> getParseDataFrameAfterClassify(){
        List<String> listStr = new ArrayList<>();

        int num_Min = Math.min(num_PEG, num_SP);
        if(num_Min > 0){
            if(TEM.size() >= 17*(num_Min) && TEM.size() >= 17*(num_Min-1)){
                Log.d("ParseBLE", "True");
            }else{
                Log.d("ParseBLE", "False");
                return listStr;
            }
            for(int i = 0; i < 17; i++){
                String ecg = String.format("%-7s", ECG.get(17*(num_Min-1)+i).toString());
                String ppg = String.format("%-7s", PPG.get(17*(num_Min-1)+i).toString());
                String gsr = String.format("%-7s", GSR.get(17*(num_Min-1)+i).toString());
                String spo2 = String.format("%-10s", SpO2.get(17*(num_Min-1)+i).toString());
                String pulse = String.format("%-10s", Pulse.get(17*(num_Min-1)+i).toString());
                String ds18b20 = String.format("%-8s", TEM.get(17*(num_Min-1)+i).toString());
                String mlx904 = String.format("%-8s", INFRARED_TEM.get(17*(num_Min-1)+i).toString());
                listStr.add(ecg+ppg+gsr+spo2+pulse+ds18b20+mlx904);
            }
        }

//        int num_Min = getMinLength();
//        if(num_Min > 0){
//            for(int i = 0; i < 17; i++){
//                String ecg = String.format("%-7s", ECG.get(num_Min-17+i).toString());
//                String ppg = String.format("%-7s", PPG.get(num_Min-17+i).toString());
//                String gsr = String.format("%-7s", GSR.get(num_Min-17+i).toString());
//                String spo2 = String.format("%-10s", SpO2.get(num_Min-17+i).toString());
//                String pulse = String.format("%-10s", Pulse.get(num_Min-17+i).toString());
//                String ds18b20 = String.format("%-8s", TEM.get(num_Min-17+i).toString());
//                String mlx904 = String.format("%-8s", INFRARED_TEM.get(num_Min-17+i).toString());
//                listStr.add(ecg+ppg+gsr+spo2+pulse+ds18b20+mlx904);
//            }
//        }

        return listStr;
    }

    //解析DS18B20温度
    public static void getDS18B20Data(String s) {
        StringBuilder builder = new StringBuilder();
        String sub = s.substring(DATA_LENGTH - 14, DATA_LENGTH - 12);
        builder.append(sub);
        sub = s.substring(DATA_LENGTH - 16, DATA_LENGTH - 14);
        builder.append(sub);
        sub = s.substring(DATA_LENGTH - 18, DATA_LENGTH - 16);
        builder.append(sub);
        sub = s.substring(DATA_LENGTH - 20, DATA_LENGTH - 18);
        builder.append(sub);
        int temperature = Integer.valueOf(builder.toString(), 16);
        float res_integer = temperature / 16;       //
        float res_decimal = 0.0f;
        int temp_temperature = (temperature & 0xf);
        if(temp_temperature != 0){  // 避免除0错误
            res_decimal = (10000 / (temp_temperature)) / 100;
            while (res_decimal >= 1) res_decimal /= 10;
        }
        for (int i = 0; i < 17; i++){
            TEM.add(res_integer + res_decimal);
        }
//        try {
//            int temp_temperature = (temperature & 0xf);
//            if(temp_temperature != 0){  // 避免除0错误
//                res_decimal = (10000 / (temp_temperature)) / 100;
//                while (res_decimal >= 1) res_decimal /= 10;
//            }
//            for (int i = 0; i < 17; i++){
//                TEM.add(res_integer + res_decimal);
//            }
//        }catch (ArithmeticException e){
//            Log.e("ParseBLE", builder.toString() + "   "+ temperature);
//            for (int i = 0; i < 17; i++){
//                TEM.add(res_integer);
//            }
//        }
//        return res_integer + res_decimal;
    }

    //解析红外温度
    public static void getInfraredTem(String s) {
        StringBuilder builder = new StringBuilder();
        String sub = s.substring(DATA_LENGTH - 6, DATA_LENGTH - 4);
        builder.append(sub);
        sub = s.substring(DATA_LENGTH - 8, DATA_LENGTH - 6);
        builder.append(sub);
        sub = s.substring(DATA_LENGTH - 10, DATA_LENGTH - 8);
        builder.append(sub);
        sub = s.substring(DATA_LENGTH - 12, DATA_LENGTH - 10);
        builder.append(sub);
        int temperature = Integer.valueOf(builder.toString(), 16);
        float res_integer = temperature / 100.0f;
        for (int i = 0; i < 17; i++){
            INFRARED_TEM.add(res_integer);
        }
        //return res_integer;
    }

    //解析心电ECG数据
    public static void getCardiogramData(String s) {
        for (int i = 0; i < 17; i++) {
            StringBuilder builder = new StringBuilder();
            String sub = s.substring(12 * i + 2, 12 * i + 4);
            builder.append(sub);
            sub = s.substring(12 * i, 12 * i + 2);
            builder.append(sub);
            int cardiogram = Integer.valueOf(builder.toString(), 16);
            ECG.add(cardiogram);
        }
    }

    // 解析脉搏波PPG数据
    public static void getPPGData(String s) {
        for (int i = 0; i < 17; i++) {
            StringBuilder builder = new StringBuilder();
            String sub = s.substring(12 * i + 6, 12 * i + 8);
            builder.append(sub);
            sub = s.substring(12 * i + 4, 12 * i + 6);
            builder.append(sub);
            int ppg = Integer.valueOf(builder.toString(), 16);
            PPG.add(ppg);
        }
    }

    // 解析皮电GSR数据
    public static void getGSRData(String s) {
        for (int i = 0; i < 17; i++) {
            StringBuilder builder = new StringBuilder();
            String sub = s.substring(12 * i + 10, 12 * i + 12);
            builder.append(sub);
            sub = s.substring(12 * i + 8, 12 * i + 10);
            builder.append(sub);
            int gsr = Integer.valueOf(builder.toString(), 16);
            GSR.add(gsr);
        }
    }

    // 解析血氧饱和SpO2数据
    public static void getSpO2(String s) {
        for (int i = 0; i < 17; i++) {
            StringBuilder builder = new StringBuilder();
            String sub = s.substring(12 * i + 4, 12 * i + 6);
            builder.append(sub);
            sub = s.substring(12 * i + 2, 12 * i + 4);
            builder.append(sub);
            sub = s.substring(12 * i, 12 * i + 2);
            builder.append(sub);
            int spo2 = Integer.valueOf(builder.toString(), 16);
            SpO2.add(spo2);
        }
    }

    // 解析脉搏数据（血氧传感去获得的）
    public static void getPulse(String s) {
        for (int i = 0; i < 17; i++) {
            StringBuilder builder = new StringBuilder();
            String sub = s.substring(12 * i + 10, 12 * i + 12);
            builder.append(sub);
            sub = s.substring(12 * i + 8, 12 * i + 10);
            builder.append(sub);
            sub = s.substring(12 * i + 6, 12 * i + 8);
            builder.append(sub);
            int pulse = Integer.valueOf(builder.toString(), 16);
            Pulse.add(pulse);
        }
    }

    // 清空所有数据
    public static void clearData(){
        ECG.clear();
        PPG.clear();
        GSR.clear();
        SpO2.clear();
        Pulse.clear();
        TEM.clear();
        INFRARED_TEM.clear();

        num_PEG = 0;
        num_SP = 0;
    }

    // 将解析后的数据写入文件
    public static void writeParseDataToFile(Label label, String filename){
        String rootDir = Environment.getExternalStoragePublicDirectory("").toString() + "/ATemp/Extract_Data";
        File directory = new File(rootDir);
        if(!directory.exists()){    // // 创建二级目录
            directory.mkdir();
        }
        File extract_out = new File(rootDir, filename); // 实例化文件对象
        try {
            BufferedWriter extract_data_writer = new BufferedWriter(new FileWriter(extract_out));
            // 写入标签
            extract_data_writer.write("Number: "+ label.getNumber() + System.lineSeparator());
            extract_data_writer.write("Hungry: "+ label.getHungry_label() + System.lineSeparator());
            extract_data_writer.write("Tired: "+ label.getTired_label() + System.lineSeparator());
            extract_data_writer.write("Fear: "+ label.getFear_label() + System.lineSeparator());
            extract_data_writer.write("Health: "+ label.getHealth_label() + System.lineSeparator());
            // 写入特征参数
            String header = String.format("%-12s%-12s%-12s%-12s%-12s%-12s%-12s",
                    "ECG","PPG","GSR","SpO2","Pulse","DS18B20","MLX904");
            extract_data_writer.write(header + System.lineSeparator());
            int ecg = ECG.size();
            int ppg = PPG.size();
            int gsr = GSR.size();
            int spo2 = SpO2.size();
            int pulse = Pulse.size();
            int ds18b20 = TEM.size();
            int mlx904 = INFRARED_TEM.size();
            int max = Math.max(ecg, ppg);
            max = Math.max(max, gsr);
            max = Math.max(max, spo2);
            max = Math.max(max, pulse);
            max = Math.max(max, ds18b20);
            max = Math.max(max, mlx904);
            String ecg_data, ppg_data, gsr_data, spo2_data, pulse_data, ds18b20_data, mlx904_data;
            for (int i = 0; i < max; i++) {

                if (i < ecg) ecg_data = ECG.get(i).toString();
                else ecg_data = "null";

                if (i < ppg) ppg_data = PPG.get(i).toString();
                else ppg_data = "null";

                if (i < gsr) gsr_data = GSR.get(i).toString();
                else gsr_data = "null";

                if (i < spo2) spo2_data = SpO2.get(i).toString();
                else spo2_data = "null";

                if (i < pulse) pulse_data = Pulse.get(i).toString();
                else pulse_data = "null";

                if (i < ds18b20) ds18b20_data = TEM.get(i).toString();
                else ds18b20_data = "null";

                if (i < mlx904) mlx904_data = INFRARED_TEM.get(i).toString();
                else mlx904_data = "null";

                String line = String.format("%-12s%-12s%-12s%-12s%-12s%-12s%-12s",
                        ecg_data, ppg_data, gsr_data, spo2_data, pulse_data, ds18b20_data, mlx904_data);
                extract_data_writer.write(line + System.lineSeparator());
            }
            extract_data_writer.flush();
            extract_data_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //
    public static int getMaxLength(){
        int ecg = ECG.size();
        int ppg = PPG.size();
        int gsr = GSR.size();
        int spo2 = SpO2.size();
        int pulse = Pulse.size();
        int ds18b20 = TEM.size();
        int mlx904 = INFRARED_TEM.size();
        int max = Math.max(ecg, ppg);
        max = Math.max(max, gsr);
        max = Math.max(max, spo2);
        max = Math.max(max, pulse);
        max = Math.max(max, ds18b20);
        max = Math.max(max, mlx904);
        return max;
    }
}
