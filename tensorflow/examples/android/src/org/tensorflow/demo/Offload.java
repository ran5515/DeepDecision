package org.tensorflow.demo;

//package com.example.jiasi.aroffload;

import android.util.Log;

/**
 * Created by jiasi on 7/12/17.
 */

//import android.util.Log;

public class Offload {

    //debug
    private final boolean PRINT_DEBUG = false;

    //inputs
    private double costTarget;
    private double costPerBit;
    private double batteryTarget;
    private double framerateTarget;
    private double accuracyTarget;
    private double networkBandwidth;
    private double networkLatency;
    private double alpha;
    private MeasureFxn[] accuracy;
    private MeasureFxn[] latency;
    private MeasureFxn[] battery;
    private double latency_remote;

    //parameters fixed
    private int numModels;
    private double[] resolutionValLocal;
    private double[] resolutionValRemote;
    private double[] resolutionValLocalFirstDim;
    private double[] resolutionValRemoteFirstDim;
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
    public Offload(double costTarget, double costPerBit, double batteryTarget, double framerateT, double accuracyT, double alpha){
        this.costTarget = costTarget;
        this.costPerBit = costPerBit;
        this.batteryTarget = batteryTarget;
        this.framerateTarget = framerateT;
        this.accuracyTarget = accuracyT;
        this.alpha = alpha;
        setDiscreteVars();
    }

    //set functions
    public void setNetworkInput(double networkBandwidth, double networkLatency){
        this.networkBandwidth = networkBandwidth;
        this.networkLatency = networkLatency;
    }

    public void setTarget(double costTarget, double batteryTarget){
        this.batteryTarget = batteryTarget;
        this.costTarget = costTarget;
    }

    public void setAlpha(double a){
        this.alpha = a;
    }

    public void setFxns(MeasureFxn[] accuracy, MeasureFxn[] latency, MeasureFxn[] battery, double latency_remote){
        this.accuracy = accuracy;
        this.latency = latency;
        this.battery = battery;
        this.latency_remote = latency_remote;
        this.numModels = accuracy.length;
    }

    public void setBatteryTarget(double batteryTarget){
        this.batteryTarget = batteryTarget;
    }

    public void setFramerateTarget(double framerateRate){
        //this.framerateTarget = framerateTarget;
    }

    public void setAccuracyTarget(double accuracyRate){
        //this.accuracyTarget = accuracyTarget;
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
        //dummy initial values
        bitrate = -1;
        resolution = -1;
        framerate = -1;
        decision = -1;
        double utility_max = -100000;


        //check remote
        //Log.v(TAG,"--------check remote model---------", PRINT_DEBUG);
        if(batteryTarget > 2800){

            for (int i=0; i<bitrateVal.length; i++){
                double f = 1/latency_remote*1000;
                f = closestLessThan(framerateVal, f);


                double r = bitrateVal[i];

                if (r >  min(networkBandwidth, battery[0].inv(batteryTarget, networkBandwidth), costTarget/costPerBit/1000,100000)){
                    //Log.v(TAG,"offloading battery: " + battery[0].inv(batteryTarget, networkBandwidth));
                    //Log.v(TAG,"offloading cost: " + costTarget/costPerBit/1000);
                    continue;
                }

                for (int j=0; j<resolutionValRemote.length; j++){
                    double p = resolutionValRemote[j];
                    double delay = (latency_remote + r/f/networkBandwidth*1000 + networkLatency)/30.0;

                    double utility = f + alpha*accuracy[0].interp(p, r, delay);

                    //debug each iteration of the remote model
                    //Log.v(TAG, "Bitrate=" + r + ", framerate=" + f +", resolution=" + p + ", utility=" +utility + ", accuracy per frame:" + accuracy[0].interp(p, r, delay),PRINT_DEBUG);


                    //check if maximum
                    if (utility >= utility_max && f >= framerateTarget && accuracy[0].interp(p, r, delay) >= accuracyTarget){
                        utility_max = utility;
                        this.framerate = f;
                        this.resolution = resolutionValRemoteFirstDim[j];
                        this.bitrate = r;
                        this.decision = 0;
                        this.accuracyPerFrame = accuracy[0].interp(p, r, delay);
                    }
                }
            }
        }



        //check local
        for (int j=1; j<numModels; j++){
            //debug
            //Log.v(TAG,"-------check local model " + j + "-------", PRINT_DEBUG);
            for (int i=0; i<resolutionValLocal.length; i++){

                double p = resolutionValLocal[i];
                double f = Math.min(1/latency[j].interp(p)*1000, batteryTarget/1000/battery[j].interp(p));
                //Log.v(TAG,"framerate necessary to satisfy latency constraint: " + 1/latency[j].interp(p)*1000);
                //Log.v(TAG,"framerate necessary to satisfy battery constraint: " + batteryTarget/1000.0/battery[j].interp(p));
                f = closestLessThan(framerateVal, f);

                double utility = f + alpha * accuracy[j].interp(p,latency[j].interp(p)/30.);

                //debug each iteration of the local model
                //Log.v(TAG, "Bitrate=" + bitrateVal[bitrateVal.length-1] + ", framerate=" + f +", resolution=" + p + ", utility=" +utility + ", accuracy per frame:" + accuracy[j].interp(p,latency[j].interp(p)/30.),PRINT_DEBUG);

                //check if maximum
                if (utility > utility_max && f >= framerateTarget && accuracy[j].interp(p,latency[j].interp(p)/30.) >= accuracyTarget){
                    utility_max = utility;
                    this.framerate = f;
                    this.resolution = resolutionValLocalFirstDim[indOf(resolutionValLocal,p)];
                    this.bitrate = bitrateVal[bitrateVal.length-1];
                    this.decision = j;
                    this.accuracyPerFrame = accuracy[j].interp(p, latency[j].interp(p)/30.);
                }
            }
        }


    }

