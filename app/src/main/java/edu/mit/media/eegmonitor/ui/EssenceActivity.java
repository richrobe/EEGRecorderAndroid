package edu.mit.media.eegmonitor.ui;

import android.os.Bundle;

import edu.mit.media.eegmonitor.R;

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
