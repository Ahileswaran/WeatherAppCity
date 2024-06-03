package com.example.weatherappcity;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private TextView textAddress, textTime, textTemperature, textHumidity, textDescription;
    private EditText editTextCity;
    private Button buttonFetchWeather;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textAddress = findViewById(R.id.textAddress);
        textTime = findViewById(R.id.textTime);
        textTemperature = findViewById(R.id.textTemperature);
        textHumidity = findViewById(R.id.textHumidity);
        textDescription = findViewById(R.id.textDescription);
        editTextCity = findViewById(R.id.editTextCity);
        buttonFetchWeather = findViewById(R.id.buttonFetchWeather);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sharedPreferences = getSharedPreferences("WeatherAppCity", Context.MODE_PRIVATE);

        buttonFetchWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cityName = editTextCity.getText().toString();
                fetchWeatherByCityName(cityName);
                saveCityName(cityName);
            }
        });

        // Load last searched city name from SharedPreferences
        String lastCityName = sharedPreferences.getString("last_city_name", "");
        if (!lastCityName.isEmpty()) {
            editTextCity.setText(lastCityName);
            fetchWeatherByCityName(lastCityName);
        }

        getLocation();
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            getAddress(latitude, longitude);
                            fetchWeatherByLocation(latitude, longitude);
                            displayCurrentTime();
                        }
                    }
                });
    }

    private void getAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                textAddress.setText("Address: " + address.getAddressLine(0));
            } else {
                textAddress.setText("Address: Unable to determine address");
            }
        } catch (IOException e) {
            e.printStackTrace();
            textAddress.setText("Address: Unable to determine address");
        }
    }

    private void displayCurrentTime() {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        textTime.setText("Time: " + currentTime);
    }

    private void fetchWeatherByLocation(double latitude, double longitude) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherService service = retrofit.create(WeatherService.class);
        Call<WeatherResponse> call = service.getWeather(latitude, longitude, "YOUR_API_KEY");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    WeatherResponse weatherResponse = response.body();
                    if (weatherResponse != null) {
                        double temp = weatherResponse.main.temp - 273.15; // Convert from Kelvin to Celsius
                        int humidity = weatherResponse.main.humidity;
                        String description = weatherResponse.weather.get(0).description;

                        textTemperature.setText("Temperature: " + String.format("%.2f", temp) + "°C");
                        textHumidity.setText("Humidity: " + humidity + "%");
                        textDescription.setText("Description: " + description);
                    }
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void fetchWeatherByCityName(String cityName) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherService service = retrofit.create(WeatherService.class);
        Call<WeatherResponse> call = service.getWeatherByCityName(cityName, "8ac92796adbe70f0028033cf60abcdb2");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    WeatherResponse weatherResponse = response.body();
                    if (weatherResponse != null) {
                        double temp = weatherResponse.main.temp - 273.15; // Convert from Kelvin to Celsius
                        int humidity = weatherResponse.main.humidity;
                        String description = weatherResponse.weather.get(0).description;

                        textTemperature.setText("Temperature: " + String.format("%.2f", temp) + "°C");
                        textHumidity.setText("Humidity: " + humidity + "%");
                        textDescription.setText("Description: " + description);
                    }
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void saveCityName(String cityName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("last_city_name", cityName);
        editor.apply();
    }
}
