package com.uzitech.hyperioncontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class SetColorActivity extends AppCompatActivity {

    SharedPreferences preferences;

    String url;

    ColorPickerView colorPickerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_color);

        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(750, TimeUnit.MILLISECONDS)
                .build();

        AndroidNetworking.initialize(this, okHttpClient);

        preferences = getSharedPreferences(getString(R.string.pref_data), Context.MODE_PRIVATE);

        String addr = preferences.getString("server_addr", "");
        String port = preferences.getString("server_port", "8090");

        url = "http://" + addr + ":" + port + "/json-rpc";

        colorPickerView = findViewById(R.id.colorPickerView);
    }

    private void setColor(int[] val) {
        JSONArray colorObject = new JSONArray();
        JSONObject responseObject = new JSONObject();

        try {
            colorObject.put(val[1]);
            colorObject.put(val[2]);
            colorObject.put(val[3]);

            responseObject.put("command", "color");
            responseObject.put("color", colorObject);
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

    @Override
    protected void onResume() {
        super.onResume();
        colorPickerView.setColorListener((ColorEnvelopeListener) (envelope, fromUser) -> {
            if (fromUser) {
                setColor(envelope.getArgb());
            }
        });
    }
}