package com.example.weatherapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String API_KEY = "e6e80e479652405cab181147252303"; // WeatherAPI.com key
    private static final String API_URL = "https://api.weatherapi.com/v1/current.json";

    private EditText locationInput;
    private Button searchButton;
    private Button currentLocationButton;
    private TextView locationName;
    private TextView currentDate;
    private TextView weatherDescription;
    private TextView temperature;
    private TextView humidity;
    private TextView windSpeed;
    private ImageView weatherIcon;

    private FusedLocationProviderClient fusedLocationClient;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        locationInput = findViewById(R.id.location_input);
        searchButton = findViewById(R.id.search_button);
        currentLocationButton = findViewById(R.id.current_location_button);
        locationName = findViewById(R.id.location_name);
        currentDate = findViewById(R.id.current_date);
        weatherDescription = findViewById(R.id.weather_description);
        temperature = findViewById(R.id.temperature);
        humidity = findViewById(R.id.humidity);
        windSpeed = findViewById(R.id.wind_speed);
        weatherIcon = findViewById(R.id.weather_icon);

        // Set current date
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
        String formattedDate = sdf.format(new Date());
        currentDate.setText(formattedDate);

        // Initialize OkHttp client
        client = new OkHttpClient();

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set button click listeners
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String location = locationInput.getText().toString();
                if (!location.isEmpty()) {
                    fetchWeatherData(location);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a location", Toast.LENGTH_SHORT).show();
                }
            }
        });

        currentLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
            }
        });

        // Automatically get current location when app starts
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Show a message to indicate permission is needed
            Toast.makeText(MainActivity.this, "Location permission needed for automatic weather updates", Toast.LENGTH_LONG).show();

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            fetchWeatherDataByCoordinates(location.getLatitude(), location.getLongitude());
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                            // Fallback to a default location
                            fetchWeatherData("London");
                        }
                    }
                });
    }

    private void fetchWeatherData(String cityName) {
        String url = API_URL + "?key=" + API_KEY + "&q=" + cityName + "&aqi=no";
        fetchWeather(url);
    }

    private void fetchWeatherDataByCoordinates(double latitude, double longitude) {
        String url = API_URL + "?key=" + API_KEY + "&q=" + latitude + "," + longitude + "&aqi=no";
        fetchWeather(url);
    }

    private void fetchWeather(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Error fetching weather data", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseData = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseData);
                        updateUI(json);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Error parsing weather data", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void updateUI(final JSONObject weatherData) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Parse location data
                    JSONObject location = weatherData.getJSONObject("location");
                    String city = location.getString("name");
                    String country = location.getString("country");
                    locationName.setText(city + ", " + country);

                    // Parse current weather data
                    JSONObject current = weatherData.getJSONObject("current");
                    double temp = current.getDouble("temp_c");
                    temperature.setText(String.format(Locale.getDefault(), "%.1f°C", temp));

                    int humidityValue = current.getInt("humidity");
                    humidity.setText("Humidity: " + humidityValue + "%");

                    double windKph = current.getDouble("wind_kph");
                    String windDirection = current.getString("wind_dir");
                    windSpeed.setText("Wind: " + windKph + " km/h " + windDirection);

                    // Parse condition data
                    JSONObject condition = current.getJSONObject("condition");
                    String description = condition.getString("text");
                    weatherDescription.setText(description);

                    // Get weather condition code and day/night status
                    int weatherCode = condition.getInt("code");
                    boolean isDay = current.getInt("is_day") == 1;

                    // Set the appropriate weather icon
                    setWeatherIcon(weatherCode, isDay);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error displaying weather data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setWeatherIcon(int weatherCode, boolean isDay) {
        // Map WeatherAPI.com condition codes to drawable resources
        int iconResource;

        if (weatherCode == 1000) { // Clear/Sunny
            iconResource = isDay ? R.drawable.sunny : R.drawable.cloudngiht;
        } else if (weatherCode >= 1003 && weatherCode <= 1009) { // Cloudy conditions
            iconResource = isDay ? R.drawable.cloudyday : R.drawable.cloudy;
        } else if ((weatherCode >= 1063 && weatherCode <= 1171) ||
                (weatherCode >= 1180 && weatherCode <= 1201)) { // Rain conditions
            iconResource = R.drawable.rain;
        } else if (weatherCode >= 1273 && weatherCode <= 1282) { // Thunder conditions
            iconResource = R.drawable.thander;
        } else if (weatherCode >= 1210 && weatherCode <= 1264) { // Snow/Sleet conditions
            iconResource = R.drawable.snow;
        } else if (weatherCode >= 1030 && weatherCode <= 1147) { // Fog/Mist conditions
            iconResource = R.drawable.cloudyday;
        } else {
            iconResource = R.drawable.sunny; // Default icon
        }

        weatherIcon.setImageResource(iconResource);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, get location
                getCurrentLocation();
            } else {
                // Permission denied, show message and use default location
                Toast.makeText(this, "Location permission denied. Using default location.", Toast.LENGTH_SHORT).show();
                // Fetch weather for a default location
                fetchWeatherData("London");
            }
        }
    }
}