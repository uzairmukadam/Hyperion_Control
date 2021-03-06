package com.uzitech.hyperioncontrol;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.uzitech.hyperioncontrol.Support.PriorityListAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class Dashboard extends AppCompatActivity implements PriorityListAdapter.onButtonClick {

    SharedPreferences preferences;

    String url;

    boolean first_run = true, connected = false;
    SeekBar brightness_control;
    ImageButton set_color, set_effect, set_image, configuration;
    RecyclerView priority_list;
    TextView no_source_msg;

    TextView no_connection;

    JSONArray priorities, effects;
    PriorityListAdapter priorityListAdapter;

    JSONObject serverInfo, infoCommand;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(750, TimeUnit.MILLISECONDS)
                .build();

        AndroidNetworking.initialize(this, okHttpClient);

        preferences = getSharedPreferences(getString(R.string.pref_data), Context.MODE_PRIVATE);

        if (!preferences.contains("server_addr") || preferences.getString("server_addr", "").equals("")) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
        } else {
            String addr = preferences.getString("server_addr", "");
            String port = preferences.getString("server_port", "8090");

            url = "http://" + addr + ":" + port + "/json-rpc";

            try {
                infoCommand = new JSONObject();
                infoCommand.put("command", "serverinfo");
            } catch (Exception e) {
                e.printStackTrace();
            }

            brightness_control = findViewById(R.id.brightness_control);
            set_color = findViewById(R.id.color_mode);
            set_effect = findViewById(R.id.effect_mode);
            set_image = findViewById(R.id.image_mode);
            configuration = findViewById(R.id.config_mode);
            priority_list = findViewById(R.id.priority_list);
            no_source_msg = findViewById(R.id.no_source_msg);
            no_connection = findViewById(R.id.no_connection);

            brightness_control.setMax(100);
            brightness_control.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        if (connected) {
                            changeBrightness(progress);
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            set_color.setOnClickListener(v -> {
                if (connected) {
                    startActivity(new Intent(getApplicationContext(), SetColorActivity.class));
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                }
            });
            set_effect.setOnClickListener(v -> {
                if (connected) {
                    String data = effects.toString();
                    Intent effects = new Intent(getApplicationContext(), SetEffectActivity.class);
                    effects.putExtra("list", data);
                    startActivity(effects);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                }
            });
            set_image.setOnClickListener(v -> {
                if (connected) {
                    ImagePicker.with(this)
                            .crop()
                            .compress(1024)
                            .maxResultSize(120, 68)
                            .start();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                }
            });
            configuration.setOnClickListener(v -> {
                if (connected) {
                    Intent effects = new Intent(getApplicationContext(), ConfigurationActivity.class);
                    startActivity(effects);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                }
            });

            priorities = new JSONArray();
            priorityListAdapter = new PriorityListAdapter(this, priorities, this);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
            priority_list.setLayoutManager(mLayoutManager);
            priority_list.setItemAnimator(new DefaultItemAnimator());
            priority_list.setAdapter(priorityListAdapter);

            getServerInfo();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updatePriorityList() {
        if (priorities.length() > 0) {
            no_source_msg.setVisibility(View.GONE);
            priority_list.setVisibility(View.VISIBLE);

            priorityListAdapter.setItem(priorities);
            priorityListAdapter.notifyDataSetChanged();
        } else {
            no_source_msg.setVisibility(View.VISIBLE);
            priority_list.setVisibility(View.GONE);
        }
    }

    private boolean updatePriorities(JSONArray array) throws JSONException {
        if (array.length() == priorities.length()) {
            boolean result = false;
            for (int i = 0; i < array.length(); i++) {
                JSONObject object1 = priorities.getJSONObject(i);
                JSONObject object2 = array.getJSONObject(i);

                HashMap<String, Object> map1 = new HashMap<>();
                HashMap<String, Object> map2 = new HashMap<>();

                map1.put("origin", object1.getString("origin"));
                map1.put("componentId", object1.getString("componentId"));
                map1.put("priority", object1.getInt("priority"));
                map1.put("visible", object1.getString("visible"));

                map2.put("origin", object2.getString("origin"));
                map2.put("componentId", object2.getString("componentId"));
                map2.put("priority", object2.getInt("priority"));
                map2.put("visible", object2.getString("visible"));

                if (!map1.equals(map2)) {
                    result = true;
                    break;
                }
            }
            return result;
        } else {
            return true;
        }
    }

    private void processData() {
        try {
            if (first_run) {
                first_run = false;
                effects = serverInfo.getJSONObject("info").getJSONArray("effects");
                priorities = serverInfo.getJSONObject("info").getJSONArray("priorities");
                int brightness = serverInfo.getJSONObject("info").getJSONArray("adjustment").getJSONObject(0).getInt("brightness");
                brightness_control.setProgress(brightness);
                updatePriorityList();
            } else {
                if (updatePriorities(serverInfo.getJSONObject("info").getJSONArray("priorities"))) {
                    priorities = serverInfo.getJSONObject("info").getJSONArray("priorities");
                    updatePriorityList();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        getServerInfo();
    }

    private void getServerInfo() {
        AndroidNetworking.post(url)
                .addJSONObjectBody(infoCommand)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        connected = true;
                        no_connection.setVisibility(View.GONE);
                        serverInfo = response;
                        processData();
                    }

                    @Override
                    public void onError(ANError error) {
                        if (error.getErrorCode() == 0) {
                            connected = false;
                            no_connection.setVisibility(View.VISIBLE);
                            getServerInfo();
                        }
                    }
                });
    }

    private void sendRequest(JSONObject responseObject) {
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

    private void changeBrightness(int val) {
        JSONObject object = new JSONObject();
        JSONObject brightnessObject = new JSONObject();
        try {
            object.put("command", "adjustment");

            brightnessObject.put("brightness", val);

            object.put("adjustment", brightnessObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendRequest(object);
    }

    private void setImage(String val) {
        JSONObject responseObject = new JSONObject();

        try {
            responseObject.put("command", "image");
            responseObject.put("imagedata", val);
            responseObject.put("name", "Set from controller");
            responseObject.put("format", "auto");
            responseObject.put("priority", 50);
            responseObject.put("origin", getString(R.string.app_name));
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendRequest(responseObject);
    }

    @Override
    public void stopSource(int priority) {
        JSONObject object = new JSONObject();
        try {
            object.put("command", "clear");
            object.put("priority", priority);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendRequest(object);
    }

    @Override
    public void selectSource(int priority) {
        JSONObject object = new JSONObject();
        try {
            object.put("command", "sourceselect");
            object.put("priority", priority);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendRequest(object);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            assert data != null;
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();

                String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
                setImage(encoded);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}