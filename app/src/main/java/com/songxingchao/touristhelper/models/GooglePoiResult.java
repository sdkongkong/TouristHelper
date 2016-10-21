package com.songxingchao.touristhelper.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songxingchao on 15/10/2016.
 */

public class GooglePoiResult {
    public List<GooglePoi> results;


    public class GooglePoi {
        public String name;
        public String id;
        public String icon;
        public String place_id;
        public GeoMetry geometry;
    }

    public class GeoMetry {
        public Location location;
    }

    public class Location {
        public double lat;
        public double lng;
    }

}
