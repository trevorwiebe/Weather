package com.trevorwiebe.weather.activities;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.trevorwiebe.weather.R;
import com.trevorwiebe.weather.utils.GetDeviceLocale;
import com.trevorwiebe.weather.utils.LoadWeatherData;
import com.trevorwiebe.weather.utils.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements LoadWeatherData.OnWeatherLoadFinished {

    private static final String TAG = "MainActivity";

    private static final int PERMISSION_CODE = 930;
    private int mCurrentColor;
    private BottomSheetBehavior mBottomSheetBehavior;
    private HashMap<String, String> mWeatherMap = new HashMap<>();
    private boolean isSunUp;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private boolean isDrawerUp = false;

    // Widgets
    private FrameLayout mBaseLayout;
    private LinearLayout mShadowLayout;
    private TextView mCurrentTemp;
    private TextView mCurrentCondition;
    private LinearLayout mLoadingLayout;
    private TextView mLoadingInform;
    private LinearLayout mErrorLayout;
    private TextView mErrorTv;
    private Button mErrorBtn;
    private Button mErrorBtn2;
    private ConstraintLayout mBottomSheet;
    private Button mMoreInfoBtn;
    private TextView mWind;
    private TextView mDewpoint;
    private TextView mPressure;
    private TextView mCityAndState;
    private TextView mHumidity;
    private TextView mPrecip;
    private TextView mVisibility;
    private TextView mFeelsLike;
    private ProgressBar mBottomSheetHorizontalPb;
    private ProgressBar mHorizontalPb;
    private ImageView mWeatherImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect widgets
        mBaseLayout = findViewById(R.id.main_layout);
        mShadowLayout = findViewById(R.id.shadow_layout);
        mCurrentTemp = findViewById(R.id.current_temp);
        mCurrentCondition = findViewById(R.id.current_condition);
        mLoadingLayout = findViewById(R.id.loading_layout);
        mLoadingInform = findViewById(R.id.loading_inform);
        mErrorLayout = findViewById(R.id.errors_layout);
        mErrorTv = findViewById(R.id.error_tv);
        mErrorBtn = findViewById(R.id.error_button);
        mErrorBtn2 = findViewById(R.id.error_button_2);
        mBottomSheet = findViewById(R.id.bottom_sheet);
        mMoreInfoBtn = findViewById(R.id.more_information_btn);

        mWind = findViewById(R.id.wind_content);
        mPrecip = findViewById(R.id.precipitation_content);
        mVisibility = findViewById(R.id.visibility_content);
        mFeelsLike = findViewById(R.id.feels_like_content);
        mDewpoint = findViewById(R.id.dew_point_content);
        mPressure = findViewById(R.id.pressure_content);
        mCityAndState = findViewById(R.id.location_content);
        mHumidity = findViewById(R.id.humidity_content);
        mWeatherImage = findViewById(R.id.weather_image);
        mBottomSheetHorizontalPb = findViewById(R.id.bottom_horizontal_pb);
        mHorizontalPb = findViewById(R.id.horizontal_pb);

        mBottomSheetHorizontalPb.setScaleY(3f);
        mHorizontalPb.setScaleY(3f);


        mCurrentColor = getIntent().getIntExtra("current_color", getResources().getColor(R.color.colorPrimary));

        if (savedInstanceState != null) {
            isSunUp = savedInstanceState.getBoolean("isSunUp");
            mWeatherMap = (HashMap<String, String>) savedInstanceState.getSerializable("weatherMap");
        }
        mBottomSheet.setVisibility(View.INVISIBLE);
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);
        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setPeekHeight(-10);
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (Float.isNaN(slideOffset)) slideOffset = 1;
                if (Float.isInfinite(slideOffset)) slideOffset = 0;
                int transparent = getResources().getColor(android.R.color.transparent);
                int colorWithDrawerUp = getResources().getColor(R.color.color_with_drawer_up);
                int backgroundColor = ColorUtils.blendARGB(transparent, colorWithDrawerUp, slideOffset);

                mShadowLayout.setBackgroundColor(backgroundColor);

                if (slideOffset == 1.0) {
                    isDrawerUp = true;
                } else if (slideOffset == 0.0) {
                    isDrawerUp = false;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int a = Color.alpha(mCurrentColor);
                    int r = Math.round(Color.red(mCurrentColor) * 0.6f);
                    int g = Math.round(Color.green(mCurrentColor) * 0.6f);
                    int b = Math.round(Color.blue(mCurrentColor) * 0.6f);
                    int notificationBarColor = Color.argb(a, Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
                    int statusBarColor = ColorUtils.blendARGB(mCurrentColor, notificationBarColor, slideOffset);
                    Window window = MainActivity.this.getWindow();
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setStatusBarColor(statusBarColor);
                }
            }
        });

        mErrorLayout.setVisibility(View.INVISIBLE);

        mMoreInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomSheet.setVisibility(View.VISIBLE);
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        // this location listener is call when determineLocation is called
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                mLocationManager.removeUpdates(mLocationListener);
                loadWeatherDataStepTwo(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
    }

    @Override
    protected void onResume() {
        loadWeatherDataStepOne();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        getIntent().putExtra("current_color", mCurrentColor);
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (mLocationManager != null) {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, mLocationListener);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isSunUp", isSunUp);
        outState.putSerializable("weatherMap", mWeatherMap);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void loadFinished(String rawData) {
        parseData(rawData);
        putDataInViews();
    }

    public void rate(View view) {
        final String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    public void openMenu(View view) {
        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    public void refresh(View view) {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mWeatherMap.clear();
        loadWeatherDataStepOne();
    }

    @Override
    public void onBackPressed() {
        if (isDrawerUp) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    private void loadWeatherDataStepOne() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean(getString(R.string.pref_previously_started), false);
        if (!previouslyStarted) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(getString(R.string.pref_previously_started), Boolean.TRUE);
            edit.apply();
            showErrorMessage(Utility.FIRST_TIME_START_UP);
        } else {
            if (mWeatherMap.size() == 0 || mWeatherMap == null || !mWeatherMap.get("selectedLocation").equals(getCurrentLocationSetting())) {
                showLoading("");
                if (Utility.isConnectedToInternet(this)) {
                    String strLocation = getCurrentLocationSetting();

                    // need to get the current location of the device
                    if (strLocation.equals(getResources().getString(R.string.location_current_location_label))) {

                        // check if the device has the locations turned on
                        if (Utility.isLocationEnabled(this)) {

                            // check if we have permission to access the location
                            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                if (mLocationManager != null) {
                                    showLoading(getResources().getString(R.string.loading_inform_requesting_location));
                                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, mLocationListener);
                                } else {
                                    showErrorMessage(Utility.FAILED_TO_GET_LOCATION);
                                }
                            } else {
                                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                                    showErrorMessage(Utility.NEED_LOCATION_PERMISSION);
                                } else {
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
                                }
                            }
                        } else {
                            showErrorMessage(Utility.LOCATIONS_NOT_TURNED_ON);
                        }

                    } else {
                        // since we don't need to get the location of the device, we will just pass null.  This will notify loadWeatherDataStepTwo to
                        // use the a saved location instead
                        loadWeatherDataStepTwo(null);
                    }
                } else {
                    showErrorMessage(Utility.NO_INTERNET_CONNECTION);
                }
            } else {
                putDataInViews();
            }
        }
    }

    private void loadWeatherDataStepTwo(@Nullable Location location) {

        final String selectedLocation = getCurrentLocationSetting();

        if (location == null) {
            final Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (Geocoder.isPresent()) {
                        try {

                            Geocoder gc = new Geocoder(MainActivity.this);
                            List<Address> addresses = gc.getFromLocationName(selectedLocation, 1);

                            if (addresses.size() == 0) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadWeatherDataStepThree("38", "-97", selectedLocation);
                                    }
                                });
                                return;
                            }

                            Address a = addresses.get(0);

                            if (a.hasLatitude() && a.hasLongitude()) {
                                final String latitude = Double.toString(a.getLatitude());
                                final String longitude = Double.toString(a.getLongitude());

                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadWeatherDataStepThree(latitude, longitude, selectedLocation);
                                    }
                                });
                            } else {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadWeatherDataStepThree("38", "-97", selectedLocation);
                                    }
                                });
                            }
                        } catch (final IOException e) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    loadWeatherDataStepThree("38", "-97", selectedLocation);
                                }
                            });
                        }
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                loadWeatherDataStepThree("38", "-97", selectedLocation);
                            }
                        });
                    }
                }
            };
            new Thread(runnable).start();
        } else {
            String latitude = Double.toString(location.getLatitude());
            String longitude = Double.toString(location.getLongitude());

            loadWeatherDataStepThree(latitude, longitude, selectedLocation);
        }

    }

    private void loadWeatherDataStepThree(String latitude, String longitude, String currentLocation) {

        if (latitude != null || longitude != null) {

            showLoading(getResources().getString(R.string.loading_inform_fetching_weather_info));

            com.luckycatlabs.sunrisesunset.dto.Location sunriseSunsetLocation = new com.luckycatlabs.sunrisesunset.dto.Location(latitude, longitude);
            SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(sunriseSunsetLocation, TimeZone.getDefault());

            Calendar calendar = Calendar.getInstance();

            Calendar officialSunrise = calculator.getOfficialSunriseCalendarForDate(calendar);
            Calendar officialSunset = calculator.getOfficialSunsetCalendarForDate(calendar);

            long sunsetMillis = officialSunset.getTimeInMillis();
            long sunriseMillis = officialSunrise.getTimeInMillis();
            long currentTime = calendar.getTimeInMillis();

            isSunUp = sunriseMillis < currentTime && sunsetMillis > currentTime;

            String url;

            if (currentLocation != null) {
                if (!currentLocation.equals(getResources().getString(R.string.location_current_location_label))) {
                    url = Utility.BASE_URL + currentLocation + ".json";
                } else {
                    url = Utility.BASE_URL + latitude + "," + longitude + ".json";
                }
            } else {
                url = Utility.BASE_URL + latitude + "," + longitude + ".json";
            }

            new LoadWeatherData(MainActivity.this).execute(url);

        } else {
            showErrorMessage(Utility.FAILED_TO_GET_LOCATION);
        }
    }

    private void parseData(String rawData) {
        if (rawData == null || rawData.equals("")) {
            showErrorMessage(Utility.NO_DATA_RETURNED);
        } else {
            try {

                // clear the weather hash map of any data
                mWeatherMap.clear();

                /*
                   This block of code parses the json data
                */
                JSONObject baseJsonObject = new JSONObject(rawData);
                JSONObject currentObservationObject = baseJsonObject.getJSONObject("current_observation");

                mWeatherMap.put("selectedLocation", getCurrentLocationSetting());
                mWeatherMap.put("weather", currentObservationObject.getString("weather"));
                mWeatherMap.put("pressure", currentObservationObject.getString("pressure_in"));
                mWeatherMap.put("last_updated", currentObservationObject.getString("observation_time"));
                mWeatherMap.put("humidity", currentObservationObject.getString("relative_humidity"));
                mWeatherMap.put("temp_f", currentObservationObject.getString("temp_f"));

                JSONObject locationObject = currentObservationObject.getJSONObject("display_location");
                mWeatherMap.put("city_and_state", locationObject.getString("full"));
                mWeatherMap.put("sample_lat", locationObject.getString("latitude"));
                mWeatherMap.put("sample_lng", locationObject.getString("longitude"));

                mWeatherMap.put("wind_mph", currentObservationObject.getString("wind_mph"));
                mWeatherMap.put("wind_mph_gust", currentObservationObject.getString("wind_gust_mph"));
                mWeatherMap.put("wind_dir", currentObservationObject.getString("wind_dir"));
                mWeatherMap.put("temp", currentObservationObject.getString("temp_f"));
                mWeatherMap.put("dewpoint", currentObservationObject.getString("dewpoint_f"));
                mWeatherMap.put("precip_today", currentObservationObject.getString("precip_today_in"));
                mWeatherMap.put("visibility", currentObservationObject.getString("visibility_mi"));
                mWeatherMap.put("feelslike", currentObservationObject.getString("feelslike_f"));

            } catch (JSONException e) {
                try {
                    JSONObject baseJsonObject = new JSONObject(rawData);
                    JSONObject responseObject = baseJsonObject.getJSONObject("response");
                    JSONObject errorObject = responseObject.getJSONObject("error");
                    String type = errorObject.getString("type");
                    if (type.equals("querynotfound")) {
                        showErrorMessage(Utility.LOCATION_NOT_RECOGNIZED);
                    } else {
                        showErrorMessage(Utility.JSON_PARSING_FAILED);
                    }
                } catch (JSONException e2) {
                    showErrorMessage(Utility.JSON_PARSING_FAILED);
                }
            }
        }
    }

    private void setBackgroundColors(boolean shouldFade, int colorToSetAt) {

        if (shouldFade) {
            ObjectAnimator colorFade = ObjectAnimator.ofObject(mBaseLayout, "backgroundColor", new ArgbEvaluator(), mCurrentColor, colorToSetAt);
            colorFade.setDuration(1000);
            colorFade.start();

            ObjectAnimator moreInfoFade = ObjectAnimator.ofObject(mMoreInfoBtn, "backgroundColor", new ArgbEvaluator(), mCurrentColor, colorToSetAt);
            moreInfoFade.setDuration(1000);
            moreInfoFade.start();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = this.getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                ObjectAnimator notiFade = ObjectAnimator.ofObject(window, "StatusBarColor", new ArgbEvaluator(), mCurrentColor, colorToSetAt);
                notiFade.setDuration(1000);
                notiFade.start();
            }
        } else {
            mBaseLayout.setBackgroundColor(colorToSetAt);
            mMoreInfoBtn.setBackgroundColor(colorToSetAt);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = this.getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(colorToSetAt);
            }
        }

        mCurrentColor = colorToSetAt;
    }

    private void setTextColor(int color) {
        mCurrentTemp.setTextColor(color);
        mCurrentCondition.setTextColor(color);
    }

    private void putDataInViews() {

        if (mWeatherMap.size() == 0 || mWeatherMap == null) {
            return;
        }

        showMainContent();

        String tempIdentifier;
        String distanceIdentifier;
        String windIdentifier;
        String liquidIdentifier;

        String currentTemp = mWeatherMap.get("temp");
        String wind = mWeatherMap.get("wind_mph");
        String windDir = mWeatherMap.get("wind_dir");
        String windGust = mWeatherMap.get("wind_gust_mph");
        String dewPoint = mWeatherMap.get("dewpoint");
        String pressure = mWeatherMap.get("pressure");
        String location = mWeatherMap.get("city_and_state");
        String lastUpdated = mWeatherMap.get("last_updated");
        String humidity = mWeatherMap.get("humidity");
        String precipitation = mWeatherMap.get("precip_today");
        String feelsLike = mWeatherMap.get("feelslike");
        String visibility = mWeatherMap.get("visibility");

        switch (getCurrentUnitSetting()) {
            case Utility.AUTOMATIC:
                if (GetDeviceLocale.getDefault() == GetDeviceLocale.Imperial) {
                    tempIdentifier = getResources().getString(R.string.fahrenheit);
                    distanceIdentifier = " mi";
                    windIdentifier = " mph";
                    liquidIdentifier = " in";
                } else {
                    tempIdentifier = getResources().getString(R.string.celsius);
                    windIdentifier = " kph";
                    distanceIdentifier = " km";
                    liquidIdentifier = " mm";

                    currentTemp = convertToMetric(currentTemp, Utility.TEMPERATURE);
                    wind = convertToMetric(wind, Utility.DISTANCE);
                    windGust = convertToMetric(windGust, Utility.DISTANCE);
                    dewPoint = convertToMetric(dewPoint, Utility.TEMPERATURE);
                    precipitation = convertToMetric(precipitation, Utility.LENGTH);
                    feelsLike = convertToMetric(feelsLike, Utility.TEMPERATURE);
                    visibility = convertToMetric(visibility, Utility.DISTANCE);
                }
                break;
            case Utility.CELSIUS:
                tempIdentifier = getResources().getString(R.string.celsius);
                windIdentifier = " kph";
                distanceIdentifier = " km";
                liquidIdentifier = " mm";

                currentTemp = convertToMetric(currentTemp, Utility.TEMPERATURE);
                wind = convertToMetric(wind, Utility.DISTANCE);
                windGust = convertToMetric(windGust, Utility.DISTANCE);
                dewPoint = convertToMetric(dewPoint, Utility.TEMPERATURE);
                precipitation = convertToMetric(precipitation, Utility.LENGTH);
                feelsLike = convertToMetric(feelsLike, Utility.TEMPERATURE);
                visibility = convertToMetric(visibility, Utility.DISTANCE);

                break;
            case Utility.IMPERIAL:
                tempIdentifier = getResources().getString(R.string.fahrenheit);
                windIdentifier = " mph";
                distanceIdentifier = " mi";
                liquidIdentifier = " in";
                break;
            default:
                tempIdentifier = getResources().getString(R.string.fahrenheit);
                windIdentifier = " mph";
                distanceIdentifier = " mi";
                liquidIdentifier = " in";
                break;
        }


        mCurrentCondition.setText(mWeatherMap.get("weather"));
        int drawable = chooseWeatherImage(mWeatherMap.get("weather"));
        Resources resources = getResources();
        Bitmap weather_icon = BitmapFactory.decodeResource(resources, drawable);
        mWeatherImage.setImageBitmap(weather_icon);

        String finishedCurrentTemp = currentTemp + " " + tempIdentifier;
        String finishedWind = wind + windIdentifier;
        String finishedWindString;
        if (windGust != null) {
            String finishedWindGust = windGust + windIdentifier;
            finishedWindString = getResources().getString(R.string.wind_string_gust, finishedWind, windDir, finishedWindGust);
        } else {
            finishedWindString = getResources().getString(R.string.wind_string, finishedWind, windDir);
        }
        String finishedDewPoint = dewPoint + " " + tempIdentifier;
        String finishedPrecipitation = precipitation + liquidIdentifier;
        String finishedFeelsLike = feelsLike + " " + tempIdentifier;
        String finishedVisibility = visibility + distanceIdentifier;
        String finishedPressure = pressure + " in";

        mCurrentTemp.setText(finishedCurrentTemp);
        mWind.setText(finishedWindString);
        mDewpoint.setText(finishedDewPoint);
        mPressure.setText(finishedPressure);
        mCityAndState.setText(location);
        mHumidity.setText(humidity);
        mPrecip.setText(finishedPrecipitation);
        mFeelsLike.setText(finishedFeelsLike);
        mVisibility.setText(finishedVisibility);

        // set background colors
        double temp = Float.parseFloat(mWeatherMap.get("temp_f"));

        int blue = getResources().getColor(R.color.blue);
        int red = getResources().getColor(R.color.red);
        float colorRatio;
        if (temp < 0) {
            colorRatio = 0.0f;
        } else if (temp > 100) {
            colorRatio = 1f;
        } else {
            double colorRationDb = temp * 0.01;
            colorRatio = (float) colorRationDb;
        }
        int colorToSetAt = ColorUtils.blendARGB(blue, red, colorRatio);

        setBackgroundColors(true, colorToSetAt);

        // set the text color
        int textColor = getResources().getColor(R.color.white);
        setTextColor(textColor);
    }

    private int chooseWeatherImage(String weather) {
        if (isSunUp) {
            switch (weather) {
                case "Overcast":
                    return R.drawable.cloudy;
                case "Cloudy":
                    return R.drawable.cloudy;
                case "Clear":
                    return R.drawable.sun;
                case "Sunny":
                    return R.drawable.sun;
                case "Flurries":
                    return R.drawable.snow;
                case "Freezing Rain":
                    return R.drawable.freezing_rain;
                case "Sleet":
                    return R.drawable.freezing_rain;
                case "Snow":
                    return R.drawable.snow;
                case "Chance of Snow":
                    return R.drawable.snow;
                case "Chance of Flurries":
                    return R.drawable.snow;
                case "Chance of Freezing Rain":
                    return R.drawable.freezing_rain;
                case "Chance of Sleet":
                    return R.drawable.freezing_rain;
                case "Chance of Thunderstorms":
                    return R.drawable.thunderstorm;
                case "Chance of a Thunderstorm":
                    return R.drawable.thunderstorm;
                case "Thunderstorm":
                    return R.drawable.thunderstorm;
                case "Thunderstorms":
                    return R.drawable.thunderstorm;
                case "Fog":
                    return R.drawable.cloudy;
                case "Haze":
                    return R.drawable.cloudy;
                case "Chance of Rain":
                    return R.drawable.rain;
                case "Chance Rain":
                    return R.drawable.rain;
                case "Rain":
                    return R.drawable.rain;
                case "Partly Cloudy":
                    return R.drawable.partly_cloudy_sun;
                case "Mostly Cloudy":
                    return R.drawable.mostly_cloudy_sun;
                case "Mostly Sunny":
                    return R.drawable.partly_cloudy_sun;
                case "Partly Sunny":
                    return R.drawable.mostly_cloudy_sun;
                case "Scattered Clouds":
                    return R.drawable.partly_cloudy_sun;
                default:
                    return R.drawable.unknown;
            }
        } else {
            switch (weather) {
                case "Overcast":
                    return R.drawable.cloudy;
                case "Cloudy":
                    return R.drawable.cloudy;
                case "Clear":
                    return R.drawable.moon;
                case "Flurries":
                    return R.drawable.snow;
                case "Freezing Rain":
                    return R.drawable.freezing_rain;
                case "Sleet":
                    return R.drawable.freezing_rain;
                case "Snow":
                    return R.drawable.snow;
                case "Chance of Snow":
                    return R.drawable.snow;
                case "Chance of Flurries":
                    return R.drawable.snow;
                case "Chance of Freezing Rain":
                    return R.drawable.freezing_rain;
                case "Chance of Sleet":
                    return R.drawable.freezing_rain;
                case "Chance of Thunderstorms":
                    return R.drawable.thunderstorm;
                case "Chance of a Thunderstorm":
                    return R.drawable.thunderstorm;
                case "Thunderstorm":
                    return R.drawable.thunderstorm;
                case "Thunderstorms":
                    return R.drawable.thunderstorm;
                case "Fog":
                    return R.drawable.cloudy;
                case "Haze":
                    return R.drawable.cloudy;
                case "Chance of Rain":
                    return R.drawable.rain;
                case "Chance Rain":
                    return R.drawable.rain;
                case "Rain":
                    return R.drawable.rain;
                case "Mostly Cloudy":
                    return R.drawable.mostly_cloudy_moon;
                case "Partly Cloudy":
                    return R.drawable.partly_cloudy_moon;
                case "Mostly Sunny":
                    return R.drawable.partly_cloudy_moon;
                case "Partly Sunny":
                    return R.drawable.mostly_cloudy_moon;
                case "Scattered Clouds":
                    return R.drawable.partly_cloudy_moon;
                default:
                    return R.drawable.unknown;
            }
        }
    }

    private void showErrorMessage(int errorCode) {

        mWeatherMap.clear();
        setBackgroundColors(false, getResources().getColor(R.color.colorPrimary));

        hideAllViews();
        mErrorBtn2.setVisibility(View.INVISIBLE);

        View.OnClickListener errorBtnClickListener;
        View.OnClickListener errorBtn2ClickListener;

        String errorMessage;
        String buttonText = getResources().getString(R.string.retry);
        String buttonText2 = "";
        switch (errorCode) {
            case Utility.JSON_PARSING_FAILED:
                errorMessage = getResources().getString(R.string.data_returned_is_unknown);
                buttonText = getResources().getString(R.string.update_settings);
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent setNewLocationIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(setNewLocationIntent);
                    }
                };
                errorBtn2ClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                };
                break;
            case Utility.NO_DATA_RETURNED:
                errorMessage = getResources().getString(R.string.no_data_returned);
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadWeatherDataStepOne();

                    }
                };
                errorBtn2ClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                };
                break;
            case Utility.LOCATIONS_NOT_TURNED_ON:
                errorMessage = getResources().getString(R.string.turn_on_location);
                buttonText = getResources().getString(R.string.open_location);
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                };
                errorBtn2ClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                };
                break;
            case Utility.NEED_LOCATION_PERMISSION:
                errorMessage = getResources().getString(R.string.needs_permission);
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
                    }
                };
                errorBtn2ClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(settingsIntent);
                    }
                };
                buttonText = getResources().getString(R.string.grant_permission);
                buttonText2 = getResources().getString(R.string.update_settings);
                mErrorBtn2.setVisibility(View.VISIBLE);
                break;
            case Utility.NO_INTERNET_CONNECTION:
                errorMessage = getResources().getString(R.string.no_connection);
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadWeatherDataStepOne();
                    }
                };
                errorBtn2ClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                };
                break;
            case Utility.FAILED_TO_GET_LOCATION:
                errorMessage = getResources().getString(R.string.location_not_found);
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadWeatherDataStepOne();
                    }
                };

                errorBtn2ClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent setNewLocationIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(setNewLocationIntent);
                    }
                };

                buttonText2 = getResources().getString(R.string.update_settings);
                mErrorBtn2.setVisibility(View.VISIBLE);
                break;
            case Utility.LOCATION_NOT_RECOGNIZED:
                errorMessage = getResources().getString(R.string.location_not_found);
                buttonText = getResources().getString(R.string.location_not_found_btn_text);
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent setNewLocationIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(setNewLocationIntent);
                    }
                };
                errorBtn2ClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                };
                break;

            case Utility.FIRST_TIME_START_UP:
                errorMessage = getResources().getString(R.string.thanks_for_downloading);

                buttonText = getResources().getString(R.string.current_location_btn);
                buttonText2 = getResources().getString(R.string.set_a_location);
                mErrorBtn2.setVisibility(View.VISIBLE);

                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadWeatherDataStepOne();
                    }
                };
                errorBtn2ClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(settingsIntent);
                    }
                };
                break;
            default:
                errorMessage = getResources().getString(R.string.unknown_error);
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadWeatherDataStepOne();
                    }
                };

                errorBtn2ClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                };
                break;
        }

        mErrorLayout.setVisibility(View.VISIBLE);
        mErrorTv.setText(errorMessage);
        mErrorBtn.setOnClickListener(errorBtnClickListener);
        mErrorBtn.setText(buttonText);

        mErrorBtn2.setOnClickListener(errorBtn2ClickListener);
        mErrorBtn2.setText(buttonText2);
    }

    private void showLoading(String loadingInform) {

        hideAllViews();

        int color;
        if (getIntent().getExtras() == null) {
            color = getResources().getColor(R.color.colorPrimary);
        } else {
            color = getIntent().getIntExtra("current_color", getResources().getColor(R.color.colorPrimary));
        }
        setBackgroundColors(false, color);
        setTextColor(color);
        mLoadingLayout.setVisibility(View.VISIBLE);
        mLoadingInform.setText(loadingInform);
    }

    private void showMainContent() {
        mMoreInfoBtn.setVisibility(View.VISIBLE);
        mErrorLayout.setVisibility(View.INVISIBLE);
        mCurrentTemp.setVisibility(View.VISIBLE);
        mWeatherImage.setVisibility(View.VISIBLE);
        mCurrentCondition.setVisibility(View.VISIBLE);
        mLoadingLayout.setVisibility(View.INVISIBLE);
    }

    private void hideAllViews() {

        mCurrentTemp.setVisibility(View.INVISIBLE);
        mWeatherImage.setVisibility(View.INVISIBLE);
        mCurrentCondition.setVisibility(View.INVISIBLE);
        mLoadingLayout.setVisibility(View.INVISIBLE);
        mHorizontalPb.setVisibility(View.INVISIBLE);
        mBottomSheetHorizontalPb.setVisibility(View.INVISIBLE);
        mErrorLayout.setVisibility(View.INVISIBLE);
        mMoreInfoBtn.setVisibility(View.INVISIBLE);
    }

    private String getCurrentUnitSetting() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getString(getResources().getString(R.string.unit_pref_key), getResources().getString(R.string.unit_auto_value));
    }

    private String getCurrentLocationSetting() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getString(getResources().getString(R.string.location_pref_key), getResources().getString(R.string.location_current_location_label));
    }

    private String convertToMetric(String value, int type) {
        if (value == null) return null;
        double doubleValue = Double.parseDouble(value);
        switch (type) {
            case Utility.TEMPERATURE:
                double doubleCelsius = ((doubleValue - 32) * 5) / 9;
                return Double.toString(roundToNearestTenth(doubleCelsius));
            case Utility.DISTANCE:
                double kmDouble = doubleValue * 1.60934;
                return Double.toString(roundToNearestTenth(kmDouble));
            case Utility.LENGTH:
                double mmDouble = doubleValue * 25.4;
                return Double.toString(roundToNearestTenth(mmDouble));
        }
        return null;
    }

    private double roundToNearestTenth(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

}
