package com.uzitech.hyperioncontrol;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONObject;

import java.util.ArrayList;

public class SetupActivity extends AppCompatActivity {

    SharedPreferences preferences;
    EditText server_addr, server_port;
    ArrayList<EditText> input_field;
    Button connect;
    ProgressBar connecting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        AndroidNetworking.initialize(this);

        preferences = getSharedPreferences(getString(R.string.pref_data), Context.MODE_PRIVATE);

        input_field = new ArrayList<>();

        server_addr = findViewById(R.id.server_addr_in);
        server_port = findViewById(R.id.server_port_in);

        input_field.add(server_addr);
        input_field.add(server_port);

        connect = findViewById(R.id.connect_btn);

        connect.setOnClickListener(v -> {
            if (checkInput()) {
                disableButton();
                checkServer();
            }
        });

        connecting = findViewById(R.id.connecting);
    }

    private boolean checkInput() {
        boolean check = true;
        for (EditText input : input_field) {
            if (input.getText().toString().equals("")) {
                check = false;
                Toast.makeText(this, getString(R.string.input_error), Toast.LENGTH_SHORT).show();
                input.requestFocus();
                break;
            }
        }
        return check;
    }

    private void checkServer() {
        String addr, port;

        addr = server_addr.getText().toString();
        port = server_port.getText().toString();

        String url = "http://" + addr + ":" + port + "/json-rpc";
        JSONObject responseObject = new JSONObject();
        try {
            responseObject.put("command", "serverinfo");
        } catch (Exception e) {
            e.printStackTrace();
        }

        AndroidNetworking.post(url)
                .addJSONObjectBody(responseObject)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        addServer(addr, port);
                    }

                    @Override
                    public void onError(ANError error) {
                        if (error.getErrorCode() == 0) {
                            Toast.makeText(getApplicationContext(), getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                        }
                        enableButton();
                    }
                });
    }

    private void disableButton() {
        connect.setVisibility(View.GONE);
        connecting.setVisibility(View.VISIBLE);
    }

    private void enableButton() {
        connect.setVisibility(View.VISIBLE);
        connecting.setVisibility(View.GONE);
    }

    private void addServer(String addr, String port) {
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("server_addr", addr);
        editor.putString("server_port", port);

        editor.apply();

        startActivity(new Intent(this, Dashboard.class));
        finish();
    }
}