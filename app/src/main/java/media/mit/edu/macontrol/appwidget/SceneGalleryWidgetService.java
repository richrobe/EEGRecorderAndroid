package media.mit.edu.macontrol.appwidget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class SceneGalleryWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return (new SceneGalleryGridProvider(getApplicationContext()));
    }

}
