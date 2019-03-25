package arnavigation.appsan.com.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import java.util.List;

import arnavigation.appsan.com.myapplication.utils.PermissionCheck;
import arnavigation.appsan.com.myapplication.utils.UtilsCheck;
import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, OnConnectionFailedListener
        , ConnectionCallbacks, GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener {

    private final static String TAG = "MapsActivity";

    SharedPreferences getPrefs;
    boolean isFirstStart;

    private GoogleApiClient googleApiClient;
    private GoogleMap mMap;

  //  private Location location;

    private Marker RevMarker;

    @BindView(R.id.fab_menu_btn)
    FloatingActionMenu fab_menu;
    @BindView(R.id.ar_nav_btn)
    com.github.clans.fab.FloatingActionButton ar_nav_btn;
    @BindView(R.id.poi_browser_btn)
    com.github.clans.fab.FloatingActionButton poi_browser_btn;
    @BindView(R.id.decode_box)
    EditText decode_editText;
    @BindView(R.id.decode_btn)
    Button decode_button;
    @BindView(R.id.progressBar_maps)
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        // startActivity(intent);
        ButterKnife.bind(this);

        //start the intro activity
        Init_intro();
        //check all permissions
        PermissionCheck.initialPermissionCheckAll(this, this);

        //init progress bar, set it to hidden
        progressBar.setVisibility(View.GONE);

        //check and indicate internet connectivity
        if (!UtilsCheck.isNetworkConnected(this)) {
            Snackbar mySnackbar = Snackbar.make(findViewById(R.id.main_content),
                    "Turn Internet On", Snackbar.LENGTH_SHORT);
            mySnackbar.show();
        }


        //initialize google API
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }


        //Navigation button (Opens the select navigation mode where user selects the start and end locations)
        ar_nav_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapsActivity.this, NavActivity.class);
                startActivity(intent);
            }
        });
        //POI browser button(Open POIBrowserActivity)
        poi_browser_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapsActivity.this, PoiBrowserActivity.class);
                startActivity(intent);
            }
        });


//search bar button(Which is removed now from public view)
        decode_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (TextUtils.isEmpty(decode_editText.getText())) {
                        Snackbar mySnackbar = Snackbar.make(findViewById(R.id.main_content),
                                "Search Field is Empty", Snackbar.LENGTH_SHORT);
                        mySnackbar.show();
                    } else {
                        Geocode_Call(decode_editText.getText().toString());
                    }
                } catch (NullPointerException npe) {
                    Snackbar mySnackbar = Snackbar.make(findViewById(R.id.main_content),
                            "Search Field is Empty", Snackbar.LENGTH_SHORT);
                    mySnackbar.show();
                }
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    //get coordinates from location information
    void Geocode_Call(String address) {
        /*
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        progressBar.setVisibility(View.VISIBLE);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.directions_base_url))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitInterface apiService =
                retrofit.create(RetrofitInterface.class);

        final Call<GeocodeResponse> call = apiService.getGecodeData(address,
                getResources().getString(R.string.google_maps_key));

        call.enqueue(new Callback<GeocodeResponse>() {
            @Override
            public void onResponse(Call<GeocodeResponse> call, Response<GeocodeResponse> response) {

                progressBar.setVisibility(View.GONE);

                List<arnavigation.appsan.com.myapplication.network.geocode.Result> results = response.body().getResults();
                location = results.get(0).getGeometry().getLocation();
                Toast.makeText(MapsActivity.this, location.getLat() + "," + location.getLng(), Toast.LENGTH_SHORT).show();

                try {
                    mMap.clear();
                    LatLng loc = new LatLng(location.getLat(), location.getLng());
                    mMap.addMarker(new MarkerOptions()
                            .position(loc)
                            .title(results.get(0).getFormattedAddress())
                            .snippet(results.get(0).getGeometry().getLocationType()));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 14.0f));
                    mMap.getUiSettings().setMapToolbarEnabled(false);
                    //decode_button.setBackground(getDrawable());

//                    mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
//                        @Override
//                        public boolean onMarkerClick(Marker marker) {
//                            if(marker.isInfoWindowShown())
//                                fab_menu.hideMenu(true);
//                            else
//                                fab_menu.hideMenu(false);
//                            return false;
//                        }
//                    });
                } catch (NullPointerException npe) {
                    Log.d(TAG, "onMapReady: Location is NULL");
                }
            }

            @Override
            public void onFailure(Call<GeocodeResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MapsActivity.this, "Invalid Request", Toast.LENGTH_SHORT).show();
            }
        });
*/
    }

    //Reverse geocoding(Get location information from coordinates)
    void Rev_Geocode_Call(LatLng latlng) {

//show the spinning progressbar
        progressBar.setVisibility(View.VISIBLE);
//initialize mapbox API
        MapboxGeocoding reverseGeocode = MapboxGeocoding.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .query(Point.fromLngLat(latlng.longitude, latlng.latitude))
                .geocodingTypes(GeocodingCriteria.TYPE_POI)
                .mode(GeocodingCriteria.MODE_PLACES)
                .build();


        //Get location information from mapbox
        reverseGeocode.enqueueCall(new Callback<GeocodingResponse>() {

            /**
             * Location call is successful

             */
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                //Receive the location information from the mapbox
                progressBar.setVisibility(View.GONE);
                List<CarmenFeature> results = response.body().features();

                if (results.size() > 0) {

                    // Log the first results Point.
                    String pn = results.get(0).text();
                    Log.d(TAG, "onResponse: " + pn);

                    Toast.makeText(MapsActivity.this, pn, Toast.LENGTH_LONG).show();

                    RevMarker.setTitle(pn);
                    RevMarker.setSnippet(results.get(0).geometry().type());

                } else {

                    // No result for your request were found.
                    Log.d(TAG, "onResponse: No result found");

                }

            }

            /**
             * The location call is not successful
             *

             */
            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MapsActivity.this, "Invalid Request", Toast.LENGTH_SHORT).show();
            }
        });


    }

    /**
     * To start the intro(Welcome) activity
     */
    void Init_intro() {

        getPrefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());

        //  Create a new boolean and preference and set it to true
        // isFirstStart = getPrefs.getBoolean("firstStart", true);
        isFirstStart = true;
        //  Declare a new thread to do a preference check
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {


                //  Launch app intro
                Intent i = new Intent(MapsActivity.this, WelcomeActivity.class);
                startActivity(i);

                //  Make a new preferences editor
                SharedPreferences.Editor e = getPrefs.edit();

                //  Edit preference to make it false because we don't want this to run again
                e.putBoolean("firstStart", false);

                //  Apply changes
                e.apply();

            }

        });
        //check if the app is running for the first time
        if (isFirstStart) {
            t.start();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 10: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public void onMapLongClick(LatLng latLng) {

        mMap.clear();
        RevMarker = mMap.addMarker(new MarkerOptions().position(latLng));
        Toast.makeText(this, latLng.latitude + " " + latLng.longitude, Toast.LENGTH_SHORT).show();
        Rev_Geocode_Call(latLng);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }



        mMap.setOnMapLongClickListener(this);

        //restrict the map to the KFU only
        mMap.setLatLngBoundsForCameraTarget(new LatLngBounds(new LatLng(25.327515, 49.598666), new LatLng(25.347917, 49.600547)));
        mMap.setBuildingsEnabled(true);

        mMap.setMinZoomPreference(15);
        //mMap.setOnMapClickListener(this);
        Log.d(TAG, "onMapReady: MAP IS READY");

    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //PermissionCheck.initialPermissionCheck(this,this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "onMapClick: Short Click " + latLng.toString());
    }
}
