package com.uzitech.hyperioncontrol;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ConfigurationActivity extends AppCompatActivity {

    SharedPreferences preferences;

    TextView ip_addr;
    Button diss_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        preferences = getSharedPreferences(getString(R.string.pref_data), Context.MODE_PRIVATE);

        ip_addr = findViewById(R.id.ip_addr);
        diss_button = findViewById(R.id.diss_btn);

        ip_addr.setText(preferences.getString("server_addr", ""));
        diss_button.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light, null));

        diss_button.setOnClickListener(v -> disconnect());
    }

    private void disconnect() {
        Intent intent = new Intent(getApplicationContext(), SetupActivity.class);

        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();

        if (editor.commit()) {
            finishAffinity();
            startActivity(intent);
        }
    }
}