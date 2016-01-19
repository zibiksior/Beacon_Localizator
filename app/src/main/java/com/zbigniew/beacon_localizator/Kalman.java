package com.zbigniew.beacon_localizator;

/**
 * Created by Zbigniew on 18/01/16.
 */
class Kalman
{
    /// <summary>
    ///  x
    /// </summary>
    private double value;                       //x
    /// <summary>
    ///  q
    /// </summary>
    private double processNoiseCovar;         //q
    /// <summary>
    ///  r
    /// </summary>
    private double measureNoiseCovar;           //r
    /// <summary>
    ///  p
    /// </summary>
    private double estimationErrorCovar;        //p
    /// <summary>
    ///  k
    /// </summary>
    private double gain;                        //k

    public Kalman(double value, double processNoiseCovar, double measureNoiseCovar, double estimationErrorCovar) {
        this.value = value;
        this.processNoiseCovar = processNoiseCovar;
        this.measureNoiseCovar = measureNoiseCovar;
        this.estimationErrorCovar = estimationErrorCovar;
    }

    public double getValue() {
        return value;
    }

    public double update(double reading)
    {
        //Prediction Update
        estimationErrorCovar = estimationErrorCovar + processNoiseCovar;

        //Measurement Update
        gain = estimationErrorCovar / (estimationErrorCovar + measureNoiseCovar);
        value = value + (gain * (reading - value));
        estimationErrorCovar = (1 - gain)*estimationErrorCovar;
        return value;
    }
}