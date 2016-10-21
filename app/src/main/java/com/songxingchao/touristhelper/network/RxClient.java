package com.songxingchao.touristhelper.network;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.songxingchao.touristhelper.models.GooglePoiResult;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;

/**
 * Created by songxingchao on 25/09/2016.
 */
public class RxClient {
    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/";
    private static RxClient instance;
    private NearestPoiService nearestPoiService;

    private RxClient() {
        final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder().addInterceptor(loggingInterceptor).build();

        final Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();

        nearestPoiService = retrofit.create(NearestPoiService.class);


    }

    public static RxClient getInstance() {
        if (instance == null) {
            instance = new RxClient();
        }
        return instance;
    }

    public Observable<GooglePoiResult> getNearestPoi(String location, String radius, String apiKey){
      return nearestPoiService.getNearstPoi(location,radius,apiKey);
    }



}
