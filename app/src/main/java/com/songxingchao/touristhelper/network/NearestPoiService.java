package com.songxingchao.touristhelper.network;

import com.songxingchao.touristhelper.models.GooglePoiResult;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by songxingchao on 15/10/2016.
 */
//json?location=-33.8075196,150.9755109&radius=500&type=restaurant&keyword=cruise&key=AIzaSyD05qm7lDK8BCMrueXWFAZ5atO5_R80nXg
public interface NearestPoiService {
    @GET("json")
    Observable<GooglePoiResult> getNearstPoi(
            @Query("location") String location,
            @Query("radius") String radius,
            @Query("key") String key

    );
}
