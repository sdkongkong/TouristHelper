package com.songxingchao.touristhelper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.songxingchao.touristhelper.models.GooglePoiResult;
import com.songxingchao.touristhelper.network.RxClient;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MapDemoActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, ActivityCompat.OnRequestPermissionsResultCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMapLongClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private boolean mPermissionDenied = false;
    private String TAG = this.getClass().getName();
    private GoogleApiClient mGoogleApiClient;
    private GooglePoiResult mGooglePoiResult;
    private List<LatLng> mPoints = new ArrayList<>();
    private Polygon mPolygon;
    private Marker mMySelectMarker;
    private Subscription mSubGetSortedList;
    private Subscription mSubGetPoiList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_demo);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMyLocationButtonClickListener(this);
        enableMyLocation();
        mMap.setOnMapLongClickListener(this);
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
            mGoogleApiClient.connect();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }
        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();

        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
            ArrayList arr;

        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }


    @Override
    protected void onDestroy() {
        mGoogleApiClient.disconnect();
        if (mSubGetPoiList != null) {
            mSubGetPoiList.unsubscribe();
        }
        if (mSubGetSortedList != null) {
            mSubGetSortedList.unsubscribe();
        }
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //get current location
        Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.d(TAG, currentLocation.getLatitude() + "");
        //scale the map base on your current location
        LatLng coordinate = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 12);
        mMap.animateCamera(yourLocation);
        //get 20 interest of points
        mSubGetPoiList = RxClient.getInstance().getNearestPoi(currentLocation.getLatitude() + "," + currentLocation.getLongitude(), "1500", getString(R.string.google_maps_key))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GooglePoiResult>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "completed:");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "error");
                    }

                    @Override
                    public void onNext(GooglePoiResult googlePoiResult) {
                        mGooglePoiResult = googlePoiResult;
                        Log.d("name", googlePoiResult.results.get(1).name + "::::::" + googlePoiResult.results.get(2).name);
                        addMarker(googlePoiResult);
                        Log.d("name", "distance between 1 and 2 is :" + getDistance(mPoints.get(1), mPoints.get(2)) + "");
                    }
                });

    }

    private void addMarker(GooglePoiResult result) {
        for (GooglePoiResult.GooglePoi poi : result.results) {
            Log.d(TAG, "size:" + poi.geometry.location.lat + ":" + poi.geometry.location.lng);
            Log.d(TAG, "name:" + poi.name);
            Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(poi.geometry.location.lat, poi.geometry.location.lng)).title(poi.name));
        }


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (mMySelectMarker != null) {
            mMySelectMarker.remove();
        }
        for (GooglePoiResult.GooglePoi poi : mGooglePoiResult.results) {
            mPoints.add(new LatLng(poi.geometry.location.lat, poi.geometry.location.lng));
        }
        mMySelectMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("your select choice"));
        drawPolyGon(latLng);
    }

    /**
     * draw the ploygon useing the sorted array of location
     *
     * @param selectedPoint
     */
    private void drawPolyGon(final LatLng selectedPoint) {
        if (mPolygon != null) {
            mPolygon.remove();
        }
        // caculation may take some time base on the size of list , so put it in background thread using RxJava
        mSubGetSortedList = Single.fromCallable(new Callable<List<LatLng>>() {
            @Override
            public List<LatLng> call() throws Exception {
                return getShortestLine(mPoints, selectedPoint);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<List<LatLng>>() {
                    @Override
                    public void onSuccess(List<LatLng> list) {
                        PolygonOptions rectOptions = new PolygonOptions();
                        for (LatLng poi : list) {
                            rectOptions.add(poi);
                        }
                        mPolygon = mMap.addPolygon(rectOptions);
                    }

                    @Override
                    public void onError(Throwable error) {
                        Log.d(TAG, "error on getshort list:" + error.getMessage());
                    }
                });
    }

    /**
     * sort the array by just finding the nearest the location as the next location
     *
     * @param points        the location array list
     * @param selectedPoint the location which you selected and would be added to the array
     * @return sorted array
     */
    private List<LatLng> getShortestLine(List<LatLng> points, LatLng selectedPoint) {
        List<LatLng> sortedList = new ArrayList<>();
        sortedList.add(selectedPoint);

        while (points.size() > 0) {
            if (points.size() == 1) {
                sortedList.addAll(points);
                break;
            }
            LatLng lastAddedPoint = selectedPoint;
            int selectedIndex = 0;
            double distance = getDistance(lastAddedPoint, points.get(0));
            for (int i = 0; i < points.size(); i++) {
                double tempDistance = getDistance(points.get(i), lastAddedPoint);
                if (tempDistance < distance) {
                    distance = tempDistance;
                    selectedIndex = i;
                }
            }
            selectedPoint = points.get(selectedIndex);
            points.remove(selectedPoint);
            sortedList.add(selectedPoint);
        }
        return sortedList;
    }

    /**
     * calculate the distance between to location
     *
     * @param locationA first location
     * @param locationB second location
     * @return calculated distance
     */
    private double getDistance(LatLng locationA, LatLng locationB) {
//        float[] disantce = new float[1];
//        //double startLatitude, double startLongitude, double endLatitude, double endLongitude, float[] results
//        Location.distanceBetween(locationA.latitude,locationA.longitude,locationB.latitude,locationB.longitude,disantce);
//        return disantce[0];
        double xDistance = locationB.latitude - locationA.latitude;
        double yDistance = locationB.longitude - locationA.longitude;
        double distance = Math.sqrt(xDistance * xDistance + yDistance * yDistance);
        Log.d("distance", "locationB.latitude:" + locationB.latitude + " locationA.latitude:" + locationA.latitude + "locationB.longitude:" + locationB.longitude + " locationA.longitude:" + locationA.longitude + " distance:" + distance);
        return distance;
    }


}
