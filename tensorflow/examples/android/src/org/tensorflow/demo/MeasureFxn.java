package org.tensorflow.demo;

//package com.example.jiasi.aroffload;

/**
 * Created by jiasi on 7/12/17.
 */

//import android.util.Log;

import java.util.Arrays;
import java.lang.String;



public class MeasureFxn {

    private double[] x1; //used for 1D and 2D and 3D
    private double[] x2; //used for 2D and 3D
    private double[] x3;
    private double[] y; //used for 1D
    private double[][] y2; // used for 2D
    private double[][][] y3; // used for 3D

    private double y_max; // used for 1D
    private double y_max_ind; //used for 1D
    private double[] slope; //used for 1D
    private double[] intercept; //used for 1D
    private int numDimensions;
    private static final String TAG = "MeasureFxn";
    private final double MAX_VALUE = 10000;
    private final double MIN_VALUE = 0;


    /*** CONSTRUCTORS ***/

    //constructor for a 1D function
    //add extra padding. 0 as the first entry
    public MeasureFxn(String x1_input, String y_input){
        double[] x1_input_double = parseDouble(x1_input.split(","));
        double[] y_input_double = parseDouble(y_input.split(","));

        //add padding to x
        this.x1 = new double[x1_input_double.length+1];
        System.arraycopy(x1_input_double,0,this.x1,1,x1_input_double.length);
        //this.x1[x1.length-1] = MAX_VALUE;
        this.x1[0] = 0;

        //add padding to y
        this.y = new double[y_input_double.length+1];
        System.arraycopy(y_input_double,0,this.y,1,y_input_double.length);
        //this.y[y.length-1] = this.y[y.length-2];
        this.y[0] = 0;

        //calculate some parameters
        numDimensions = 1;
        double[] ret = max(y);
        this.y_max = ret[0];
        this.y_max_ind = ret[1];
        computeParams1D();  //compute slope, intercept
    }

    //constructor for a 2D function
    //add extra padding. 0s around left, upper
    public MeasureFxn(String x1_input, String x2_input, String y_input){

        double[] x1_input_double = parseDouble(x1_input.split(","));
        double[] x2_input_double = parseDouble(x2_input.split(","));

        //add padding to x
        this.x1 = new double[x1_input_double.length+1];
        this.x2 = new double[x2_input_double.length+1];
        System.arraycopy(x1_input_double,0,this.x1,1,x1_input_double.length);
        System.arraycopy(x2_input_double,0,this.x2,1,x2_input_double.length);
        this.x1[0] = 0;
        this.x2[0] = 0;


        this.y2 = new double[x1.length][x2.length];
        String[] yPerRow = y_input.split(";");
        for (int i=0; i<yPerRow.length; i++){
            double[] y_input_double = parseDouble(yPerRow[i].split(","));
            System.arraycopy(y_input_double, 0, this.y2[i+1], 1, y_input_double.length);

            //add 0 to beginning of each row
            this.y2[i+1][0] = 0;

        }

        //add padding in first row
        Arrays.fill(this.y2[0],0);


        //compute some parameters
        numDimensions = 2;


    }

    public MeasureFxn(String x1_input, String x2_input, String x3_input, String y_input){
        double[] x1_input_double = parseDouble(x1_input.split(","));
        double[] x2_input_double = parseDouble(x2_input.split(","));
        double[] x3_input_double = parseDouble(x3_input.split(","));

        //add padding to x
        this.x1 = new double[x1_input_double.length+1];
        this.x2 = new double[x2_input_double.length+1];
        this.x3 = x3_input_double;
        System.arraycopy(x1_input_double,0,this.x1,1,x1_input_double.length);
        System.arraycopy(x2_input_double,0,this.x2,1,x2_input_double.length);
        this.x1[0] = 0;
        this.x2[0] = 0;


        this.y3 = new double[x1.length][x2.length][x3.length];
        String[] yPerThirdDim = y_input.split(";;");
        for (int i=0; i<yPerThirdDim.length; i++){
            String[] yPerSecondDim = yPerThirdDim[i].split(";");

            for (int j=0; j<yPerSecondDim.length; j++){
                double[] y_input_double = parseDouble(yPerSecondDim[j].split(","));
                System.arraycopy(y_input_double, 0, this.y3[i+1][j+1], 0, y_input_double.length);

                //Log.v(TAG,"i :" + i+1 + ", j: " + j+1 + ", value: " + Arrays.toString(y_input_double));

                //add padding in first row
                Arrays.fill(this.y3[0][j],0);

            }

            //add padding to each row
            Arrays.fill(this.y3[i+1][0],0);

        }




        //compute some parameters
        numDimensions = 3;

    }

