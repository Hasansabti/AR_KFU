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
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.beyondar.android.util.location.BeyondarLocationManager;
import com.beyondar.android.world.GeoObject;
import com.beyondar.android.world.World;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.maps.android.SphericalUtil;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.ArrayList;
import java.util.List;

import arnavigation.appsan.com.myapplication.ar.ArFragmentSupport;
import arnavigation.appsan.com.myapplication.ar.DemoUtils;
import arnavigation.appsan.com.myapplication.ar.MyMarker;
import arnavigation.appsan.com.myapplication.utils.LocationCalc;
import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.rendering.LocationNode;
import uk.co.appoly.arcorelocation.rendering.LocationNodeRender;
import uk.co.appoly.arcorelocation.sensor.DeviceLocation;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;
import uk.co.appoly.arcorelocation.utils.LocationUtils;


public class PoiBrowserActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private boolean installRequested;
    public static boolean hasFinishedLoading = false;

    private Snackbar loadingMessageSnackbar = null;

    private ArSceneView arSceneView;

    // Renderables for this example
    private ModelRenderable andyRenderable;

    ArrayList<ViewRenderable> vrs = new ArrayList<>();

    // Our ARCore-Location scene
    private LocationScene locationScene;

    private ArrayList<MyMarker> pois = new ArrayList<>();
    private DirectionsRoute currentRoute;
    private static final String TAG = "DirectionsActivity";
    PoiBrowserActivity myac;

    DeviceLocation lastloc;


    //Points with coordinates taken from Google maps

    @BindView(R.id.ar_source_dest2)
    TextView srcDestText;
    @BindView(R.id.ar_dir_distance)
    TextView dirDistance;
    @BindView(R.id.ar_dir_time)
    TextView dirTime;
    @BindView(R.id.speed)
    TextView speed;

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
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_poi_browser );

        //Nav
        ButterKnife.bind(this);



        Set_googleApiClient(); //Sets the GoogleApiClient

        arSceneView = findViewById(R.id.ar_scene_view);
        myac = this;

        //Points with coordinates taken from Google maps
       // pois.add(new MyMarker(49.599270,
        //        25.337279, "College of Business", "College of Business Administration was established in the academic year 1984 in the College of Planning and Administrative Sciences", this));
//49.59915161132813
        //25.33689909204587
        //25.337370
        pois.add(new MyMarker(49.600150,
                25.335154, "CCSIT", "This is the college of computer science and information technology in KFU. There are three departments: Computer Science,Information System,Computer Network", this));
//
        //25.335013     49.600114

        pois.add(new MyMarker(49.59763884544373,
                25.338707531475805, "Cafeteria", "This is the cafeteria", this));
