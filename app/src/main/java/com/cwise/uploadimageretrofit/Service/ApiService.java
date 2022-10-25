package com.cwise.uploadimageretrofit.Service;

import com.cwise.uploadimageretrofit.Model.AddCustomerRes;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    // API with other options
    @Multipart
    @POST("/add_customer")
    Call<AddCustomerRes> addCustomer(@Part MultipartBody.Part image,
                                     @Part("customer_name") RequestBody customername,
                                     @Part("reference") RequestBody refernce);

    // API with only one file
    @Multipart
    @POST("/upload_image")
    Call<String> uploadImage(@Part MultipartBody.Part image);
}