    /*** CALCULATE INTERPOLATION AND INVERSE  ***/
    //1D interpolation
    public double interp(double x1 ){
        double[] x = new double[1];
        x[0] = x1;
        double[] y = interp1D(x);
        return y[0];
    }

    //1D interpolation helper function
    //Based on: http://www.java2s.com/Code/Java/Collections-Data-Structure/LinearInterpolation.htm
    private double[] interp1D(double[] xi) throws IllegalArgumentException {

        // Perform the interpolation here
        double[] yi = new double[xi.length];
        for (int i = 0; i < xi.length; i++) {
            //if ((xi[i] > x1[x1.length - 1]) || (xi[i] < x1[0])) {
            if (xi[i] < x1[0]) {
                yi[i] = Double.NaN;

            } else {
                if (xi[i] > x1[x1.length - 1]) {    //if trying to interpolate a big value, use the last known linear piecewise function

                    int loc = x1.length - 2;
                    yi[i] = slope[loc] * xi[i] + intercept[loc];
                }
                else {  //interpolate normally
                    int loc = Arrays.binarySearch(x1, xi[i]);
                    if (loc < -1) { //cannot find the exact value, so use interpolation
                        loc = -loc - 2;
                        yi[i] = slope[loc] * xi[i] + intercept[loc];
                    } else {
                        yi[i] = y[loc];
                    }
                }
            }
        }

        return yi;
    }

    //2D interpolation
    //Based on: http://supercomputingblog.com/graphics/coding-bilinear-interpolation/
    //Returns y_found = y(x1_search, x2_search)
    public double interp(double x1_search, double x2_search ) throws IllegalArgumentException
    {
        //Log.v(TAG,"2D interp begin. Searching for x1=" + x1_search + ", x2=" + x2_search);

        if (x1_search <0 || x2_search < 0) {
            throw new IllegalArgumentException("Trying to interpolate a negative number.");
        }

        double y_found = -1;

        //look for the closest x1 value
        int loc_x1_upper = -1;
        int loc_x1 = (x1_search > x1[x1.length-1])? x1.length-1 : Arrays.binarySearch(x1, x1_search);   //search only if x1_search is within range

        if (loc_x1 < -1) { //cannot find the exact value, so use interpolation
            loc_x1 = -loc_x1 - 2;
            loc_x1_upper = loc_x1+1;
        }
        else{   //found the exact value. Set the border of the square to the one below, unless we are already at the bottom.
            if (loc_x1 == 0){
                loc_x1_upper = 1;
            }
            else {
                loc_x1_upper = loc_x1;
                loc_x1 = loc_x1 - 1;
            }
        }

        //look for the closest x2 value
        int loc_x2_upper = -1;
        int loc_x2 = (x2_search > x2[x2.length-1])? x2.length-1 : Arrays.binarySearch(x2, x2_search); //search only if x2_search is within range

        if (loc_x2 < -1) { //cannot find the exact value, so use interpolation
            loc_x2 = -loc_x2 - 2;
            loc_x2_upper = loc_x2+1;
        }
        else{   //found the exact value. Set the border of the square to the one below, unless we are already at the bottom.
            if (loc_x2 == 0){
                loc_x2_upper = 1;
            }
            else {
                loc_x2_upper = loc_x2;
                loc_x2 = loc_x2 - 1;
            }
        }

        //Log.v(TAG,String.valueOf(loc_x1));
        //Log.v(TAG,String.valueOf(loc_x2));

        //calculate value of the function near x1_search, x2_search
        double Q11 = y2[loc_x1][loc_x2];
        double Q12 = y2[loc_x1][loc_x2_upper];
        double Q21 = y2[loc_x1_upper][loc_x2];
        double Q22 = y2[loc_x1_upper][loc_x2_upper];
        double x1_upper = x1[loc_x1_upper];
        double x1_lower = x1[loc_x1];
        double x2_upper = x2[loc_x2_upper];
        double x2_lower = x2[loc_x2];

        //interpolate along x2 (R1, R2 are for a fixed x1, diff x2)
        double R1 = (x1_upper - x1_search)/(x1_upper - x1_lower)*Q11 + (x1_search - x1_lower)/(x1_upper - x1_lower)*Q21;
        double R2 = (x1_upper - x1_search)/(x1_upper - x1_lower)*Q12 + (x1_search - x1_lower)/(x1_upper - x1_lower)*Q22;

        //interpolate along x1 (y_found is for a fixed x2)
        y_found = (x2_upper - x2_search)/(x2_upper - x2_lower)*R1 + (x2_search- x2_lower)/(x2_upper - x2_lower)*R2;

        //  Log.v(TAG,"2D interp end");

        return y_found;
    }