    //run on local only. Check if satisfy energy constraint.
    private void runLocal(){
        int resolutionInd = 2;
        double p = resolutionValLocal[resolutionInd];
        this.decision = 1;
        this.resolution = resolutionValLocalFirstDim[resolutionInd]; //fixed resolution
        this.bitrate = 0;
        this.accuracyPerFrame = 0;
        this.framerate = 0;

        double f = 1/latency[this.decision].interp(p)*1000; //inverse of processing latency. This assumes NN runs sequentally.

        //check if energy constraint is satisfied
        if (f * battery[this.decision].interp(p) <= batteryTarget){
            this.bitrate = bitrateVal[bitrateVal.length-1];
            this.accuracyPerFrame = accuracy[this.decision].interp(p);
            this.framerate = f;
        }
    }

    //run on server only. Check if satisfy energy and cost constraints are satisfied.
    private void runServer(){
        int resolutionInd = 2;
        double p = resolutionValLocal[resolutionInd];
        this.decision = 0;
        this.resolution = resolutionValRemoteFirstDim[resolutionInd]; //fixed resolution
        this.bitrate = 0;
        this.accuracyPerFrame = 0;
        this.framerate = 0;

        double r = closestLessThan(bitrateVal, networkBandwidth);   //choose bitrate closest to network capacity
        double f = 1 / (networkLatency + r / networkBandwidth);

        //check battery and cost constraints are satisfied
        if (costPerBit * r <= costTarget && battery[this.decision].interp(r,networkBandwidth) <= batteryTarget) {
            this.bitrate = r;    //choose bitrate just under available bandwidth
            this.accuracyPerFrame = accuracy[this.decision].interp(p);
            this.framerate = f;    //inverse of transmission latency.  This assumes NN runs sequentially.
        }
    }

