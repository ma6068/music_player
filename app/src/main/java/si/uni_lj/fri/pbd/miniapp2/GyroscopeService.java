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

public class GyroscopeService extends Service implements SensorEventListener {

    private final IBinder serviceBinder = new GyroscopeServiceLocalBinder();

    // Sensors
    private SensorManager sensorManager;
    private Sensor sensor;

    // variable that indicates if the service is started
    private boolean gOn;

    // variable that indicates if we have x,y,z for the first time
    private boolean firstTimeSensor;

    // variable that indicates whether the sensorManager is registered
    private boolean isRegistered;

    // variables for x, y, z os
    private double x, y, z;

    // Noise threshold
    public static final int NOISE_THRESHOLD = 7;

    public class GyroscopeServiceLocalBinder extends Binder {
        GyroscopeService getService() {
            return GyroscopeService.this;
        }
    }

    @Override
    public void onCreate() {
        // Instantiate sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Check if sensor present, set sensor if so
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
        gOn = false;
        firstTimeSensor = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // get updated through the sensor (register the listener)
        sensorManager.registerListener(this, sensor, 500000);
        isRegistered = true;
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // set the starting positions
        if (firstTimeSensor) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            firstTimeSensor = false;
        }
        double dX = Math.abs(event.values[0] - x);
        double dY = Math.abs(event.values[1] - y);
        double dZ = Math.abs(event.values[2] - z);
        if (dX < NOISE_THRESHOLD) {
            dX = 0;
        }
        if (dY < NOISE_THRESHOLD) {
            dY = 0;
        }
        if (dZ < NOISE_THRESHOLD) {
            dZ = 0;
        }
        if (dZ > dX) {
            Intent intent = new Intent("GyroChange");
            intent.putExtra("action", "1/3");
            sendBroadcast(intent);
        }
        if (dX > dZ) {
            Intent intent = new Intent("GyroChange");
            intent.putExtra("action", "2/3");
            sendBroadcast(intent);
        }
        // update the values
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void gyroscopeOn() {
        // if the service is not already started => start it
        if (!gOn) {
            gOn = true;
            // start the service
            Intent i = new Intent(GyroscopeService.this, GyroscopeService.class);
            startService(i);
        }
    }

    public void gyroscopeOff() {
        // if the service is not already stopped => stop it
        if (gOn) {
            gOn = false;
            if (isRegistered) {
                sensorManager.unregisterListener(this);
                isRegistered = false;
            }
            // stop the service
            Intent i = new Intent(GyroscopeService.this, GyroscopeService.class);
            stopService(i);
            firstTimeSensor = true;
        }
    }
}
