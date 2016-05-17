package com.zbigniew.beacon_localizator;

/**
 * Created by Zbigniew on 19/01/16.
 */
public class Kalman2
{
    double Q = 0.125; //process noise covariance
    double R = 4; //measurement noise covariance
    double P = 1; // P - estimation error covariance
    private double X = 0, K; // X - value,  K - //kalman gain
    public Kalman2(double Qvalue, double Rvalue, double Pvalue)
    {
        this.Q = Qvalue;
        this.R = Rvalue;
        this.P = Pvalue;
    }

    public Kalman2(){

    }

    private void measurementUpdate()
    {
        K = (P + Q) / (P + Q + R);
        P = R * (P + Q) / (R + P + Q);
    }

    public double update(double measurement)
    {
        measurementUpdate();
        double result = X + (measurement - X) * K;
        X = result;
        return result;
    }

    public double getValue() {
        return X;
    }
}