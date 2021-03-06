package com.zbigniew.beacon_localizator;

import android.graphics.Point;
import android.os.Parcelable;

import org.altbeacon.beacon.Beacon;

/**
 * Created by Zbigniew on 13/01/16.
 */
public class MyBeacon extends Beacon{
    static Parcelable.Creator CREATOR;

    private Punkt punktBeacona;
    private int lastTenRSSi[] = new int[10];
    private double avgRssi;
    private double avgDistance;

    public MyBeacon(Beacon beacon){
        super(beacon);
    }

    public void setAvgDistance(double avgDistance) {
        this.avgDistance = avgDistance;
    }

    public void addToAvgDistance(double value) {
        this.avgDistance += value;
    }

    public void minusToAvgDistance(double value) {
        this.avgDistance -= value;
    }

    public double getAvgDistance() {
        return avgDistance;
    }


    public void setPunktBeacona(Punkt punktBeacona) {
        this.punktBeacona = punktBeacona;
    }

    public Punkt getPunktBeacona() {
        return punktBeacona;
    }

    public double getAvgRssi() {
        return avgRssi;
    }

    public void setAvgRssi(double rssi){
        avgRssi = rssi;
    }

    public void addToLastTenRSSi(int value) {
        for (int i = 1; i < 10; i++) {
            lastTenRSSi[i - 1] = lastTenRSSi[i];
        }
        lastTenRSSi[9] = value;
    }

    @Override
    public boolean equals(Object object)
    {
        boolean sameSame = false;

        if (object != null && object instanceof MyBeacon)
        {
            sameSame = this.getId2().equals(((MyBeacon) object).getId2());
        }

        return sameSame;
    }
}

