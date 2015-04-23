package com.pandamonium.chargealert;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    public static final String PREFERENCES_NAME = "com.pandamonium.chargealert";
    public static final String PREFERENCE_KEY_ENABLED = "enabled";
    public static final String PREFERENCE_KEY_VIBRATE = "vibrate";
    public static final String PREFERENCE_KEY_SOUND = "sound";

    private SharedPreferences mPreferences;
    private Switch mEnabledSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getSupportActionBar().setIcon(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mPreferences = getSharedPreferences(PREFERENCES_NAME, 0);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.version_text)).setText(BuildConfig.VERSION_NAME);

        boolean isEnabled = mPreferences.getBoolean(PREFERENCE_KEY_ENABLED, false);
        mEnabledSwitch = (Switch) findViewById(R.id.enabled_switch);
        mEnabledSwitch.setChecked(isEnabled);
        mEnabledSwitch.setOnCheckedChangeListener(this);

        Switch vibrateSwitch = (Switch) findViewById(R.id.vibrate_switch);
        vibrateSwitch.setChecked(mPreferences.getBoolean(PREFERENCE_KEY_VIBRATE, false));
        vibrateSwitch.setOnCheckedChangeListener(this);

        Switch soundSwitch = (Switch) findViewById(R.id.sound_switch);
        soundSwitch.setChecked(mPreferences.getBoolean(PREFERENCE_KEY_SOUND, false));
        soundSwitch.setOnCheckedChangeListener(this);

        MainService.startIfEnabled(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.feedback) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse(String.format("mailto:%s?subject=%s", getString(R.string.email), getString(R.string.app_name))));
//            intent.setType("message/rfc822");
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.no_email, Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();

        switch (id) {
            case R.id.enabled_switch:
                mPreferences.edit().putBoolean(PREFERENCE_KEY_ENABLED, isChecked).apply();
                MainService.startIfEnabled(this);
                break;
            case R.id.vibrate_switch:
                mPreferences.edit().putBoolean(PREFERENCE_KEY_VIBRATE, isChecked).apply();
                break;
            case R.id.sound_switch:
                mPreferences.edit().putBoolean(PREFERENCE_KEY_SOUND, isChecked).apply();
                break;
        }
    }
}
