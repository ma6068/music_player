package si.uni_lj.fri.pbd.miniapp2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class TemperatureService extends Service implements SensorEventListener {

    private final IBinder serviceBinder = new TemperatureServiceLocalBinder();

    // variable that indicates if the service is started
    private boolean tOn;

    // Sensort
    private SensorManager sensorManager;
    private Sensor sensor;

    // variable that shows if the temperature sensor is available
    private boolean isRegistered;

    // varialble that show if the temperature sensor is active for the first time
    private boolean firstTImeSensor;

    private int songGroup;

    public class TemperatureServiceLocalBinder extends Binder {
        TemperatureService getService() {
            return TemperatureService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public void onCreate() {
        // Instantiate sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Check if sensor present, set sensor if so
        if (sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        }
        tOn = false;
        firstTImeSensor = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        isRegistered = true;
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // get the temperature
        double temperature = event.values[0];
        int group = -1;
        // christmas songs
        if (temperature <= 5.0) {
            group = 2;
        }
        // summer songs
        else if (temperature >= 30.0) {
            group = 3;
        }
        // other songs
        else {
            group = 1;
        }
        if (firstTImeSensor) {
            firstTImeSensor = false;
            songGroup = group;
            // send data to media player to play the right group of songs
            Intent intent = new Intent("TemperatureChange");
            String s = String.valueOf(songGroup);
            intent.putExtra("change", s);
            sendBroadcast(intent);
        }
        else {
            // change in temperature => other songs have to be played
            if (songGroup != group) {
                // send data to media player to play the right group of songs
                songGroup = group;
                Intent intent = new Intent("TemperatureChange");
                String s = String.valueOf(songGroup);
                intent.putExtra("change", s);
                sendBroadcast(intent);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void temperatureOn() {
        // if the service is not already started => start it
        if (!tOn) {
            tOn = true;
            // start the service
            Intent i = new Intent(TemperatureService.this, TemperatureService.class);
            startService(i);
        }
    }

    public void temperatureOff() {
        // if the service is not already stopped => stop it
        if (tOn) {
            tOn = false;
            if (isRegistered) {
                sensorManager.unregisterListener(this);
                isRegistered = false;
            }
            // stop the service
            Intent i = new Intent(TemperatureService.this, TemperatureService.class);
            stopService(i);
            firstTImeSensor = true;
        }
    }
}
