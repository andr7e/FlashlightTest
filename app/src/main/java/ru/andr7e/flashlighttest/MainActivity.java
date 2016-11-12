package ru.andr7e.flashlighttest;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

/**
 * Created by andre on 15.10.16.
 */
public class MainActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        Context context = getBaseContext();

        FlashlightControl flashlightControl = new FlashlightControl(context);
        flashlightControl.test();
    }

}