    //3D interpolation
    public double interp(double x1_search, double x2_search, double x3_search){
        if (x1_search <0 || x2_search < 0) {
            throw new IllegalArgumentException("Trying to interpolate a negative number.");
        }

        double y_found = -1;

        //lazy, didn't implement interpolation. Instead, find the max value and scale it down.
        int large_x3_flag = 0;

        double x3_search_original = x3_search;
        if (x3_search >= x3.length-1){
            x3_search = x3.length-1;
            large_x3_flag = 1;
        }

        //round the x3 value
        int x3_search_round = (int) Math.round(x3_search);	//small hack here since we input the data in terms of frames, but we really care about time. assume 30fps input data
        //Log.v(TAG,"x3_search: " + x3_search);
        //Log.v(TAG,"x3_search_round: " + x3_search_round);

        //look for the closest x1 value
        int loc_x1_upper = -1;
        int loc_x1 = (x1_search > x1[x1.length-1])? x1.length-1 : Arrays.binarySearch(x1, x1_search);   //search only if x1_search is within range

        if (loc_x1 < -1) { //cannot find the exact value, so use interpolation
            loc_x1 = -loc_x1 - 2;
            loc_x1_upper = loc_x1+1;
        }
        else{   //found the exact value. Set the border of the square to the one below, unless we are already at the bottom.
            if (loc_x1 == 0){
                loc_x1_upper = 1;
            }
            else {
                loc_x1_upper = loc_x1;
                loc_x1 = loc_x1 - 1;
            }
        }

        //look for the closest x2 value
        int loc_x2_upper = -1;
        int loc_x2 = (x2_search > x2[x2.length-1])? x2.length-1 : Arrays.binarySearch(x2, x2_search); //search only if x2_search is within range

        if (loc_x2 < -1) { //cannot find the exact value, so use interpolation
            loc_x2 = -loc_x2 - 2;
            loc_x2_upper = loc_x2+1;
        }
        else{   //found the exact value. Set the border of the square to the one below, unless we are already at the bottom.
            if (loc_x2 == 0){
                loc_x2_upper = 1;
            }
            else {
                loc_x2_upper = loc_x2;
                loc_x2 = loc_x2 - 1;
            }
        }

        //Log.v(TAG,String.valueOf(loc_x1));
        //Log.v(TAG,String.valueOf(loc_x2));

        //calculate value of the function near x1_search, x2_search
        double Q11 = y3[loc_x1][loc_x2][x3_search_round];
        double Q12 = y3[loc_x1][loc_x2_upper][x3_search_round];
        double Q21 = y3[loc_x1_upper][loc_x2][x3_search_round];
        double Q22 = y3[loc_x1_upper][loc_x2_upper][x3_search_round];
        double x1_upper = x1[loc_x1_upper];
        double x1_lower = x1[loc_x1];
        double x2_upper = x2[loc_x2_upper];
        double x2_lower = x2[loc_x2];

        //interpolate along x2 (R1, R2 are for a fixed x1, diff x2)
        double R1 = (x1_upper - x1_search)/(x1_upper - x1_lower)*Q11 + (x1_search - x1_lower)/(x1_upper - x1_lower)*Q21;
        double R2 = (x1_upper - x1_search)/(x1_upper - x1_lower)*Q12 + (x1_search - x1_lower)/(x1_upper - x1_lower)*Q22;

        //interpolate along x1 (y_found is for a fixed x2)
        y_found = (x2_upper - x2_search)/(x2_upper - x2_lower)*R1 + (x2_search- x2_lower)/(x2_upper - x2_lower)*R2;

        //hack if we're looking for a really big latency
        if (large_x3_flag == 1){
            y_found = y_found * x3.length/x3_search_original;
        }
        //Log.v(TAG, "Scaling factor: " + x3_search/x3_search_original);

        return y_found;


    }

