package com.bogdancornianu.Wake2Radio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.LinkedHashMap;
import java.util.Map;

public class RadioActivity extends Activity {
    private ListView radioStationList;
    private EditText filterRadioText;

    private LinkedHashMapAdapter<String, String> radioStationListAdapter;
    private LinkedHashMap<String, String> radioMapData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);

        String[] radio_urls = this.getResources().getStringArray(R.array.radio_url_entries);
        String[] radio_names = this.getResources().getStringArray(R.array.radio_name_entries);

        radioMapData = new LinkedHashMap<>();

        for (int i = 0; i < Math.min(radio_urls.length, radio_names.length); ++i) {
            radioMapData.put(radio_urls[i], radio_names[i]);
        }

        radioStationListAdapter = new LinkedHashMapAdapter<>(this, android.R.layout.simple_list_item_1, radioMapData, LinkedHashMapAdapter.FLAG_FILTER_ON_VALUE);

        radioStationList = (ListView) findViewById(R.id.radioStationList);
        radioStationList.setAdapter(radioStationListAdapter);

        radioStationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LinkedHashMap.Entry<String, String> listItem = (LinkedHashMap.Entry<String, String>)adapterView.getItemAtPosition(i);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("radioUrl", listItem.getKey());
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });

        filterRadioText = (EditText) findViewById(R.id.filterRadioText);
        filterRadioText.setHint("Enter radio station name");
        filterRadioText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                radioStationListAdapter.getFilter().filter(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }
}
