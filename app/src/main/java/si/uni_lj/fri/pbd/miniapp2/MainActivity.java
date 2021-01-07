package si.uni_lj.fri.pbd.miniapp2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private MediaPlayerService mediaPlayerService;

    // variable that indicates whether the MediaPlayerService is connected
    private boolean mediaPlayerServiceBound;

    private TextView songInfo, timeNow, timeDuration;
    private SeekBar seekBar;

    // variable that indicates whether the broadcast is registered
    private boolean isRegistered;

    // vabiable that shows if the song is playing / paused (need it for seekBar)
    private boolean playOrPause;

    // variables that shows the state of sensors
    private boolean accOn;
    private boolean tempOn;
    private boolean gyroOn;

    //  define a MediaPlayerService connection and disconnection
    private ServiceConnection mediaPlayerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MediaPlayerService.MediaPlayerServiceLocalBinder binder = (MediaPlayerService.MediaPlayerServiceLocalBinder) iBinder;
            mediaPlayerService = binder.getService();
            mediaPlayerServiceBound = true;
            // Update we play the song at least once update the UI
            if (mediaPlayerService.isPlaying() || mediaPlayerService.isPaused() || mediaPlayerService.isStopped()) {
                mediaPlayerService.updateUI();
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mediaPlayerServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get the textViews
        songInfo = (TextView) findViewById(R.id.song_info);
        timeNow = (TextView) findViewById(R.id.timeNow);
        timeDuration = (TextView) findViewById(R.id.timeDuration);

        // get the seekBar
        seekBar = (SeekBar) findViewById(R.id.seekBar);

        // set the play / pause value
        playOrPause = false;

        // set the values of the variables that show the state
        accOn = false;
        tempOn = false;
        gyroOn = false;
    }

    // start the service
    @Override
    protected void onStart() {
        super.onStart();
        // Start the MediaPlayerService
        Intent i = new Intent(this, MediaPlayerService.class);
        startService(i);
        // bind the service
        bindService(i, mediaPlayerServiceConnection, 0);
        // broadcast
        IntentFilter filter = new IntentFilter("Song and time");
        registerReceiver(broadcastReceiver, filter);
        isRegistered = true;

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && playOrPause) {
                    // move the seekBar where the user clicked
                    seekBar.setProgress(progress);
                    // update the song time
                    String time = timeMinutesSeconds(progress);
                    timeNow.setText(time);
                    // send broadcast to MediaPlayer to move the song where the seekBar is clicked
                    String s = String.valueOf(progress);
                    Intent intent = new Intent("SeekBarChange");
                    intent.putExtra("change", s);
                    sendBroadcast(intent);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // if the Service is bound, unbind it
        if (mediaPlayerServiceBound) {
            unbindService(mediaPlayerServiceConnection);
        }
        if (isRegistered) {
            unregisterReceiver(broadcastReceiver);
            isRegistered = false;
        }
    }

    // if the user click on play button, play music
    public void play(View view) {
        // check if the service is bound
        if (mediaPlayerServiceBound) {
            // play the song from the service
            mediaPlayerService.start();
            Toast.makeText(getApplicationContext(), "Play", Toast.LENGTH_SHORT).show();
            playOrPause = true;
        }
    }

    // if the user click on pause button, pause the music
    public void pause(View view) {
        // check if the service is bound
        if (mediaPlayerServiceBound) {
            // pause the music from the service
            mediaPlayerService.pause();
            Toast.makeText(getApplicationContext(), "Pause", Toast.LENGTH_SHORT).show();
            playOrPause = true;
        }
    }

    // if the user click on next button, play next song
    public void next(View view) {
        // check if the service is bound
        if (mediaPlayerServiceBound) {
            // pause the music from the service
            mediaPlayerService.next();
            Toast.makeText(getApplicationContext(), "Next song", Toast.LENGTH_SHORT).show();
        }
    }

    // if the user click on stop button, stop the music
    public void stop(View view) {
        // check if the service is bound
        if (mediaPlayerServiceBound) {
            // stop the music from the service
            mediaPlayerService.stop();
            Toast.makeText(getApplicationContext(), "Stop", Toast.LENGTH_SHORT).show();
            playOrPause = false;
        }
    }

    // if the user click on exit button, stop everything and exit the app
    public void exit(View view) {
        // check if the service is bound
        if (mediaPlayerServiceBound) {
            // exit from the service
            mediaPlayerService.exit();
        }
        playOrPause = false;
        accOn = false;
        tempOn = false;
        gyroOn = false;
    }

    // if the user click on Gestures button, activate / deactivate the gestures
    public void gesturesOnOff (View view) {
        // check if the MediaPlayerService is bound, connect the AccelerationService and GyroscopeService
        if (mediaPlayerServiceBound) {
            if (!accOn) {
                accOn = true;
                Toast.makeText(getApplicationContext(), "Accelerometer activated", Toast.LENGTH_SHORT).show();
                mediaPlayerService.gesturesOn();
                // we must turn off the gyroscope (not good if two of them are active)
                if (gyroOn) {
                    gyroOn = false;
                    mediaPlayerService.gyroscopeOff();
                }
            }
            else {
                accOn = false;
                Toast.makeText(getApplicationContext(), "Accelerometer deactivated", Toast.LENGTH_SHORT).show();
                mediaPlayerService.gesturesOff();
            }
        }
    }

    // If the user click on Temperature button, activate / deactivate the temperature sensor
    public void temperatureOnOff (View view) {
        if (mediaPlayerServiceBound) {
            if (!tempOn) {
                tempOn = true;
                Toast.makeText(getApplicationContext(), "Temperature sensor activated", Toast.LENGTH_SHORT).show();
                mediaPlayerService.temperatureOn();
            }
            else {
                tempOn = false;
                Toast.makeText(getApplicationContext(), "Temperature sensor deactivated", Toast.LENGTH_SHORT).show();
                mediaPlayerService.temperatureOff();
            }
        }
    }

    // If the user click on Gyroscope button, activate / deactivate the gyroscope sensor
    public void gyroscopeOnOff(View view) {
        if (mediaPlayerServiceBound) {
            if (!gyroOn) {
                gyroOn = true;
                Toast.makeText(getApplicationContext(), "Gyroscope activated", Toast.LENGTH_SHORT).show();
                mediaPlayerService.gyroscopeOn();
                // we must turn off the accelerometer (not good if two of them are active)
                if (accOn) {
                    accOn = false;
                    mediaPlayerService.gesturesOff();
                }
            }
            else {
                gyroOn = false;
                Toast.makeText(getApplicationContext(), "Gyroscope deactivated", Toast.LENGTH_SHORT).show();
                mediaPlayerService.gyroscopeOff();
            }
        }
    }

    // if the user click on radio on, play the radio
    public void radioOn(View view) {
        // check if the service is bound
        if (mediaPlayerServiceBound) {
            // turn on the radio
            Toast.makeText(getApplicationContext(), "Radio on", Toast.LENGTH_SHORT).show();
            mediaPlayerService.radioOn();
            playOrPause = false;
        }
    }

    // if the user click on radio of, stop the radio
    public void radioOff(View view) {
        // check if the service is bound
        if (mediaPlayerServiceBound) {
            // turn off the radio
            Toast.makeText(getApplicationContext(), "Radio off", Toast.LENGTH_SHORT).show();
            mediaPlayerService.radioOff();
        }
    }

    // if the user click on next radio, change the station
    public void nextRadio(View view) {
        // check if the service is bound
        if (mediaPlayerServiceBound) {
            // turn off the radio
            Toast.makeText(getApplicationContext(), " Next station", Toast.LENGTH_SHORT).show();
            mediaPlayerService.nextRadio();
        }
    }

    // time duration in seconds and minutes (from int to String)
    public String timeMinutesSeconds(int d) {
        String time = "";
        int seconds = d / 1000 % 60;
        int minutes = d / 1000 / 60;
        if (minutes < 10) {
            time += "0" + minutes;
        }
        else {
            time += Long.toString(minutes);
        }
        time += ":";
        if (seconds < 10) {
            time += "0" + seconds;
        }
        else {
            time += Long.toString(seconds);
        }
        return time;
    }

    // get the data from the MediaPlayerService
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals("Song and time")){
                String song = intent.getExtras().getString("song");
                String time = intent.getExtras().getString("time");
                if (song.equals("no song") && time.equals("no time")) {
                    songInfo.setText("");
                    timeNow.setText("00:00");
                    timeDuration.setText("00:00");
                    // set the seekBar at the start
                    seekBar.setProgress(0);
                }
                else if (song.equals("radio")) {
                    // here variable time is name of the radio station !!!
                    songInfo.setText("Radio " + time);
                    timeNow.setText("00:00");
                    timeDuration.setText("00:00");
                    // set the seekBar at the start
                    seekBar.setProgress(0);
                }
                else {
                    songInfo.setText(song);
                    // set the textViews for song time and song duration
                    String []times = time.split("/");
                    String s1 = timeMinutesSeconds(Integer.parseInt(times[0]));
                    String s2 = timeMinutesSeconds(Integer.parseInt(times[1]));
                    timeNow.setText(s1);
                    timeDuration.setText(s2);
                    // set the seekBar
                    int time1 = Integer.parseInt(times[0]);
                    int time2 = Integer.parseInt(times[1]);
                    seekBar.setProgress(time1);
                    seekBar.setMax(time2);
                }
            }
        }
    };
}

