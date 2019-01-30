package gradleplugn.gradleplugn;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private static final boolean isDebug = false;
    private static final String channelName = "GP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println("LOG_DEBUG: " + isDebug);
        System.out.println("LOG_DEBUG: " + channelName);
    }
}
