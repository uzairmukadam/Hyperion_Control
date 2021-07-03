package com.uzitech.hyperioncontrol;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.github.dhaval2404.imagepicker.ImagePicker;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class SetImageActivity extends AppCompatActivity {

    Activity activity;

    SharedPreferences preferences;

    String url;

    ImageButton addImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_image);

        activity = this;

        preferences = getSharedPreferences(getString(R.string.pref_data), Context.MODE_PRIVATE);

        String addr = preferences.getString("server_addr", "");
        String port = preferences.getString("server_port", "8090");

        url = "http://" + addr + ":" + port + "/json-rpc";

        addImg = findViewById(R.id.add_img);

        addImg.setOnClickListener(v -> ImagePicker.with(activity)
                .crop()
                .compress(1024)
                .maxResultSize(120, 68)
                .start());
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
            finish();
        } else {
            Toast.makeText(this, getText(R.string.something_wrong), Toast.LENGTH_SHORT).show();
        }
    }
}