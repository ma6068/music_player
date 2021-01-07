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
import androidx.annotation.Nullable;

public class AccelerationService extends Service implements SensorEventListener {

    private final IBinder serviceBinder = new AccelerationServiceLocalBinder();

    // Sensors
    private SensorManager sensorManager;
    private Sensor sensor;

    // variable that indicates if the service is started
    private boolean gOn;

    // variable that indicates if we have x,y,z for the first time
    private boolean firstTimeSensor;

    // variables for x, y, z os
    private double x, y, z;

    // Noise threshold
    public static final int NOISE_THRESHOLD = 5;

    // variable that indicates whether the sensorManager is registered
    private boolean isRegistered;

    public class AccelerationServiceLocalBinder extends Binder {
        AccelerationService getService() {
            return AccelerationService.this;
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
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        // set the starting positions
        if (firstTimeSensor) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            firstTimeSensor = false;
        }
        else {
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
            if (dX > dZ) {
                // horizontal change => send data that MediaPlayer.pause have to be called
                Intent intent = new Intent("AccChange");
                intent.putExtra("action", "pause");
                sendBroadcast(intent);
            }
            if (dZ > dX) {
                // vertical change => send data that MediaPlayer.play have to be called
                Intent intent = new Intent("AccChange");
                intent.putExtra("action", "play");
                sendBroadcast(intent);
            }
            // update the values
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void gesturesOn() {
        // if the service is not already started => start it
        if (!gOn) {
            gOn = true;
            // start the service
            Intent i = new Intent(AccelerationService.this, AccelerationService.class);
            startService(i);
        }
    }
    public void gesturesOff() {
        // if the service is not already stopped => stop it
        if (gOn) {
            gOn = false;
            if (isRegistered) {
                sensorManager.unregisterListener(this);
                isRegistered = false;
            }
            // stop the service
            Intent i = new Intent(AccelerationService.this, AccelerationService.class);
            stopService(i);
            firstTimeSensor = true;
        }
    }
}
