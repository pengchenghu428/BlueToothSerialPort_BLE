package com.pengchenghu.bluetoothserialport.domain;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by pengchenghu on 2018/11/2.
 * Author Email: 15651851181@163.com
 * Describe:
 */
public class Label implements Parcelable {
    private String number;
    private int hungry_label;
    private int tired_label;
    private int fear_label;
    private int health_label;

    /*
     * 构造函数
     */
    public Label(String number, int hungry_label, int tired_label, int fear_label,
                 int health_label){
        this.number = number;
        this.hungry_label = hungry_label;
        this.tired_label = tired_label;
        this.fear_label = fear_label;
        this.health_label = health_label;
    }

    // 一般函数
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getHungry_label() {
        return hungry_label;
    }

    public void setHungry_label(int hungry_label) {
        this.hungry_label = hungry_label;
    }

    public int getTired_label() {
        return tired_label;
    }

    public void setTired_label(int tired_label) {
        this.tired_label = tired_label;
    }

    public int getFear_label() {
        return fear_label;
    }

    public void setFear_label(int fear_label) {
        this.fear_label = fear_label;
    }

    public int getHealth_label() {
        return health_label;
    }

    public void setHealth_label(int health_label) {
        this.health_label = health_label;
    }

    /*
     * 1.内容描述
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * 2.序列化
     */
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(number);
        parcel.writeInt(hungry_label);
        parcel.writeInt(tired_label);
        parcel.writeInt(fear_label);
        parcel.writeInt(health_label);
    }

    /**
     * 3.反序列化
     */
    public static final Creator<Label> CREATOR = new Creator<Label>() {
        @Override
        public Label createFromParcel(Parcel in) {
            return new Label(in);
        }

        @Override
        public Label[] newArray(int size) {
            return new Label[size];
        }
    };

    /**
     * 4.自动创建的的构造器，使用反序列化得到的 Parcel 构造对象
     * @param in
     */
    protected Label(Parcel in) {
        number = in.readString();
        hungry_label = in.readInt();
        tired_label = in.readInt();
        fear_label = in.readInt();
        health_label = in.readInt();
    }

}
