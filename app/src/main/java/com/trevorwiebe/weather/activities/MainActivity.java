package com.trevorwiebe.weather.activities;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.trevorwiebe.weather.R;
import com.trevorwiebe.weather.utils.LoadWeatherData;
import com.trevorwiebe.weather.utils.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements LoadWeatherData.OnWeatherLoadFinished {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_CODE = 930;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private int mCurrentColor;
    private BottomSheetBehavior mBottomSheetBehavior;

    // Widgets
    private FrameLayout mBaseLayout;
    private TextView mCurrentTemp;
    private TextView mCurrentCondition;
    private TextView mWind;
    private ProgressBar mLoadingIndicator;
    private LinearLayout mGrantPermissionLayout;
    private Button mGrantPermissionBtn;
    private LinearLayout mErrorLayout;
    private TextView mErrorTv;
    private Button mRetryConnectingBtn;
    private LinearLayout mBottomSheet;
    private Button mMoreInfoBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect widgets
        mBaseLayout = findViewById(R.id.main_layout);
        mCurrentTemp = findViewById(R.id.current_temp);
        mCurrentCondition = findViewById(R.id.current_condition);
        mWind = findViewById(R.id.wind_conditions);
        mLoadingIndicator = findViewById(R.id.loading_indicator);
        mGrantPermissionLayout = findViewById(R.id.need_permission_layout);
        mGrantPermissionBtn = findViewById(R.id.grant_location_permission_btn);
        mErrorLayout = findViewById(R.id.errors_layout);
        mErrorTv = findViewById(R.id.error_tv);
        mRetryConnectingBtn = findViewById(R.id.retry_connecting_btn);
        mBottomSheet = findViewById(R.id.bottom_sheet);
        mMoreInfoBtn = findViewById(R.id.more_information_btn);

        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);

        mBottomSheetBehavior.setHideable(true);

        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                slideOffset = slideOffset * -1;
                if (Float.isNaN(slideOffset)) slideOffset = 0;
                int color = ColorUtils.blendARGB(getResources().getColor(R.color.white), mCurrentColor, slideOffset);
                mBottomSheet.setBackgroundColor(color);

                int colorWithDrawerUp = manipulateColor(mCurrentColor, 0.6f);
                int backgroundColor = ColorUtils.blendARGB(colorWithDrawerUp, mCurrentColor, slideOffset);
                mBaseLayout.setBackgroundColor(backgroundColor);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Window window = MainActivity.this.getWindow();
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setStatusBarColor(backgroundColor);
                }

                int textColorWithDrawerUp = manipulateColor(getResources().getColor(R.color.white), 0.6f);
                int textColor = ColorUtils.blendARGB(textColorWithDrawerUp, getResources().getColor(R.color.white), slideOffset);
                setTextColor(textColor);
            }
        });

        mGrantPermissionLayout.setVisibility(View.INVISIBLE);
        mErrorLayout.setVisibility(View.INVISIBLE);

        // Set the colors
        setBackgroundColors(getResources().getColor(R.color.loading_color));
        setTextColor(getResources().getColor(R.color.loading_color));

        // Check if device is connected to the internet
        if (isConnectedToInternet()) {

            // If it is - get weather data
            startLoadingWeatherData();

        } else {

            // If not - Show that in the Ui
            mErrorLayout.setVisibility(View.VISIBLE);
            mErrorTv.setText(getResources().getString(R.string.no_connection));
            mLoadingIndicator.setVisibility(View.INVISIBLE);

        }

        mGrantPermissionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
            }
        });

        mRetryConnectingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnectedToInternet()) {
                    mLoadingIndicator.setVisibility(View.VISIBLE);
                    mErrorLayout.setVisibility(View.INVISIBLE);
                    startLoadingWeatherData();
                } else {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mMoreInfoBtn.setVisibility(View.INVISIBLE);
        mMoreInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLoadingIndicator.setVisibility(View.VISIBLE);
                mGrantPermissionLayout.setVisibility(View.INVISIBLE);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mFusedLocationProviderClient.getLastLocation()
                            .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    if (location != null) {
                                        parseLocationAndLoadData(location);
                                    }
                                }
                            });
                }
            } else {
                mLoadingIndicator.setVisibility(View.INVISIBLE);
                mGrantPermissionLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void loadFinished(String rawData) {
        HashMap<String, String> weatherHashMap = parseData(rawData);
        if (weatherHashMap == null) return;
        mCurrentTemp.setText(weatherHashMap.get("temp_f") + (char) 0x00B0 + "F");
        mCurrentCondition.append(" " + weatherHashMap.get("weather"));
        mWind.append(" " + weatherHashMap.get("wind_mph") + " mph from the " + weatherHashMap.get("wind_dir"));

        float temp = Float.parseFloat(weatherHashMap.get("temp_f"));
        evaluateBackgroundColor(temp);
        setBackgroundColors(mCurrentColor);

        mMoreInfoBtn.setVisibility(View.VISIBLE);

        // set the text color
        int textColor = getResources().getColor(R.color.white);
        setTextColor(textColor);

        // hide the loading indicator
        mLoadingIndicator.setVisibility(View.INVISIBLE);

    }

    private void startLoadingWeatherData() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                parseLocationAndLoadData(location);
                            }
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        }
    }

    private void parseLocationAndLoadData(Location location) {
        String latitude = Double.toString(location.getLatitude());
        String longitude = Double.toString(location.getLongitude());
        String url = Utility.BASE_URL + latitude + "," + longitude + ".json";
        new LoadWeatherData(MainActivity.this).execute(url);
    }

    private HashMap<String, String> parseData(String rawData) {
        if (rawData == null || rawData.equals("")) {
            mErrorLayout.setVisibility(View.VISIBLE);
            mErrorTv.setText(getResources().getString(R.string.no_data_returned));
            mLoadingIndicator.setVisibility(View.INVISIBLE);
            return null;
        }
        HashMap<String, String> weather_map = new HashMap<>();
        try {
            JSONObject baseJsonObject = new JSONObject(rawData);
            JSONObject currentObservationObject = baseJsonObject.getJSONObject("current_observation");
            weather_map.put("weather", currentObservationObject.getString("weather"));
            weather_map.put("temp_f", currentObservationObject.getString("temp_f"));
            weather_map.put("wind_dir", currentObservationObject.getString("wind_dir"));
            weather_map.put("wind_mph", currentObservationObject.getString("wind_mph"));
            weather_map.put("last_updated", currentObservationObject.getString("observation_time"));
            return weather_map;
        } catch (JSONException e) {
            Log.d(TAG, "parseData: returning null " + e);
            mErrorLayout.setVisibility(View.VISIBLE);
            mErrorTv.setText(getResources().getString(R.string.data_returned_is_unknown));
            mLoadingIndicator.setVisibility(View.INVISIBLE);
            return null;
        }

    }

    private void evaluateBackgroundColor(float temp) {
        int blue = getResources().getColor(R.color.blue);
        int red = getResources().getColor(R.color.red);
        float colorRatio;
        if (temp < 0) {
            colorRatio = 1f;
        } else if (temp < 10) {
            colorRatio = 0.9f;
        } else if (temp < 20) {
            colorRatio = 0.8f;
        } else if (temp < 30) {
            colorRatio = 0.7f;
        } else if (temp < 40) {
            colorRatio = 0.6f;
        } else if (temp < 50) {
            colorRatio = 0.5f;
        } else if (temp < 60) {
            colorRatio = 0.4f;
        } else if (temp < 70) {
            colorRatio = 0.3f;
        } else if (temp < 80) {
            colorRatio = 0.2f;
        } else if (temp < 90) {
            colorRatio = 0.1f;
        } else if (temp < 100) {
            colorRatio = 0.0f;
        } else {
            colorRatio = 0.5f;
        }
        mCurrentColor = ColorUtils.blendARGB(red, blue, colorRatio);
    }

    private void setBackgroundColors(int color) {
        ObjectAnimator colorFade = ObjectAnimator.ofObject(mBaseLayout, "backgroundColor", new ArgbEvaluator(), getResources().getColor(R.color.loading_color), color);
        colorFade.setDuration(3000);
        colorFade.start();

        ObjectAnimator moreInfoFade = ObjectAnimator.ofObject(mMoreInfoBtn, "backgroundColor", new ArgbEvaluator(), getResources().getColor(R.color.loading_color), color);
        moreInfoFade.setDuration(3000);
        moreInfoFade.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
            ObjectAnimator notiFade = ObjectAnimator.ofObject(window, "StatusBarColor", new ArgbEvaluator(), getResources().getColor(R.color.loading_color), color);
            notiFade.setDuration(3000);
            notiFade.start();
        }
    }

    private void setTextColor(int color) {
        mCurrentTemp.setTextColor(color);
        mCurrentCondition.setTextColor(color);
        mWind.setTextColor(color);
    }

    private boolean isConnectedToInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    public static int manipulateColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r, 255),
                Math.min(g, 255),
                Math.min(b, 255));
    }

}
