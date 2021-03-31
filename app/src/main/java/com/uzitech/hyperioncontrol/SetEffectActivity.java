package com.uzitech.hyperioncontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class SetEffectActivity extends AppCompatActivity {

    SharedPreferences preferences;
    String url;
    JSONArray effects;
    ArrayList<String> effectNames;

    ListView effects_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_effect);

        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(750, TimeUnit.MILLISECONDS)
                .build();

        AndroidNetworking.initialize(this, okHttpClient);

        preferences = getSharedPreferences(getString(R.string.pref_data), Context.MODE_PRIVATE);

        String addr = preferences.getString("server_addr", "");
        String port = preferences.getString("server_port", "8090");

        url = "http://" + addr + ":" + port + "/json-rpc";

        try {
            effects = new JSONArray(getIntent().getStringExtra("list"));

            if (effects.length() > 0) {
                effectNames = new ArrayList<>();
                for (int i = 0; i < effects.length(); i++) {
                    effectNames.add(effects.getJSONObject(i).getString("name"));
                }

                effects_list = findViewById(R.id.effects_list);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, effectNames);
                effects_list.setAdapter(adapter);

                effects_list.setOnItemClickListener((parent, view, position, id) -> startEffect(position));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startEffect(int pos) {
        JSONObject effectsObject = new JSONObject();
        JSONObject responseObject = new JSONObject();

        try {
            effectsObject.put("name", effectNames.get(pos));

            responseObject.put("command", "effect");
            responseObject.put("effect", effectsObject);
            responseObject.put("priority", 50);
            responseObject.put("origin", getString(R.string.app_name));
        } catch (Exception e) {
            e.printStackTrace();
        }

        AndroidNetworking.post(url)
                .addJSONObjectBody(responseObject)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {

                    }

                    @Override
                    public void onError(ANError error) {
                        if (error.getErrorCode() == 0) {
                            Toast.makeText(getApplicationContext(), getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}