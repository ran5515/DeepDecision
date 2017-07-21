package org.tensorflow.demo;

import android.util.Log;

import org.tensorflow.demo.MeasureFxn;

/**
 * Created by jiasi on 7/12/17.
 */



public class Offload {

    //inputs
    private double costTarget;
    private double costPerBit;
    private double batteryTarget;
    private double networkBandwidth;
    private double networkLatency;
    private MeasureFxn[] accuracy;
    private MeasureFxn[] latency;
    private MeasureFxn[] battery;

    //parameters fixed
    private int numModels;
    private double[] resolutionVal;
    private double[] bitrateVal;
    private double[] framerateVal;

    //outputs
    private double bitrate;
    private double resolution;
    private double framerate;
    private int decision;
    private double accuracyPerFrame;

    //fixed values
    private static final String TAG = "Offload";

    //constructor
    public Offload(double costTarget, double costPerBit, double batteryTarget){
        this.costTarget = costTarget;
        this.costPerBit = costPerBit;
        this.batteryTarget = batteryTarget;
        setDiscreteVars();
    }

    //set functions
    public void setNetworkInput(double networkBandwidth, double networkLatency){
        this.networkBandwidth = networkBandwidth;
        this.networkLatency = networkLatency;
    }

    public void setFxns(MeasureFxn[] accuracy, MeasureFxn[] latency, MeasureFxn[] battery){
        this.accuracy = accuracy;
        this.latency = latency;
        this.battery = battery;
        this.numModels = accuracy.length;
    }

    //get functions
    public double getBitrate(){
        return bitrate;
    }
    public double getResolution(){
        return resolution;
    }
    public double getFramerate(){
        return framerate;
    }
    public int getDecision(){ return decision; }
    public double getAccuracyPerFrame(){ return accuracyPerFrame; }

    //run the optimization
    public void runOptimization() {
        Log.v(TAG,"runOptimization begin");
        //dummy initial values
        bitrate = -1;
        resolution = -1;
        framerate = -1;
        decision = -1;
        double utility_max = -100000;


        //check remote
        Log.v(TAG,"--------check remote model---------");


        for (int i=0; i<framerateVal.length; i++){
            double f = framerateVal[i];

            //bitrate is pretty sensitive to framerate since 1/f factor
            double r = min(networkBandwidth, battery[0].inv(batteryTarget, networkBandwidth), costTarget/costPerBit/1000, networkBandwidth*(1-networkLatency/1000*f));

            //uncomment for debug
            //Log.v(TAG,"\nbitrate necessary to satisfy battery constraint: " + battery[0].inv(batteryTarget, networkBandwidth));
            //Log.v(TAG,"bitrate necessary to satisfy cost constraint: " + costTarget/costPerBit/1000);
            //Log.v(TAG,"bitrate necessary to satisfy framerate constraint: " + networkBandwidth*(1-networkLatency/1000*f));


            r = closestLessThan(bitrateVal,r);


            if (r < 0){
                Log.v(TAG, "negative rate, skip");
                continue;
            }
            for (int j=0; j<resolutionVal.length; j++){
                double p = resolutionVal[j];
                double utility = f * accuracy[0].interp(p, r/f);    //sometimes utility is negative if interpolating beyond the known bounds of r/f

                //debug each iteration of the remote model
                //Log.v(TAG, "Bitrate=" + r + ", framerate=" + f +", resolution=" + p + "; utility=" +utility);

                //check if maximum
                if (utility >= utility_max){
                    utility_max = utility;
                    this.framerate = f;
                    this.resolution = p;
                    this.bitrate = r;
                    this.decision = 0;
                    this.accuracyPerFrame = accuracy[0].interp(p, r/f);
                }
            }
        }



        //check local
        for (int j=1; j<numModels; j++){
            //debug
            Log.v(TAG,"-------check local model " + j + "-------");
            for (int i=0; i<framerateVal.length; i++){
                double f = framerateVal[i];
                double p = Math.min(latency[j].inv(1/f*1000), battery[j].inv(batteryTarget/f));

                //uncomment for debug
                //Log.v(TAG,"\nresolution necessary to satisfy latency constraint: " + latency[j].inv(1/f*1000));
                //Log.v(TAG,"resolution necessary to satisfy battery constraint: " + battery[j].inv(batteryTarget/f));

                p = closestLessThan(resolutionVal, p);
                double utility = f * accuracy[j].interp(p, bitrateVal[bitrateVal.length-1]/f);

                //debug each iteration of the local model
                //Log.v(TAG, "Bitrate=" + bitrateVal[bitrateVal.length-1] + ", framerate=" + f +", resolution=" + p + "; utility=" +utility);

                //check if maximum
                if (utility > utility_max){
                    utility_max = utility;
                    this.framerate = f;
                    this.resolution = p;
                    this.bitrate = bitrateVal[bitrateVal.length-1];
                    this.decision = j;
                    this.accuracyPerFrame = accuracy[j].interp(p, bitrateVal[bitrateVal.length-1]/f);
                }
            }
        }

        Log.v(TAG,"runOptimization end");

    }


    /*** HELPER FUNCTIONS ***/

    //calculate the minimum
    private double min(double a, double b, double c, double d){
        return Math.min(Math.min(Math.min(a,b),c),d);
    }

    //hardcode the possible values of resolution, framerate, and bitrate
    private void setDiscreteVars(){
        this.resolutionVal = new double[4];
        resolutionVal[0] = 160*160;
        resolutionVal[1] = 224*224;
        resolutionVal[2] = 288*288;
        resolutionVal[3] = 352*352;
        //resolutionVal[4] = 416*416;
        //resolutionVal[5] = 480*480;
        this.framerateVal = new double[30];
        for (int i=1; i<31; i++){
            framerateVal[i-1] = i;
        }
        this.bitrateVal = new double[100];
        for (int i=1; i<101; i++){
            bitrateVal[i-1] = i*10;
        }
        //keep this range about the same as the accuracy measurements
    }

    //helper function that calculates which value in the array d is closest to, but still less than, value
    private double closestLessThan(double[] d, double value){
        double r = 0;
        for (int i=0; i<d.length; i++){
            if (value >= d[i]){
                r = d[i];
            }
        }
        return r;
    }

}
