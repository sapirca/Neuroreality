
package com.sapir.neuroreality.graph;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.sapir.neuroreality.DataMediator;
import com.sapir.neuroreality.R;
import com.sapir.neuroreality.graph.listviewitems.BarChartItem;
import com.sapir.neuroreality.graph.listviewitems.ChartItem;
import com.sapir.neuroreality.graph.listviewitems.LineChartItem;
import com.sapir.neuroreality.graph.listviewitems.PieChartItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates the use of charts inside a ListView. IMPORTANT: provide a
 * specific height attribute for the chart inside your listview-item
 * 
 * @author Philipp Jahoda
 */
public class ListViewMultiChartActivity extends FragmentData {

    DataMediator dataMediator = DataMediator.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_listview_chart);
        ListView lv = (ListView) findViewById(R.id.listView1);
        ArrayList<ChartItem> list = new ArrayList<ChartItem>();

        list.add(new PieChartItem(dataMediator.convertToDataPie(), getApplicationContext()));
        list.add(new BarChartItem(dataMediator.convertToDataBar(), getApplicationContext()));
        list.add(new LineChartItem(dataMediator.convertToLine(), getApplicationContext()));

        for (int i=0; i < dataMediator.getDataSetsNum(); i++) {
            list.add(new LineChartItem(dataMediator.convertToLine(i), getApplicationContext()));
        }

        ChartDataAdapter cda = new ChartDataAdapter(getApplicationContext(), list);
        lv.setAdapter(cda);
    }

    /** adapter that supports 3 different item types */
    private class ChartDataAdapter extends ArrayAdapter<ChartItem> {
        
        public ChartDataAdapter(Context context, List<ChartItem> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getItem(position).getView(position, convertView, getContext());
        }
        
        @Override
        public int getItemViewType(int position) {           
            // return the views type
            return getItem(position).getItemType();
        }
        
        @Override
        public int getViewTypeCount() {
            return 3; // we have 3 different item-types
        }
    }
}