//25.335095
        //49.599875

        pois.add(new MyMarker(49.597804,
                25.336564, "College of science", "The Faculty of Science was established on 12 June 2002 with four academic departments: Life Sciences, Chemistry, Physics, Mathematics and Statistics.", this));
        pois.add(new MyMarker(49.597685,
                25.337975, "College of Education", "College of Education was established in 1401, and the principle target of establishing college is to meet needs of eastern area from educational teachers", this));
        pois.add(new MyMarker(49.599595,
                25.346862, "College of Medicine", "College of Medicine in Al-Ahsa was established by Royal Decree No.7/B/15252 dated 18/11/1421 H ", this));
        pois.add(new MyMarker(49.594231,
                25.344999, "College of Arts", "Faculty of Arts one of the newest university colleges, its opening was approved on 10/7/1429 H ", this));

        //
        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        arSceneView
                .getScene()
                .addOnUpdateListener(
                        frameTime -> {
                            //don't show if the AR is still loading
                            if (!hasFinishedLoading) {
                                return;
                            }


                            if (locationScene == null) {
                                // If our locationScene object hasn't been setup yet, this is a good time to do it
                                // We know that here, the AR components have been initiated.
                                locationScene = new LocationScene(getApplicationContext(), myac, arSceneView);
                                locationScene.setOffsetOverlapping(true);
                                locationScene.setAnchorRefreshInterval(11190);
                                locationScene.setBearingAdjustment(locationScene.getBearingAdjustment()-10);

                                //loop for all the added POIs
                                for (final MyMarker m : pois) {

                                    LocationMarker lm = new LocationMarker(m.getLonge(), m.getLat(), m.getTheView(getApplicationContext()));
                                   lm.setOnlyRenderWhenWithin(150);

                                    if (m.getLayoutRenderable() != null)
                                        m.getLayoutRenderable().getView().setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {

                                                TextView pvname = findViewById(R.id.poi_place_name);
                                                TextView pvdet = findViewById(R.id.poi_place_address);
                                                ImageView pvimg = findViewById(R.id.poi_place_image);
                                                pvname.setText(m.getTitle());
                                                pvdet.setText(m.getInfo());
                                                pvimg.setImageDrawable(getDrawable(R.drawable.kfu));


                                                View pv = findViewById(R.id.poi_place_detail);
                                                pv.setVisibility(View.VISIBLE);
                                            }
                                        });

                                    //display the POI marker
                                    lm.setRenderEvent(new LocationNodeRender() {
                                        @Override
                                        public void render(LocationNode node) {

                                                View eView = m.getLayoutRenderable().getView();
                                                TextView distanceTextView = eView.findViewById(R.id.poi_container_dist);
                                                TextView title = eView.findViewById(R.id.poi_container_name);
                                                // TextView info = eView.findViewById(R.id.poi_container_info);
                                                //info.setText(m.getInfo());
                                                title.setText(m.getTitle());

                                                distanceTextView.setText(node.getDistance()/3.2 + "M");


                                                double alpha = 0;
                                                alpha = map(node.getDistance(), 0, 1000, 1, 0);
                                                if (alpha <= 1 && alpha >= 0)
                                                    eView.setAlpha((float) alpha);



                                        }
                                    });

                                   // if (LocationUtils.distance(lm.latitude,this.locationScene.deviceLocation.currentBestLocation.getLatitude(),lm.longitude,this.locationScene.deviceLocation.currentBestLocation.getLongitude(),0.0D, 0.0D) < 200) {
                                        locationScene.mLocationMarkers.add(lm);
                                  //  }


                                }


                            }else{

                                //get the direction of the device based on two points
                                DeviceLocation loc = locationScene.deviceLocation;
                                if(lastloc == null){
                                    lastloc = loc;
                                }
/*
                                if(loc.currentBestLocation.distanceTo(lastloc.currentBestLocation) > 20){

                                 float bear = loc.currentBestLocation.bearingTo(lastloc.currentBestLocation);
                                 //
                                    //locationScene.setBearingAdjustment();



                                        lastloc = loc;

                                }
                                /*
                                for (final LocationMarker m : locationScene.mLocationMarkers) {
                                     if (LocationUtils.distance(m.latitude,this.locationScene.deviceLocation.currentBestLocation.getLatitude(),m.longitude,this.locationScene.deviceLocation.currentBestLocation.getLongitude(),0.0D, 0.0D) > 200) {
                                    locationScene.mLocationMarkers.remove(m);

                                    }else{

                                     }

                                }
                                */
                            }

                            //procesing frames
                            Frame frame = arSceneView.getArFrame();
                            if (frame == null) {
                                return;
                            }

                            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                                return;
                            }

                            if (locationScene != null) {

                                locationScene.processFrame(frame);
                            }

                            if (loadingMessageSnackbar != null) {
                                for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
                                    if (plane.getTrackingState() == TrackingState.TRACKING) {
                                        hideLoadingMessage();
                                    }
                                }
                            }
                        });
        // Lastly request CAMERA & fine location permission which is required by ARCore-Location.
        ARLocationPermissionHelper.requestPermission(this);


    }
    //initialize the google API
    private void Set_googleApiClient() {
        Log.d(TAG, "Getting google api client");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

//close the pop-up message
    public void closepoi(View v) {
        View pv = findViewById(R.id.poi_place_detail);
        pv.setVisibility(View.GONE);

    }
    //map the distance from one scale to other scale
    //only for the transparency of the poi square

    private double map(double in, double min, double max, double newmin, double newmax) {
        double out = 0;

        out = (newmax - newmin) * (in - min) / (max - min) + newmin;

        return out;


    }



    /**
     * Make sure we call locationScene.resume();
     */
    @Override
    protected void onResume() {
        super.onResume();
        BeyondarLocationManager.enable();
        if (locationScene != null) {
            locationScene.resume();

        }

        if (arSceneView.getSession() == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                Session session = DemoUtils.createArSession(this, installRequested);
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this);
                    return;
                } else {
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                DemoUtils.handleSessionException(this, e);
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            DemoUtils.displayError(this, "Unable to get camera", ex);
            finish();
            return;
        }

        if (arSceneView.getSession() != null) {
            showLoadingMessage();
        }
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
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Getting location");
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
    private void Get_intent() {
      //  String src = "25.334986,49.599931";
       // String dest = "25.338116,49.601172";


        Log.d(TAG, "Intent: Getting Intent");
        if (getIntent() != null) {
            intent = getIntent();
             String src = intent.getStringExtra("SRCLATLNG");
              String dest = intent.getStringExtra("DESTLATLNG");

            srcDestText.setText(src + " -> " + dest);
            srcLatLng = src;
            destLatLng = dest;

         //   srcLatLng = intent.getStringExtra("SRCLATLNG");
           // destLatLng = intent.getStringExtra("DESTLATLNG");
            //create locations objects from the source and destination
            srcll = Point.fromLngLat(Double.parseDouble(srcLatLng.split(",")[1]), Double.parseDouble(srcLatLng.split(",")[0]));
            destll = Point.fromLngLat(Double.parseDouble(destLatLng.split(",")[1]), Double.parseDouble(destLatLng.split(",")[0]));

            Directions_call(); //HTTP Call Mapbox for navigation path
        }
    }
    private void Directions_call() {
        //Start navigation from mapbox
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
  // remove the default       //world.setDefaultImage(R.drawable.ar_forward);

        //Select the AR fragment from the interface
        arFragmentSupport = (ArFragmentSupport) getSupportFragmentManager().findFragmentById(
                R.id.ar_cam_fragment2);


        Log.d(TAG, "Configure_AR: STEP.LENGTH:" + steps.length);

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
            else if (i == steps.length - 1) {
                GeoObject signObject = new GeoObject(10000 + i);
                signObject.setImageResource(R.drawable.dest_icon);
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
            else if (instructions.contains("right")) {
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
                int pointing = R.drawable.empty;
                int pointing2 = R.drawable.empty;

                polyGeoObj.setGeoPosition(polylineLatLng.get(j).get(k).latitude,
                        polylineLatLng.get(j).get(k).longitude);
                polyGeoObj.setImageResource(pointing);
                polyGeoObj.setName("arObj" + j + k);



                try {

                    //Initialize distance of consecutive polyobjects
                    double dist = LocationCalc.haversine(polylineLatLng.get(j).get(k).latitude,
                            polylineLatLng.get(j).get(k).longitude, polylineLatLng.get(j).get(k + 1).latitude,
                            polylineLatLng.get(j).get(k + 1).longitude) * 1000;

                    //Log.d(TAG, "Configure_AR: polyLineLatLng("+j+","+k+")="+polylineLatLng.get(j).get(k).latitude+","+polylineLatLng.get(j).get(k).longitude);
                    //Log.d(TAG, "Configure_AR: polyLineLatLng("+j+","+(k+1)+")="+polylineLatLng.get(j).get(k+1).latitude+","+polylineLatLng.get(j).get(k+1).longitude);



                    if (dist > 12) {
                       // Log.d(TAG, "dist : "+ dist);
                        //Initialize count of ar objects to be added
                        int arObj_count = ((int) dist / 6) ;
                       // Log.d(TAG, "AR count : "+arObj_count);
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
                                , 6f
                                , heading);

                        //The distance to be incremented
                        double increment_dist = 6f;

                        for (int i = 0; i < arObj_count; i++) {
                            GeoObject inter_polyGeoObj = new GeoObject(5000 + temp_inter_polycount++);

                            //Store the Lat,Lng details into new LatLng Objects using the functions
                            //in LocationCalc class.
                            if (i > 0 && k < polylineLatLng.get(j).size()) {
                                increment_dist += 6f;

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
                                    pointing2 = R.drawable.arright;
                                } else {
                                    pointing2 = R.drawable.arleft;
                                }
                            }


                            //Set the Geoposition along with image and name
                            inter_polyGeoObj.setGeoPosition(tempLatLng.latitude, tempLatLng.longitude);
                            inter_polyGeoObj.setImageResource(pointing2);
                            //    inter_polyGeoObj.setAngle(90,0,0);

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
     * Request the current location of the device
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
     * start refreshing the current location of the device so when the user moves, the location update is requested
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

    /**
     * updates the center location of the Beyondar world when the location of the device updates and change
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {

        if (world != null) {
            world.setGeoPosition(location.getLatitude(), location.getLongitude());

            //check if the distance between the user and the destination is less than 200M
            Button poibtn = findViewById(R.id.poi_btn);
            if (LocationUtils.distance(destll.latitude(), location.getLatitude(), destll.longitude(), location.getLongitude(), 0, 0) < 60 && poibtn.getVisibility() == View.INVISIBLE) {

               // poibtn.setVisibility(View.INVISIBLE);
                final Animation myAnim = AnimationUtils.loadAnimation(this, R.anim.bounce);
                // Use bounce interpolator with amplitude 0.2 and frequency 20
                MyBounceInterpolator interpolator = new MyBounceInterpolator(0.2, 20);
                myAnim.setInterpolator(interpolator);
                //poibtn.startAnimation(myAnim);
            }

        }
    }

    /**
     * Make sure we call locationScene.pause();
     */
    @Override
    public void onPause() {
        super.onPause();
        BeyondarLocationManager.disable();
        if (locationScene != null) {
            locationScene.pause();
        }

        arSceneView.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arSceneView.destroy();
    }


    /**
    * Check if the app has access to the camera
     * */
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                ARLocationPermissionHelper.launchPermissionSettings(this);
            } else {
                Toast.makeText(
                        this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show();
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }



    private void showLoadingMessage() {
        if (loadingMessageSnackbar != null && loadingMessageSnackbar.isShownOrQueued()) {
            return;
        }

        loadingMessageSnackbar =
                Snackbar.make(
                        PoiBrowserActivity.this.findViewById(android.R.id.content),
                        R.string.plane_finding,
                        Snackbar.LENGTH_INDEFINITE);
        loadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
        loadingMessageSnackbar.show();
    }

    private void hideLoadingMessage() {
        if (loadingMessageSnackbar == null) {
            return;
        }

        loadingMessageSnackbar.dismiss();
        loadingMessageSnackbar = null;
    }

    class MyBounceInterpolator implements android.view.animation.Interpolator {
        private double mAmplitude = 1;
        private double mFrequency = 10;

        MyBounceInterpolator(double amplitude, double frequency) {
            mAmplitude = amplitude;
            mFrequency = frequency;
        }

        public float getInterpolation(float time) {
            return (float) (-1 * Math.pow(Math.E, -time / mAmplitude) *
                    Math.cos(mFrequency * time) + 1);
        }
    }
}
