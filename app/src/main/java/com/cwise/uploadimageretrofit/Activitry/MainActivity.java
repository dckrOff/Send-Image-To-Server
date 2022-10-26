package com.cwise.uploadimageretrofit.Activitry;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cwise.uploadimageretrofit.Utils.RealPathUtil;
import com.cwise.uploadimageretrofit.Service.RetrofitHost;
import com.cwise.uploadimageretrofit.Service.ApiService;
import com.cwise.uploadimageretrofit.databinding.ActivityMainBinding;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    ActivityMainBinding binding;
    String path;
    private Uri imageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        clickListeners();
    }

    private void clickListeners() {
        binding.selectImageFromGallery.setOnClickListener(v -> {
            // after pressing the button, a gallery opens for selecting a photo
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 0);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        });

        binding.save.setOnClickListener(v -> sendFile());
        binding.selectImageFromCamera.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            } else {
//                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(cameraIntent, 1);

                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "New Picture");
                values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
                imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Есть разрешение на камеру", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 1);
            } else {
                Toast.makeText(this, "Нет разрешения на камеру", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // after selecting a photo, it is set to imageView and its path is obtained
        if (requestCode == 0 && resultCode == RESULT_OK) {
            assert data != null;
            Uri uri = data.getData();
            path = RealPathUtil.getRealPath(this, uri);
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            binding.imageview.setImageBitmap(bitmap);
        } else if (requestCode == 1 && resultCode == RESULT_OK) {
            try {
                Bitmap photo = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                binding.imageview.setImageBitmap(photo);
                path = getRealPathFromURI(imageUri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public void sendFile() {
        // get file by its path
        File file = new File(path);

        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);

        // setting image to body
        MultipartBody.Part image = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        // adding more options
//      RequestBody cus_name = RequestBody.create(MediaType.parse("multipart/form-data"), name);
//      RequestBody cus_reference = RequestBody.create(MediaType.parse("multipart/form-data"), refernce);

        // getting API for request
        ApiService apiService = RetrofitHost.getRetrofitInstance().create(ApiService.class);
        Call<String> call = apiService.uploadImage(image);

        Log.e("--TAG--", "call-> " + call.request().url());
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                Log.e("--TAG--", "response code-> " + response.code());
                Log.e("--TAG--", "response body-> " + response.body());
                Log.e("--TAG--", "response msg-> " + response.message());
                Log.e("--TAG--", "response is scfl-> " + response.isSuccessful());
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Toast.makeText(getApplicationContext(), t.toString(), Toast.LENGTH_SHORT).show();
                Log.e("--TAG--", "onFailure-> " + t.getMessage());
            }
        });
    }
}