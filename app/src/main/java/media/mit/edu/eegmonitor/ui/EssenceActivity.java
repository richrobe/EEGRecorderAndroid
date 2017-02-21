package media.mit.edu.eegmonitor.ui;

import android.os.Bundle;

import media.mit.edu.eegmonitor.R;

/**
 * NEW Activities have to be subclasses of {@link BaseActivity}
 */
public class EssenceActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // layout resource file
        setContentView(R.layout.activity_essence);
    }
}
