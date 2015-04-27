package info.guardianproject.zt.z.rss.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import info.guardianproject.zt.R;

public class HelpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zhelp_activity);
        View view = findViewById(R.id.helpLayout);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
}
