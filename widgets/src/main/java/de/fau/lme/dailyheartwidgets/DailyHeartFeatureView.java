package de.fau.lme.dailyheartwidgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Robert on 16.10.16.
 */

public class DailyHeartFeatureView extends RelativeLayout {

    private ImageView mIconImageView;
    private TextView mValueTextView;
    private TextView mUnitTextView;


    public DailyHeartFeatureView(Context context) {
        this(context, null, -1);
    }

    public DailyHeartFeatureView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public DailyHeartFeatureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.widget_icon_view, this);
        mIconImageView = (ImageView) findViewById(R.id.iv_icon);
        mValueTextView = (TextView) findViewById(R.id.tv_value);
        mUnitTextView = (TextView) findViewById(R.id.tv_unit);

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.DailyHeartFeatureView, defStyleAttr, 0);

        try {
            mIconImageView.setImageResource(attributes.getResourceId(R.styleable.DailyHeartFeatureView_icon, 0));
            int color = attributes.getColor(R.styleable.DailyHeartFeatureView_text_color, 0);
            String value = attributes.getString(R.styleable.DailyHeartFeatureView_value);
            String unit = attributes.getString(R.styleable.DailyHeartFeatureView_unit);
            int numCols = attributes.getInteger(R.styleable.DailyHeartFeatureView_num_columns, 2);

            if (numCols > 4) {
                numCols = 4;
            }
            if (value == null) {
                value = "n/a";
            }
            if (unit == null) {
                unit = "";
            }

            mValueTextView.setText(getResources().getString(R.string.placeholder_value, value));
            mUnitTextView.setText(getResources().getString(R.string.placeholder_unit, unit));
            mValueTextView.setTextColor(color);
            mUnitTextView.setTextColor(color);

            if (numCols != 2) {
                float scale = getResources().getDisplayMetrics().density;
                int dpSize = 0;
                ViewGroup.LayoutParams params = mIconImageView.getLayoutParams();
                int valueSize = 0;
                int unitSize = 0;
                switch (numCols) {
                    case 1:
                        dpSize = (int) (80 * scale);
                        valueSize = 36;
                        unitSize = 18;
                        break;
                    case 3:
                        dpSize = (int) (30 * scale);
                        valueSize = 18;
                        unitSize = 9;
                        break;
                    case 4:
                        dpSize = (int) (20 * scale);
                        valueSize = 14;
                        unitSize = 7;
                        break;
                }
                params.height = dpSize;
                params.width = dpSize;
                mIconImageView.setLayoutParams(params);
                mValueTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, valueSize);
                mUnitTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, unitSize);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        attributes.recycle();
    }

    public ImageView getIconView() {
        return mIconImageView;
    }

    public TextView getValueView() {
        return mValueTextView;
    }

    public TextView getUnitView() {
        return mUnitTextView;
    }

    public void setValue(String value) {
        mValueTextView.setText(getResources().getString(R.string.placeholder_value, value));
    }

    public void setUnit(String unit) {
        mUnitTextView.setText(getResources().getString(R.string.placeholder_unit, unit));
    }

}
