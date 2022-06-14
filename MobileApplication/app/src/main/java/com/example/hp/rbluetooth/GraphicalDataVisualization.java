package com.example.hp.rbluetooth;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

public class GraphicalDataVisualization extends AppCompatActivity {
    LineChart linechart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graphical_data_visualization);

        linechart = (LineChart) findViewById(R.id.LineChart);
        ArrayList<String> xvalues = new ArrayList<>();
        ArrayList<Entry> yvalues = new ArrayList<>();


        yvalues.add(new Entry(0,60f));
        yvalues.add(new Entry(1,60f));
        yvalues.add(new Entry(2,60f));
        yvalues.add(new Entry(3,60f));



    }
}