    //simplified version of MCDNN.  Check if energy and cost constraints are satisfied, and pick max accuracy
    public void runMCDNN(){
        double p = resolutionValRemote[1];	//pick a medium resolution. picking max results in really low framerates.
        this.bitrate = 0;
        this.accuracyPerFrame = 0;
        this.framerate = 0;
        this.decision = -1;

        double utility = -1000;

        //check remote
        double f = 1 / latency_remote * 1000;
        f = closestLessThan(framerateVal, f);
        //double r = closestLessThan(bitrateVal, networkBandwidth);
        double r = 500;
        //check cost and energy constraints

        double delay = (latency_remote + r/f/networkBandwidth*1000 + networkLatency)/30.0;
        double accuracyPerFrame = accuracy[0].interp(p,r,delay);

        if (costPerBit * r <= costTarget && battery[0].interp(r,networkBandwidth) <= batteryTarget && f >= framerateTarget && accuracyPerFrame >= accuracyTarget) {
            this.bitrate = r;
            this.resolution = resolutionValRemoteFirstDim[1]; //fixed resolution. MAKE SURE IT'S THE SAME AS EARLIER
            this.accuracyPerFrame = accuracyPerFrame;
            this.framerate = f;    //inverse of transmission latency.  This assumes NN runs sequentally.
            this.decision = 0;
            utility = this.accuracyPerFrame;

        }

        p = resolutionValLocal[2]; //pick a medium resolution
        //check local models
        for (int i=1; i<numModels; i++){
            f = 1.0/latency[i].interp(p)*1000; //inverse of processing latency. This assumes NN runs sequentally.
            f = closestLessThan(framerateVal, f);

            //check if energy constraint satisfied and max utility
            accuracyPerFrame = accuracy[i].interp(p, latency[i].interp(p)/30.);


            //Log.v(TAG, "Bitrate=" + bitrateVal[bitrateVal.length-1] + ", framerate=" + f +", resolution=" + p + ", utility=" +utility + ", accuracy per frame:" + accuracyPerFrame);


            if (f * battery[i].interp(p) <= batteryTarget/1000.0 && accuracyPerFrame > utility && f >= framerateTarget && accuracyPerFrame >= accuracyTarget){
                this.bitrate = bitrateVal[bitrateVal.length-1];
                this.resolution = resolutionValLocalFirstDim[2];
                this.accuracyPerFrame = accuracyPerFrame;
                this.framerate = f;
                this.decision = i;
                utility = accuracyPerFrame;

            }

        }



    }


    /*** HELPER FUNCTIONS ***/

    //calculate the minimum
    private double min(double a, double b, double c, double d){
        return Math.min(Math.min(Math.min(a,b),c),d);
    }

    //hardcode the possible values of resolution, framerate, and bitrate
    private void setDiscreteVars(){
        //set possible local resolution
        this.resolutionValLocal = new double[6];
        resolutionValLocal[0] = 160*160;
        resolutionValLocal[1] = 224*224;
        resolutionValLocal[2] = 288*288;
        resolutionValLocal[3] = 352*352;
        resolutionValLocal[4] = 416*416;
        resolutionValLocal[5] = 480*480;

        this.resolutionValLocalFirstDim = new double[6];
        resolutionValLocalFirstDim[0] = 160;
        resolutionValLocalFirstDim[1] = 224;
        resolutionValLocalFirstDim[2] = 288;
        resolutionValLocalFirstDim[3] = 352;
        resolutionValLocalFirstDim[4] = 416;
        resolutionValLocalFirstDim[5] = 480;

        //set possible remote resolution
        this.resolutionValRemote = new double[3];
        resolutionValRemote[0] = 176*144;
        resolutionValRemote[1] = 320*240;
        resolutionValRemote[2] = 352*288;
        //resolutionValRemote[3] = 640*480;

        this.resolutionValRemoteFirstDim = new double[3];
        resolutionValRemoteFirstDim[0] = 176;
        resolutionValRemoteFirstDim[1] = 320;
        resolutionValRemoteFirstDim[2] = 352;
        //resolutionValRemoteFirstDim[3] = 640;

        //set possible framerates
        this.framerateVal = new double[31];
        for (int i=0; i<31; i++){
            framerateVal[i] = i;
        }

        //set possible bitrates
        this.bitrateVal = new double[10];
        for (int i=0; i<10; i++){
            bitrateVal[i] = i*100;
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

    private int indOf(double[] d, double value){
        int ind = -1;
        for (int i=0; i<d.length; i++){
            if (value == d[i]){
                ind = i;
            }
        }
        return ind;
    }
}
