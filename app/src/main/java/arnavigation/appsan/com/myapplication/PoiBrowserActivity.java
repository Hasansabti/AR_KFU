package arnavigation.appsan.com.myapplication;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.Mapbox;

import java.util.ArrayList;

import arnavigation.appsan.com.myapplication.ar.DemoUtils;
import arnavigation.appsan.com.myapplication.ar.MyMarker;
import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.rendering.LocationNode;
import uk.co.appoly.arcorelocation.rendering.LocationNodeRender;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;
import uk.co.appoly.arcorelocation.utils.LocationUtils;


public class PoiBrowserActivity extends AppCompatActivity {
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

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_poi_browser );
        arSceneView = findViewById(R.id.ar_scene_view);
        myac = this;

        //Points with coordinates taken from Google maps
        pois.add(new MyMarker(49.59915161132813,
                25.33689909204587, "College of Business", "this is the college of business", this));

        pois.add(new MyMarker(49.59991872310639,
                25.334644565363995, "CCSIT", "This is the college of computer science and information technology in KFU. There are three departments: Computer Science,Information System,Computer Network", this));

        pois.add(new MyMarker(49.59763884544373,
                25.338707531475805, "Cafeteria", "This is the cafeteria", this));
//

        pois.add(new MyMarker(49.59763884544373,
                25.338707531475805, "College of science", "this is the college of science", this));

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
                                locationScene.setBearingAdjustment(locationScene.getBearingAdjustment()-1);

                                //loop for all the added POIs
                                for (final MyMarker m : pois) {

                                    LocationMarker lm = new LocationMarker(m.getLonge(), m.getLat(), m.getTheView(getApplicationContext()));
                                   lm.setOnlyRenderWhenWithin(200);

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

                                                distanceTextView.setText(node.getDistance() + "M");


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

    /**
     * Make sure we call locationScene.pause();
     */
    @Override
    public void onPause() {
        super.onPause();

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
}
