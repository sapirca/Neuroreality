package com.sapir.neuroreality;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by sapir on 9/28/15.
 */
public class DataMediator extends AppCompatActivity {

    static DataMediator mSingelton;
    static String timeCreated;

    ArrayList<String> arrDatasetNames = new ArrayList<String>()
    {{
//            add("theta");
//            add("std");
//            add("delta");
            add("c3");
//            add("beta");
            add("e3");
            add("e1");
//            add("normType");
            add("e2");
//            add("gamma");
            add("c1");
//            add("sigma");
            add("c2");
//            add("alpha");
            add("h1");
//            add("activity");
            add("h2");
//            add("timestampMillis");
    }};

    ArrayList<ArrayList<Double>> arrTimelines;
    List<String> lstTimeStamps;
    private final int DATASETS_NUM = arrDatasetNames.size();

    public String serialize (String videoId) {
        String s = "";
        s += String.format(
                "%% videoID: %s\n%% userName: %s\n%% userAge: %s\n%% userGender: %s\n" +
                        "%% boxModel: %s\n%% startRecordingTime: %s\n%% movieStarted: %s\n" +
                        "%% movieEnded: %s\n%% %% videoTimestamps: %s\n",
                videoId,
                this.userName,
                this.userAge,
                this.userGender,
                this.boxModel,
                this.getTimeCreated(),
                lstTimeStamps.get(0),
                lstTimeStamps.get(lstTimeStamps.size()-1),
                lstTimeStamps.toString());
        for (int i = 0; i < arrTimelines.size(); i++) {
            s += String.format("%% %s: %s\n", arrDatasetNames.get(i), arrTimelines.get(i).toString());
        }
        return s;
    }

    public String getTimeCreated () {
        return timeCreated;
    }
    private DataMediator () {

        lstTimeStamps = new ArrayList<String>();
        arrTimelines = new ArrayList<ArrayList<Double>>();

        for (int i = 0; i < DATASETS_NUM; i++) {
            arrTimelines.add(new ArrayList<Double>());
        }
        Long timestamp = System.currentTimeMillis()/1000;
        timeCreated = timestamp.toString();
    }


    public static DataMediator getInstance() {
        if (mSingelton == null) {
            mSingelton = new DataMediator();
        }
        return mSingelton;
    }

    public void clear() {
        // "Clear" arrays by creating new objects.
        lstTimeStamps = new ArrayList<String>();
        arrTimelines = new ArrayList<ArrayList<Double>>();

        for (int i = 0; i < DATASETS_NUM; i++) {
            arrTimelines.add(new ArrayList<Double>());
        }
    }

    public boolean isEmpty() {
        if (lstTimeStamps.isEmpty()
                || arrTimelines.get(0).isEmpty()) {
            return true;
        }
        return false;
    }

    public int getDataSetsNum () {
        return DATASETS_NUM;
    }

