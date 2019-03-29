package arnavigation.appsan.com.myapplication;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import arnavigation.appsan.com.myapplication.utils.UtilsCheck;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.google.android.gms.location.places.ui.PlacePicker.getPlace;

public class NavActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    final static private String TAG = "NavActivity";
    private final int SOURCE_PLACE_PICKER_REQUEST = 11;
    private final int DEST_PLACE_PICKER_REQUEST = 21;

    private GoogleApiClient mGoogleApiClient;
    //source and destination
    private LatLng srcLatLong;
    private LatLng destLatLong;


    //interface views
    @BindView(R.id.source_pick_btn)
    Button sourcePickBtn;
    @BindView(R.id.dest_pick_btn)
    Button destPickBtn;
    @BindView(R.id.nav_start_btn)
    Button navStartBtn;
    @BindView(R.id.source_result_text)
    TextView sourceResultText;
    @BindView(R.id.dest_result_text)
    TextView destResultText;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//init the google maps api
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)

                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
//show layout
        setContentView(R.layout.activity_nav);

        //library to intialize the views
        ButterKnife.bind(this);


        //indecate network connection
        if (!UtilsCheck.isNetworkConnected(this)) {
            Snackbar mySnackbar = Snackbar.make(findViewById(R.id.nav_coord_layout),
                    "Turn Internet On", Snackbar.LENGTH_SHORT);
            mySnackbar.show();
        }

            //indicate GPS availability
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Snackbar mySnackbar = Snackbar.make(findViewById(R.id.nav_coord_layout),
                    "Turn GPS ON", Snackbar.LENGTH_LONG);
            mySnackbar.show();
        }

            //show back arrow button on the action bar
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        //select the source location button(opens google location picker)
        sourcePickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                    startActivityForResult(builder.build(NavActivity.this), SOURCE_PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    Log.d(TAG, "onClick: " + e.getMessage());
                }
            }
        });


        //select the destination location button(opens google location picker)
        destPickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                    startActivityForResult(builder.build(NavActivity.this), DEST_PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    Log.d(TAG, "onClick: " + e.getMessage());
                }
            }
        });


        //start AR navigation button
        navStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressBar pb = findViewById(R.id.navloading);
                pb.setVisibility(View.VISIBLE);
                navStartBtn.setVisibility(View.GONE);

                Intent intent = new Intent(NavActivity.this, ArCamActivity.class);

                try {
                    intent.putExtra("SRC", sourceResultText.getText());
                    intent.putExtra("DEST", destResultText.getText());
                    intent.putExtra("SRCLATLNG", srcLatLong.latitude + "," + srcLatLong.longitude);
                    intent.putExtra("DESTLATLNG", destLatLong.latitude + "," + destLatLong.longitude);
                    startActivity(intent);
                } catch (NullPointerException npe) {
                    Snackbar mySnackbar = Snackbar.make(findViewById(R.id.nav_coord_layout),
                            "Source/Destination Fields are Invalid", Snackbar.LENGTH_SHORT);
                    mySnackbar.show();
                    Log.d(TAG, "onClick: The IntentExtras are Empty");
                }
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


    //Listener for the google location picker(when a user select a location)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
                //if source is selected
            case SOURCE_PLACE_PICKER_REQUEST:
                if (resultCode == RESULT_OK) {
                    Place place = getPlace(data, this);
                    String srcRes = String.format("%s", place.getName());
                    sourceResultText.setText(srcRes);
                    srcLatLong = place.getLatLng();
                    Toast.makeText(this, srcRes, Toast.LENGTH_LONG).show();
                }
                break;
                //if destination is selected
            case DEST_PLACE_PICKER_REQUEST:
                if (resultCode == RESULT_OK) {
                    Place place = getPlace(data, this);
                    String destRes = String.format("%s", place.getName());
                    destResultText.setText(destRes);
                    destLatLong = place.getLatLng();
                    Toast.makeText(this, destRes, Toast.LENGTH_LONG).show();
                }
                break;

        }
    }

    //google maps api listeners
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected:  GoogleApiClient");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectedFailed:  GoogleApiClient");
    }
}
