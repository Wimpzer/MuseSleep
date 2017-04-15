package com.musesleep.musesleep;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ListViewActivity extends AppCompatActivity implements OnItemClickListener {

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview_activity);

        TextView textView = (TextView) findViewById(R.id.textView);
        String headline = getIntent().getExtras().getString("headline");
        textView.setText(headline);

        listView = (ListView) findViewById(R.id.listView);

        int arrayIdentifier = getIntent().getExtras().getInt("array");
        String[] array = getResources().getStringArray(arrayIdentifier);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, array);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selectedValue", listView.getItemAtPosition(position).toString());
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
