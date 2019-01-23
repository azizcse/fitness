package com.example.stepsdistance;

import android.Manifest;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.result.DailyTotalResult;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    public static GoogleApiClient client = null;
    private String LOG_TAG = MainActivity.class.getName();
    private TextView textViewSteps, textViewDistance;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewSteps = findViewById(R.id.text_steps);
        textViewDistance = findViewById(R.id.text_distance);
        requestPermission();
    }


    private void requestPermission() {
        if (PermissionUtil.init(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BODY_SENSORS)) {
            buildGoogleClient();

            subscribeDailySteps();
            subscribeDailyDistance();
        }
    }

    private void buildGoogleClient() {
        if(client == null) {
            client = new GoogleApiClient.Builder(this)
                    .addApi(Fitness.RECORDING_API)
                    .addApi(Fitness.HISTORY_API)
                    .addApi(Fitness.SENSORS_API)
                    .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                    .addConnectionCallbacks(
                            new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(@Nullable Bundle bundle) {
                                    Log.d(LOG_TAG, "buildFitnessClient connected");
                                    // Now you can make calls to the Fitness APIs

                                    // the HomeFragment will call the "subscribeDailySteps()"

                                }

                                @Override
                                public void onConnectionSuspended(int i) {

                                    Log.i(LOG_TAG, "buildFitnessClient connection suspended");
                                    if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                        Log.w(LOG_TAG, "Connection lost.  Cause: Network Lost.");
                                    } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                        Log.w(LOG_TAG, "Connection lost.  Reason: Service Disconnected");
                                    }
                                }


                            }
                    )
                    .enableAutoManage(MainActivity.this, 0, new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                            Log.e(LOG_TAG, "Google Play services failed. Cause: " + connectionResult.toString());

                        }
                    })
                    .build();
        }
    }

    public void subscribeDailySteps() {
        if (client != null) {
            Fitness.RecordingApi.subscribe(client, DataType.TYPE_STEP_COUNT_CUMULATIVE)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {

                            if (status.isSuccess()) {

                                if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                    Log.d(LOG_TAG, "Existing subscription for activity detected.");

                                } else {
                                    Log.d(LOG_TAG, "Successfully subscribed");

                                }

                                readStepsToday();

                            } else {
                                Log.e(LOG_TAG, "There was a problem subscribing");
                            }

                        }
                    });

        }
    }


    public void subscribeDailyDistance() {
        if (client != null) {

            // To create a subscription, invoke the Recording API.
            // As soon as the subscription is active, fitness data will start recording
            Fitness.RecordingApi.subscribe(client, DataType.TYPE_DISTANCE_DELTA)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {

                            if (status.isSuccess()) {

                                if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                    Log.d(LOG_TAG, "Existing subscription for activity detected.");

                                } else {
                                    Log.d(LOG_TAG, "Successfully subscribed");

                                }

                                readDistanceToday();

                            } else {
                                Log.e(LOG_TAG, "There was a problem subscribing");
                            }

                        }
                    });

        }

    }


    private void readStepsToday() {
        new VerifyDataTaskSteps().execute(client);
    }

    private void readDistanceToday() {
        new VerifyDataTaskDistance().execute(client);
    }


    public class VerifyDataTaskSteps extends AsyncTask<GoogleApiClient, Void, Void> {

        int total = 0;

        protected Void doInBackground(GoogleApiClient... clients) {

            PendingResult<DailyTotalResult> result = Fitness.HistoryApi.readDailyTotal(clients[0], DataType.TYPE_STEP_COUNT_DELTA);
            DailyTotalResult totalResult = result.await(30, TimeUnit.SECONDS);
            if (totalResult.getStatus().isSuccess()) {
                DataSet totalSet = totalResult.getTotal();
                total = totalSet.isEmpty()? 0: totalSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
            } else {
                Log.e(LOG_TAG, "There was a problem getting the step count");
            }

            Log.i(LOG_TAG, "Total steps: " + total);


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            textViewSteps.setText(""+total);
        }

    }

    public class VerifyDataTaskDistance extends AsyncTask<GoogleApiClient, Void, Void> {

        float total = 0;

        protected Void doInBackground(GoogleApiClient... clients) {

            PendingResult<DailyTotalResult> result = Fitness.HistoryApi.readDailyTotal(clients[0], DataType.TYPE_DISTANCE_DELTA);
            DailyTotalResult totalResult = result.await(30, TimeUnit.SECONDS);
            if (totalResult.getStatus().isSuccess()) {
                DataSet totalSet = totalResult.getTotal();
                if (totalSet != null) {
                    total = totalSet.isEmpty()
                            ? 0
                            : totalSet.getDataPoints().get(0).getValue(Field.FIELD_DISTANCE).asFloat();
                }
            } else {
                Log.e(LOG_TAG, "There was a problem getting the distance count");
            }

            Log.i(LOG_TAG, "Total distance: " + total);


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(total > 0){
                textViewDistance.setText(""+(total/1000)+" km");
            }

        }

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.REQUEST_CODE_PERMISSION_DEFAULT) {
            requestPermission();
        }
    }
}
