package arnavigation.appsan.com.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.beyondar.android.util.location.BeyondarLocationManager;
import com.beyondar.android.world.BeyondarObject;
import com.beyondar.android.world.GeoObject;
import com.beyondar.android.world.World;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.ArrayList;
import java.util.List;

import arnavigation.appsan.com.myapplication.ar.ArFragmentSupport;
import arnavigation.appsan.com.myapplication.ar.MyMarker;
import arnavigation.appsan.com.myapplication.utils.LocationCalc;
import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.appoly.arcorelocation.utils.LocationUtils;

public class ArCamActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    @BindView(R.id.ar_source_dest)
    TextView srcDestText;
    @BindView(R.id.ar_dir_distance)
    TextView dirDistance;
    @BindView(R.id.ar_dir_time)
    TextView dirTime;
    @BindView(R.id.speed)
    TextView speed;

    private ArrayList<MyMarker> pois = new ArrayList<>();
    //Points with coordinates taken from Google maps


    private final static String TAG = "ArCamActivity";
    private String srcLatLng;
    private String destLatLng;
    private LegStep steps[];

    Point srcll;
    Point destll;

    private LocationManager locationManager;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private ArFragmentSupport arFragmentSupport;
    private World world;

    private Intent intent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_camera);
        ButterKnife.bind(this);

        Button poibtn = findViewById(R.id.poi_btn);
        poibtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),PoiBrowserActivity.class);
                startActivity(intent);
                finish();


            }
        });

        Set_googleApiClient(); //Sets the GoogleApiClient


    }

    //initialize the google API
    private void Set_googleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    /**
     * Start the AR navigation
     */
    private void Configure_AR() {
        List<List<LatLng>> polylineLatLng = new ArrayList<>();

        //beyondAR world
        world = new World(getApplicationContext());

        //the the center position of the world to your location
        world.setGeoPosition(mLastLocation.getLatitude(), mLastLocation.getLongitude());

        Log.d(TAG, "Configure_AR: LOCATION" + mLastLocation.getLatitude() + " " + mLastLocation.getLongitude());
        //set default arrow image
        world.setDefaultImage(R.drawable.ar_sphere_default);

        //Select the AR fragment from the interface
        arFragmentSupport = (ArFragmentSupport) getSupportFragmentManager().findFragmentById(
                R.id.ar_cam_fragment);


        Log.d(TAG, "Configure_AR: STEP.LENGTH:" + steps.length);
        //TODO The given below is for rendering MAJOR STEPS LOCATIONS
        for (int i = 0; i < steps.length; i++) {
            //Add the path point locations taken from mapbox the the polylines array
            ArrayList<LatLng> locs = new ArrayList<>();
            for (StepIntersection inst : steps[i].intersections()) {
                locs.add(new LatLng(inst.location().latitude(), inst.location().longitude()));
            }
            polylineLatLng.add(i, locs);

            //Take the next instruction string (Example: turn left)
            String instructions = steps[i].maneuver().instruction();

            //if the point is the first point on the path
            if (i == 0) {
                //create an object and set its position the position of the point and its image
                GeoObject signObject = new GeoObject(10000 + i);
                signObject.setImageResource(R.drawable.start);
                signObject.setGeoPosition(steps[i].intersections().get(0).location().latitude(), steps[i].intersections().get(0).location().longitude());
                //place the object in the Beyondar world
                world.addBeyondarObject(signObject);
                Log.d(TAG, "Configure_AR: START SIGN:" + i);
            }

            //if the point is the last point on the path
            if (i == steps.length - 1) {
                GeoObject signObject = new GeoObject(10000 + i);
                signObject.setImageResource(R.drawable.stop);
                //calculate the location of the last point of the path
                LatLng latlng = SphericalUtil.computeOffset(
                        new LatLng(steps[i].intersections().get(steps[i].intersections().size() - 1).location().latitude(), steps[i].intersections().get(steps[i].intersections().size() - 1).location().longitude()),
                        4f, SphericalUtil.computeHeading(
                                new LatLng(steps[i].intersections().get(0).location().latitude(), steps[i].intersections().get(0).location().longitude()),
                                new LatLng(steps[i].intersections().get(steps[i].intersections().size() - 1).location().latitude(), steps[i].intersections().get(steps[i].intersections().size() - 1).location().longitude())));
                signObject.setGeoPosition(latlng.latitude, latlng.longitude);
                world.addBeyondarObject(signObject);
                Log.d(TAG, "Configure_AR: STOP SIGN:" + i);
            }


            //if the instruction is telling to turn right, display an arrow that is pointing right
            if (instructions.contains("right")) {
                Log.d(TAG, "Configure_AR: " + instructions);
                GeoObject signObject = new GeoObject(10000 + i);
signObject.setName("Right");
                signObject.setImageResource(R.drawable.turn_right);
                signObject.setGeoPosition(steps[i].intersections().get(0).location().latitude(), steps[i].intersections().get(0).location().longitude());
                world.addBeyondarObject(signObject);


                Log.d(TAG, "Configure_AR: RIGHT SIGN:" + i);
                //if the instruction is telling to turn left, display an arrow that is pointing left
            } else if (instructions.contains("left")) {
                Log.d(TAG, "Configure_AR: " + instructions);
                GeoObject signObject = new GeoObject(10000 + i);
                signObject.setName("Left");
                signObject.setImageResource(R.drawable.turn_left);
                signObject.setGeoPosition(steps[i].intersections().get(0).location().latitude(), steps[i].intersections().get(0).location().longitude());
                world.addBeyondarObject(signObject);
                Log.d(TAG, "Configure_AR: LEFT SIGN:" + i);

            }
        }

        int temp_polycount = 0;
        int temp_inter_polycount = 0;

        //Display the rest of the arrows between the ones displayed before(Start, end, lefts and rights)
        for (int j = 0; j < polylineLatLng.size(); j++) {
            for (int k = 0; k < polylineLatLng.get(j).size(); k++) {
                GeoObject polyGeoObj = new GeoObject(1000 + temp_polycount++);
                int pointing = R.drawable.ar_sphere_150x;
                int pointing2 = R.drawable.ar_sphere_default_125x;

                polyGeoObj.setGeoPosition(polylineLatLng.get(j).get(k).latitude,
                        polylineLatLng.get(j).get(k).longitude);
                polyGeoObj.setImageResource(pointing);
                polyGeoObj.setName("arObj" + j + k);

                /*
                To fill the gaps between the Poly objects as AR Objects in the AR View , add some more
                AR Objects which are equally spaced and provide a continuous AR Object path along the route

                Haversine formula , Bearing Calculation and formula to find
                Destination point given distance and bearing from start point is used .
                 */

                try {

                    //Initialize distance of consecutive polyobjects
                    double dist = LocationCalc.haversine(polylineLatLng.get(j).get(k).latitude,
                            polylineLatLng.get(j).get(k).longitude, polylineLatLng.get(j).get(k + 1).latitude,
                            polylineLatLng.get(j).get(k + 1).longitude) * 1000;

                    //Log.d(TAG, "Configure_AR: polyLineLatLng("+j+","+k+")="+polylineLatLng.get(j).get(k).latitude+","+polylineLatLng.get(j).get(k).longitude);
                    //Log.d(TAG, "Configure_AR: polyLineLatLng("+j+","+(k+1)+")="+polylineLatLng.get(j).get(k+1).latitude+","+polylineLatLng.get(j).get(k+1).longitude);

                    //Check if distance between polyobjects is greater than twice the amount of space
                    // intended , here it is (3*2)=6 .
                    if (dist > 6) {

                        //Initialize count of ar objects to be added
                        int arObj_count = ((int) dist / 3) - 1;

                        //Log.d(TAG, "Configure_AR: Dist:" + dist + " # No of Objects: " + arObj_count + "\n --------");

                        double bearing = LocationCalc.calcBearing(polylineLatLng.get(j).get(k).latitude,
                                polylineLatLng.get(j).get(k + 1).latitude,
                                polylineLatLng.get(j).get(k).longitude,
                                polylineLatLng.get(j).get(k + 1).longitude);

                        double heading = SphericalUtil.computeHeading(new LatLng(polylineLatLng.get(j).get(k).latitude,
                                        polylineLatLng.get(j).get(k).longitude),
                                new LatLng(polylineLatLng.get(j).get(k + 1).latitude,
                                        polylineLatLng.get(j).get(k + 1).longitude));

                        LatLng tempLatLng = SphericalUtil.computeOffset(new LatLng(polylineLatLng.get(j).get(k).latitude,
                                        polylineLatLng.get(j).get(k).longitude)
                                , 3f
                                , heading);

                        //The distance to be incremented
                        double increment_dist = 3f;

                        for (int i = 0; i < arObj_count; i++) {
                            GeoObject inter_polyGeoObj = new GeoObject(5000 + temp_inter_polycount++);

                            //Store the Lat,Lng details into new LatLng Objects using the functions
                            //in LocationCalc class.
                            if (i > 0 && k < polylineLatLng.get(j).size()) {
                                increment_dist += 3f;

                                tempLatLng = SphericalUtil.computeOffset(new LatLng(polylineLatLng.get(j).get(k).latitude,
                                                polylineLatLng.get(j).get(k).longitude),
                                        increment_dist,
                                        SphericalUtil.computeHeading(new LatLng(polylineLatLng.get(j).get(k).latitude
                                                , polylineLatLng.get(j).get(k).longitude), new LatLng(
                                                polylineLatLng.get(j).get(k + 1).latitude
                                                , polylineLatLng.get(j).get(k + 1).longitude)));

                                double hdg = SphericalUtil.computeHeading(new LatLng(polylineLatLng.get(j).get(k).latitude
                                        , polylineLatLng.get(j).get(k).longitude), new LatLng(
                                        polylineLatLng.get(j).get(k + 1).latitude
                                        , polylineLatLng.get(j).get(k + 1).longitude));
                                if (hdg >= 0) {
                                    //TODO Heading to positive direction, show propper arrows pointing right
                                    pointing2 = R.drawable.ar_sphere_default_right_125x;
                                } else {
                                    //TODO Heading to negative direction, show propper arrows pointing left
                                    pointing2 = R.drawable.ar_sphere_default_125x;
                                }
                            }


                            //Set the Geoposition along with image and name
                            inter_polyGeoObj.setGeoPosition(tempLatLng.latitude, tempLatLng.longitude);
                            inter_polyGeoObj.setImageResource(pointing2);
                            inter_polyGeoObj.setName("inter_arObj" + j + k + i);

                            //Log.d(TAG, "Configure_AR: LOC: k="+k+" "+ inter_polyGeoObj.getLatitude() + "," + inter_polyGeoObj.getLongitude());

                            //Add Intermediate ArObjects to Augmented Reality World
                            world.addBeyondarObject(inter_polyGeoObj);
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Configure_AR: EXCEPTION CAUGHT:" + e.getMessage());
                }

                //Add PolyObjects as ArObjects to Augmented Reality World
                world.addBeyondarObject(polyGeoObj);
                Log.d(TAG, "\n\n");
            }
        }

        // show the Beyondar world on the screen
        arFragmentSupport.setWorld(world);
    }

    /**
     * Get the source and destination locations information from the navigatin activity
     */
    private void Get_intent() {
        if (getIntent() != null) {
            intent = getIntent();

            srcDestText.setText(intent.getStringExtra("SRC") + " -> " + intent.getStringExtra("DEST"));
            srcLatLng = intent.getStringExtra("SRCLATLNG");
            destLatLng = intent.getStringExtra("DESTLATLNG");
            //create locations objects from the source and destination
            srcll = Point.fromLngLat(Double.parseDouble(srcLatLng.split(",")[1]), Double.parseDouble(srcLatLng.split(",")[0]));
            destll = Point.fromLngLat(Double.parseDouble(destLatLng.split(",")[1]), Double.parseDouble(destLatLng.split(",")[0]));

            Directions_call(); //HTTP Call Mapbox for navigation path
        }
    }

    private void Directions_call() {
//get navigation from mapbox
        NavigationRoute.builder(getApplicationContext())
                .accessToken(getString(R.string.mapbox_access_token))

                .origin(srcll)
                .destination(destll)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
//Receive the path information
                        DirectionsResponse directionsResponse = response.body();
                        int step_array_size = directionsResponse.routes().get(0).legs().get(0).steps().size();
//display the distance
                        dirDistance.setVisibility(View.VISIBLE);
                        dirDistance.setText(directionsResponse.routes().get(0).legs().get(0)
                                .distance().toString());
//display the time duration
                        dirTime.setVisibility(View.VISIBLE);
                        dirTime.setText(directionsResponse.routes().get(0).legs().get(0)
                                .duration().toString());
//store all the steps from Mapbox in an array
                        steps = new LegStep[step_array_size];
                        for (int i = 0; i < step_array_size; i++) {
                            steps[i] = directionsResponse.routes().get(0).legs().get(0).steps().get(i);
                            Log.d(TAG, "onResponse: STEP " + i + ": " + steps[i].destinations());
                        }

//start the AR when all information receiving is complete
                        Configure_AR();

                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.d(TAG, "onFailure: FAIL" + t.getMessage());
                        new AlertDialog.Builder(getApplicationContext()).setMessage("Fetch Failed").show();
                    }
                });


    }

    @Override
    public void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BeyondarLocationManager.disable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BeyondarLocationManager.enable();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
//All of this to get the current location of the device
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        } else {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            String locationProvider = LocationManager.NETWORK_PROVIDER;

            // mLastLocation = locationManager.getLastKnownLocation(locationProvider);

            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            if (mLastLocation != null) {
                try {
                    Get_intent(); //Fetch Intent Values
                } catch (Exception e) {
                    Log.d(TAG, "onCreate: Intent Error");
                }
            } else {

                Log.d(TAG, "MLast Location is null");
            }
        }

        startLocationUpdates();
    }

    /**
     *
     * @return the requested location of the device
     */
    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    /**
     * start refreshing the current location of the device so when the user moves, the location updates
     */
    protected void startLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, createLocationRequest(), this);




        } catch (SecurityException e) {
            Toast.makeText(this, "Location Permission not granted . Please Grant the permissions",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
//also updates the center location of the Beyondar world when the location of the device updates
        if (world != null) {
            world.setGeoPosition(location.getLatitude(), location.getLongitude());

            if(LocationUtils.distance(destll.latitude(),location.getLatitude(),destll.longitude(), location.getLongitude(), 0,0) < 50){
                Button poibtn = findViewById(R.id.poi_btn);
                poibtn.setVisibility(View.VISIBLE);
            }

        }
    }
}
