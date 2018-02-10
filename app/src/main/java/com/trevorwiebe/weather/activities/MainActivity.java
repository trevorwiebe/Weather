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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.trevorwiebe.weather.R;
import com.trevorwiebe.weather.utils.LoadWeatherData;
import com.trevorwiebe.weather.utils.UnitLocale;
import com.trevorwiebe.weather.utils.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;

//public class MainActivity extends AppCompatActivity  {

public class MainActivity extends AppCompatActivity implements LoadWeatherData.OnWeatherLoadFinished, PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "MainActivity";

    private static final int PERMISSION_CODE = 930;
    private int mCurrentColor;
    private BottomSheetBehavior mBottomSheetBehavior;
    private HashMap<String, String> mWeatherMap = new HashMap<>();
    private boolean isSunUp;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    // Widgets
    private FrameLayout mBaseLayout;
    private LinearLayout mShadowLayout;
    private TextView mCurrentTemp;
    private TextView mCurrentCondition;
    private ProgressBar mLoadingIndicator;
    private LinearLayout mErrorLayout;
    private TextView mErrorTv;
    private Button mErrorBtn;
    private ConstraintLayout mBottomSheet;
    private Button mMoreInfoBtn;
    private TextView mWind;
    private TextView mDewpoint;
    private TextView mPressure;
    private TextView mCityAndState;
    private TextView mLastUpdated;
    private TextView mHumidity;
    private TextView mPrecip;
    private TextView mVisibility;
    private TextView mFeelsLike;
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
        mLoadingIndicator = findViewById(R.id.loading_indicator);
        mErrorLayout = findViewById(R.id.errors_layout);
        mErrorTv = findViewById(R.id.error_tv);
        mErrorBtn = findViewById(R.id.error_button);
        mBottomSheet = findViewById(R.id.bottom_sheet);
        mMoreInfoBtn = findViewById(R.id.more_information_btn);

        mWind = findViewById(R.id.wind_content);
        mPrecip = findViewById(R.id.precipitation_content);
        mVisibility = findViewById(R.id.visibility_content);
        mFeelsLike = findViewById(R.id.feels_like_content);
        mDewpoint = findViewById(R.id.dew_point_content);
        mPressure = findViewById(R.id.pressure_content);
        mCityAndState = findViewById(R.id.location_content);
        mLastUpdated = findViewById(R.id.last_updated);
        mHumidity = findViewById(R.id.humidity_content);
        mWeatherImage = findViewById(R.id.weather_image);

        mCurrentColor = getIntent().getIntExtra("current_color", getResources().getColor(R.color.colorPrimary));

        mBottomSheet.setVisibility(View.INVISIBLE);
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);
        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setPeekHeight(0);
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

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                mLocationManager.removeUpdates(mLocationListener);

                String latitude = Double.toString(location.getLatitude());
                String longitude = Double.toString(location.getLongitude());
                String url = Utility.BASE_URL + latitude + "," + longitude + ".json";
                new LoadWeatherData(MainActivity.this).execute(url);

                com.luckycatlabs.sunrisesunset.dto.Location sunriseSunsetLocation = new com.luckycatlabs.sunrisesunset.dto.Location(latitude, longitude);
                SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(sunriseSunsetLocation, "GMT-0600");

                Calendar calendar = Calendar.getInstance();

                Calendar officialSunrise = calculator.getOfficialSunriseCalendarForDate(calendar);
                Calendar officialSunset = calculator.getOfficialSunsetCalendarForDate(calendar);

                long sunsetMillis = officialSunset.getTimeInMillis();
                long sunriseMillis = officialSunrise.getTimeInMillis();
                long currentTime = calendar.getTimeInMillis();

                isSunUp = sunriseMillis < currentTime && sunsetMillis > currentTime;

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
        loadFreshWeatherData();
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
        PopupMenu popupMenu = new PopupMenu(this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent settings_intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settings_intent);
                return true;
            default:
                return false;
        }
    }

    public void loadFreshWeatherData() {

        int colorToSetAt;
        if (getIntent().getExtras() == null) {
            colorToSetAt = getResources().getColor(R.color.colorPrimary);
        } else {
            colorToSetAt = getIntent().getIntExtra("current_color", getResources().getColor(R.color.colorPrimary));
        }

        showLoading(colorToSetAt);

        if (Utility.isLocationEnabled(this)) {
            if (Utility.isConnectedToInternet(this)) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (mLocationManager != null) {
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, mLocationListener);
                    }
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showErrorMessage(Utility.NEED_LOCATION_PERMISSION);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
                    }
                }
            } else {
                showErrorMessage(Utility.NO_INTERNET_CONNECTION);
            }
        } else {
            showErrorMessage(Utility.LOCATIONS_NOT_TURNED_ON);
        }
    }

    private void parseData(String rawData) {
        if (rawData == null || rawData.equals("")) {
            showErrorMessage(Utility.NO_DATA_RETURNED);
        } else {
            try {

                /*
                   This block of code parses the json data
                */

                // clear the weather hash map of any data
                mWeatherMap.clear();

                JSONObject baseJsonObject = new JSONObject(rawData);
                JSONObject currentObservationObject = baseJsonObject.getJSONObject("current_observation");

                mWeatherMap.put("weather", currentObservationObject.getString("weather"));
                mWeatherMap.put("pressure", currentObservationObject.getString("pressure_in"));
                mWeatherMap.put("last_updated", currentObservationObject.getString("observation_time"));
                mWeatherMap.put("humidity", currentObservationObject.getString("relative_humidity"));
                mWeatherMap.put("temp_f", currentObservationObject.getString("temp_f"));

                JSONObject locationObject = currentObservationObject.getJSONObject("display_location");
                mWeatherMap.put("city_and_state", locationObject.getString("full"));
                mWeatherMap.put("sample_lat", locationObject.getString("latitude"));
                mWeatherMap.put("sample_lng", locationObject.getString("longitude"));

                switch (getCurrentUnitSetting()) {
                    case Utility.AUTOMATIC:
                        if (UnitLocale.getDefault() == UnitLocale.Imperial) {
                            mWeatherMap.put("wind_str", currentObservationObject.getString("wind_string"));
                            mWeatherMap.put("temp", currentObservationObject.getString("temp_f"));
                            mWeatherMap.put("dewpoint", currentObservationObject.getString("dewpoint_f"));
                            mWeatherMap.put("precip_today", currentObservationObject.getString("precip_today_in"));
                            mWeatherMap.put("visibility", currentObservationObject.getString("visibility_mi"));
                            mWeatherMap.put("feelslike", currentObservationObject.getString("feelslike_f"));
                        } else {
                            String wind_kph = Integer.toString(currentObservationObject.getInt("wind_kph"));
                            String wind_kph_gust = currentObservationObject.getString("wind_gust_kph");
                            String wind_dir = currentObservationObject.getString("wind_dir");
                            String wind_str = "From the " + wind_dir + " at " + wind_kph + " kph Gusting to " + wind_kph_gust + " kph";
                            mWeatherMap.put("wind_str", wind_str);
                            mWeatherMap.put("temp", currentObservationObject.getString("temp_c"));
                            mWeatherMap.put("dewpoint", currentObservationObject.getString("dewpoint_c"));

                            mWeatherMap.put("temp", currentObservationObject.getString("temp_c"));
                            mWeatherMap.put("dewpoint", currentObservationObject.getString("dewpoint_c"));
                            mWeatherMap.put("precip_today", currentObservationObject.getString("precip_today_metric"));
                            mWeatherMap.put("visibility", currentObservationObject.getString("visibility_km"));
                            mWeatherMap.put("feelslike", currentObservationObject.getString("feelslike_c"));
                        }
                        break;
                    case Utility.IMPERIAL:
                        mWeatherMap.put("wind_str", currentObservationObject.getString("wind_string"));
                        mWeatherMap.put("temp", currentObservationObject.getString("temp_f"));
                        mWeatherMap.put("dewpoint", currentObservationObject.getString("dewpoint_f"));
                        mWeatherMap.put("precip_today", currentObservationObject.getString("precip_today_in"));
                        mWeatherMap.put("visibility", currentObservationObject.getString("visibility_mi"));
                        mWeatherMap.put("feelslike", currentObservationObject.getString("feelslike_f"));
                        break;
                    case Utility.CELSIUS:
                        String wind_kph = Integer.toString(currentObservationObject.getInt("wind_kph"));
                        String wind_kph_gust = currentObservationObject.getString("wind_gust_kph");
                        String wind_dir = currentObservationObject.getString("wind_dir");
                        String wind_str = "From the " + wind_dir + " at " + wind_kph + " kph Gusting to " + wind_kph_gust + " kph";
                        mWeatherMap.put("wind_str", wind_str);
                        mWeatherMap.put("temp", currentObservationObject.getString("temp_c"));
                        mWeatherMap.put("dewpoint", currentObservationObject.getString("dewpoint_c"));

                        mWeatherMap.put("temp", currentObservationObject.getString("temp_c"));
                        mWeatherMap.put("dewpoint", currentObservationObject.getString("dewpoint_c"));
                        mWeatherMap.put("precip_today", currentObservationObject.getString("precip_today_metric"));
                        mWeatherMap.put("visibility", currentObservationObject.getString("visibility_km"));
                        mWeatherMap.put("feelslike", currentObservationObject.getString("feelslike_c"));
                        break;
                    default:
                        mWeatherMap.put("wind_str", currentObservationObject.getString("wind_string"));
                        mWeatherMap.put("temp", currentObservationObject.getString("temp_f"));
                        mWeatherMap.put("dewpoint", currentObservationObject.getString("dewpoint_f"));
                        mWeatherMap.put("precip_today", currentObservationObject.getString("precip_today_in"));
                        mWeatherMap.put("visibility", currentObservationObject.getString("visibility_mi"));
                        mWeatherMap.put("feelslike", currentObservationObject.getString("feelslike_f"));
                        break;
                }
            } catch (JSONException e) {
                showErrorMessage(Utility.JSON_PARSING_FAILED);
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

        showContent();

        String tempIdentifier;
        String distanceIdentifier;
        String liquidIdentifier;

        switch (getCurrentUnitSetting()) {
            case Utility.AUTOMATIC:
                if (UnitLocale.getDefault() == UnitLocale.Imperial) {
                    tempIdentifier = getResources().getString(R.string.fahrenheit);
                    distanceIdentifier = " mi";
                    liquidIdentifier = " in";
                } else {
                    tempIdentifier = getResources().getString(R.string.celsius);
                    distanceIdentifier = " kph";
                    liquidIdentifier = " mm";
                }
                break;
            case Utility.CELSIUS:
                tempIdentifier = getResources().getString(R.string.celsius);
                distanceIdentifier = " kph";
                liquidIdentifier = " mm";
                break;
            case Utility.IMPERIAL:
                tempIdentifier = getResources().getString(R.string.fahrenheit);
                distanceIdentifier = " mi";
                liquidIdentifier = " in";
                break;
            default:
                tempIdentifier = getResources().getString(R.string.fahrenheit);
                distanceIdentifier = " mi";
                liquidIdentifier = " in";
                break;
        }

        String currentTemp = mWeatherMap.get("temp") + " " + tempIdentifier;
        mCurrentTemp.setText(currentTemp);
        mCurrentCondition.setText(mWeatherMap.get("weather"));

        int drawable = chooseWeatherImage(mWeatherMap.get("weather"));
        Resources resources = getResources();
        Bitmap weather_icon = BitmapFactory.decodeResource(resources, drawable);
        mWeatherImage.setImageBitmap(weather_icon);

        // set more info text views
        String wind = mWeatherMap.get("wind_str");
        String dewPoint = mWeatherMap.get("dewpoint") + " " + tempIdentifier;
        String pressure = mWeatherMap.get("pressure");
        String location = mWeatherMap.get("city_and_state");
        String lastUpdated = mWeatherMap.get("last_updated");
        String humidity = mWeatherMap.get("humidity");
        String precipitation = mWeatherMap.get("precip_today") + liquidIdentifier;
        String feelsLike = mWeatherMap.get("feelslike") + " " + tempIdentifier;
        String visibility = mWeatherMap.get("visibility") + distanceIdentifier;

        mWind.setText(wind);
        mDewpoint.setText(dewPoint);
        mPressure.setText(pressure);
        mCityAndState.setText(location);
        mLastUpdated.setText(lastUpdated);
        mHumidity.setText(humidity);
        mPrecip.setText(precipitation);
        mFeelsLike.setText(feelsLike);
        mVisibility.setText(visibility);

        // set background colors
        double temp = Float.parseFloat(mWeatherMap.get("temp_f"));

        Log.d(TAG, "putDataInViews: " + temp);
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

        mCurrentTemp.setVisibility(View.INVISIBLE);
        mWeatherImage.setVisibility(View.INVISIBLE);
        mCurrentCondition.setVisibility(View.INVISIBLE);
        mLoadingIndicator.setVisibility(View.INVISIBLE);

        View.OnClickListener errorBtnClickListener;

        String errorMessage;
        String buttonText = getResources().getString(R.string.retry);
        switch (errorCode) {
            case Utility.JSON_PARSING_FAILED:
                errorMessage = getResources().getString(R.string.data_returned_is_unknown);
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadFreshWeatherData();
                    }
                };
                break;
            case Utility.NO_DATA_RETURNED:
                errorMessage = getResources().getString(R.string.no_data_returned);
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadFreshWeatherData();
                    }
                };
                break;
            case Utility.LOCATIONS_NOT_TURNED_ON:
                errorMessage = getResources().getString(R.string.turn_on_location);
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadFreshWeatherData();
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
                buttonText = getResources().getString(R.string.grant_permission);
                break;
            case Utility.NO_INTERNET_CONNECTION:
                errorMessage = getResources().getString(R.string.no_connection);
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadFreshWeatherData();
                    }
                };
                break;
            case Utility.FAILED_TO_GET_LOCATION:
                errorMessage = getResources().getString(R.string.failed_to_get_location);
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadFreshWeatherData();
                    }
                };
                break;
            default:
                errorMessage = "Unknown Error";
                errorBtnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadFreshWeatherData();
                    }
                };
                break;
        }

        mErrorLayout.setVisibility(View.VISIBLE);
        mErrorTv.setText(errorMessage);
        mErrorBtn.setOnClickListener(errorBtnClickListener);
        mErrorBtn.setText(buttonText);
    }

    private void showLoading(int color) {
        setBackgroundColors(false, color);
        setTextColor(color);
        mMoreInfoBtn.setVisibility(View.INVISIBLE);
        mErrorLayout.setVisibility(View.INVISIBLE);
        mCurrentTemp.setVisibility(View.INVISIBLE);
        mWeatherImage.setVisibility(View.INVISIBLE);
        mCurrentCondition.setVisibility(View.INVISIBLE);
        mLoadingIndicator.setVisibility(View.VISIBLE);
    }

    private void showContent() {
        mMoreInfoBtn.setVisibility(View.VISIBLE);
        mErrorLayout.setVisibility(View.INVISIBLE);
        mCurrentTemp.setVisibility(View.VISIBLE);
        mWeatherImage.setVisibility(View.VISIBLE);
        mCurrentCondition.setVisibility(View.VISIBLE);
        mLoadingIndicator.setVisibility(View.INVISIBLE);
    }

    private String getCurrentUnitSetting() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getString("settings_units", getResources().getString(R.string.unit_auto_value));
    }

}