    public void updateData (String strTimeStamp, String s) {
//        Random random = new Random();
//        for (int i = 0; i < arrTimelines.size(); i++) {
//            arrTimelines.get(i).add(new Integer(random.nextInt() % 5));
//        }
        final String message = s;

        try {
            JSONObject obj = new JSONObject(s).getJSONObject("features");
            Log.d("Websocket", "JSON Data: " + obj.toString());
//            Log.d("Websocket", "theta: " + obj.getJSONObject("features").getDouble("theta"));
            for (int i = 0; i < arrTimelines.size(); i++) {
                Double val = obj.getDouble(arrDatasetNames.get(i));
                arrTimelines.get(i).add(val);
        }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        lstTimeStamps.add(strTimeStamp);
    }

    private Entry buildEntry (Double val, int index) {
        return new Entry(Float.parseFloat(val.toString()), index);
    }

    public LineData convertToLine (int nDataSet) {
        ArrayList<LineDataSet> sets = new ArrayList<LineDataSet>();
       ArrayList<Entry> arrYValues = new ArrayList<>();

        for (int nEntryIndex = 0; nEntryIndex < arrTimelines.get(nDataSet).size(); nEntryIndex++) {

            arrYValues.add(buildEntry(arrTimelines.get(nDataSet).get(nEntryIndex), nEntryIndex));
        }

        LineDataSet tmp = new LineDataSet(arrYValues, arrDatasetNames.get(nDataSet));
        tmp.setLineWidth(2.5f);
        tmp.setCircleSize(4.5f);
        tmp.setHighLightColor(Color.rgb(244, 117, 117));
        tmp.setDrawValues(false);
        tmp.setColor(ColorTemplate.PASTEL_COLORS[nDataSet % 5]);
        tmp.setCircleColor(ColorTemplate.PASTEL_COLORS[nDataSet % 5]);
        sets.add(tmp);

        return new LineData(lstTimeStamps, sets);
    }

    public LineData convertToLine () {

        ArrayList<LineDataSet> sets = new ArrayList<LineDataSet>();
        ArrayList<ArrayList<Entry>> arrYValues = new ArrayList<>();

        // Create 8 Yval entries
        for (int i = 0; i < DATASETS_NUM; i++) {
            arrYValues.add(new ArrayList<Entry>());
        }

        for (int nTimeStamp = 0; nTimeStamp < lstTimeStamps.size(); nTimeStamp++) {

            for (int nDataSet = 0; nDataSet < arrYValues.size(); nDataSet++) {
                arrYValues.get(nDataSet)
                        .add(buildEntry(arrTimelines.get(nDataSet).get(nTimeStamp), nTimeStamp));
            }
        }

        LineDataSet tmp;

        for (int i = 0; i < DATASETS_NUM; i++) {
            tmp = new LineDataSet(arrYValues.get(i), arrDatasetNames.get(i));
            tmp.setLineWidth(2.5f);
            tmp.setCircleSize(4.5f);
            tmp.setHighLightColor(Color.rgb(244, 117, 117));
            tmp.setDrawValues(false);
            tmp.setColor(ColorTemplate.PASTEL_COLORS[i % 5]);
            tmp.setCircleColor(ColorTemplate.PASTEL_COLORS[i % 5]);
            sets.add(tmp);
            tmp = null;
        }
        return new LineData(lstTimeStamps, sets);
    }

    Float sum (ArrayList<Double> arr) {
        Float sum = Float.parseFloat("0");
        for(Double n : arr)
            sum += Float.parseFloat(n.toString());
        return sum;
    }
    public BarData convertToDataBar() {

        ArrayList<BarEntry> entries = new ArrayList<BarEntry>();

        for (int i = 0; i < DATASETS_NUM; i++) {
            entries.add(new BarEntry(sum(arrTimelines.get(i)), i));
        }

        BarDataSet d = new BarDataSet(entries, "Total sums");
        d.setBarSpacePercent(20f);
        d.setColors(ColorTemplate.PASTEL_COLORS);
        d.setHighLightAlpha(255);

        return new BarData(arrDatasetNames, d);
    }

    public PieData convertToDataPie() {

        ArrayList<Entry> entries = new ArrayList<Entry>();

        for (int i = 0; i < DATASETS_NUM; i++) {
            entries.add(new Entry(sum(arrTimelines.get(i)), i));
        }

        PieDataSet d = new PieDataSet(entries, "");

        // space between slices
        d.setSliceSpace(2f);
        d.setColors(ColorTemplate.PASTEL_COLORS);

        return new PieData(arrDatasetNames, d);
    }

    private String userName;
    private String userAge;
    private String userGender;
    private String boxModel;

    public void setUserName(String userName) {
        if (userName.isEmpty()) {
            this.userName = "User_OO";
        } else {
            this.userName = userName;
        }
    }

    public void setUserAge(String userAge) {
        if (userAge.isEmpty()) {
            this.userAge = "Unknown";
        } else {
            this.userAge = userAge;
        }
    }

    public void setUserGender(String userGender) {
        if (userGender.isEmpty()) {
            this.userGender = "Unknown";
        } else {
            this.userGender = userGender;
        }
    }

    public void setBoxModel(String boxModel) {
        if (boxModel.isEmpty()) {
            this.boxModel = "RN_BS-5C18";
        } else {
            this.boxModel = boxModel;
        }
    }
}