    //1D inverse
    //Return x_found = argmax_x ( f(x) : f(x) <= y_search )
    public double inv(double y_search){
        //Log.v(TAG,"1D inverse begin");
        double x_found = -1;

        if (y_search >= this.y_max){
            return x1[(int)y_max_ind];
        }

        int loc = Arrays.binarySearch(y, y_search);
        if (loc < -1) { //cannot find the exact value, so use interpolation
            loc = -loc - 2;
            x_found = (y_search - intercept[loc]) / slope[loc];
        }
        else {  //found the exact value
            x_found = x1[loc];
        }

        //Log.v(TAG,"1D inverse end");
        return x_found;
    }

    //2D inverse
    //Return x_found = argmax_x1 ( f(x1, x2) : f(x1, x2) <= y_search  )
    //Has a bug if y_search > max(y)
    public double inv(double y_search, double x2Given){

        // Log.v(TAG,"2D inv begin");
        double x1_found = -1;

        //calculate the 1D interpolation when x2 = x2Given
        double[] yGivenX2 = new double[x1.length];
        for (int i=0; i<x1.length-1; i++){
            yGivenX2[i] = interp(x1[i], x2Given);
        }
        yGivenX2[yGivenX2.length-1] = yGivenX2[yGivenX2.length-2];  //add padding

        //Log.v(TAG,Arrays.toString(yGivenX2));

        //check if we are searching for something outside of the range
        double[] ret = max(yGivenX2);
        if (y_search >= ret[0]){
            return x1[(int)ret[1]];
        }

        //search along the new line yGivenX2
        //cannot use binarySearch because yGivenX2 may not be increasing
        //instead, find the y closest to y_search
        int loc = -1;
        double diff_min = MAX_VALUE;
        for (int l=x1.length-2; l>=0; l--){
            double diff = y_search-yGivenX2[l];

            if (Math.abs(diff) <= diff_min){

                if (diff == 0){ //found exact value
                    loc = l;
                }
                else{   //need to interpolate.  Return l -> -l-2 if above the found index. Return l-1 -> -(l-1)-2 if below the found index.
                    loc = diff>0 ? -l-2 : -l-1;
                }
                diff_min = Math.abs(diff);

            }
        }

        //Log.v(TAG,String.valueOf(loc));

        if (loc < -1) { //cannot find the exact value, so use interpolation
            loc = -loc - 2;

            //interpolate along yGivenX2 in the x1 direction
            double slope2 = (yGivenX2[loc + 1] - yGivenX2[loc]) / (x1[loc + 1] - x1[loc]);
            double intercept2 = yGivenX2[loc] - x1[loc] * slope2;
            x1_found = (y_search - intercept2) / slope2;

        }
        else {
            x1_found = x1[loc];
        }

        //Log.v(TAG,"2D inv end");

        return x1_found;
    }


    //Compute some parameters for 1D interpolation
    //Based on: http://www.java2s.com/Code/Java/Collections-Data-Structure/LinearInterpolation.htm
    private void computeParams1D() {

        if (x1.length != y.length) {
            throw new IllegalArgumentException("X and Y must be the same length");
        }
        if (x1.length == 1) {
            throw new IllegalArgumentException("X must contain more than one value");
        }
        double[] dx = new double[x1.length - 1];
        double[] dy = new double[x1.length - 1];
        slope = new double[x1.length - 1];
        intercept = new double[x1.length - 1];

        // Calculate the line equation (i.e. slope and intercept) between each point
        for (int i = 0; i < x1.length - 1; i++) {
            dx[i] = x1[i + 1] - x1[i];
            if (dx[i] == 0) {
                throw new IllegalArgumentException("X must be montotonic. A duplicate " + "x-value was found");
            }
            if (dx[i] < 0) {
                throw new IllegalArgumentException("X must be sorted");
            }
            dy[i] = y[i + 1] - y[i];
            slope[i] = dy[i] / dx[i];
            intercept[i] = y[i] - x1[i] * slope[i];
        }
    }


    /*** HELPER FUNCTIONS ***/

    private double[] parseDouble(String[] s){
        double[] d = new double[s.length];
        for (int i=0; i<s.length; i++){
            d[i] = Double.parseDouble(s[i]);
        }
        return d;
    }

    //return the maximum value and the index of the maximum value
    //is it safe to return an array like this?
    private double[] max(double[] d){
        double[] max = new double[2];   //first entry is value, second entry is index
        max[0] = d[0];
        max[1] = 0;
        for (int i=1; i<d.length; i++){
            if (d[i] >= max[0]){
                max[0] = d[i];
                max[1] = i;
            }
        }

        return max;
    }


}
