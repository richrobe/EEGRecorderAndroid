package de.fau.lme.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Robert on 13.12.15.
 */
public class MainMenuCardView extends CardView {

    private Context mContext;

    private ImageView mImageView;
    private TextView mTitleTextView;


    public MainMenuCardView(Context context) {
        this(context, null, -1);
    }

    public MainMenuCardView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public MainMenuCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.widget_main_menu_card, this);

        mContext = context;
        mTitleTextView = (TextView) findViewById(R.id.tv_main_menu);
        mImageView = (ImageView) findViewById(R.id.iv_main_menu);

        TypedArray attributes = mContext.obtainStyledAttributes(attrs, R.styleable.MainMenuCardView);

        if (attributes != null) {
            try {
                mTitleTextView.setText(attributes
                        .getString(R.styleable.MainMenuCardView_titleText));
                mImageView.setImageResource(attributes
                        .getResourceId(R.styleable.MainMenuCardView_image, 0));
                mImageView.setContentDescription(attributes
                        .getString(R.styleable.MainMenuCardView_imageDescription));
                LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(100), Gravity.CENTER);
                params.setMargins(0, getResources().getDimensionPixelSize(R.dimen.margin_vertical_between),
                        0, getResources().getDimensionPixelSize(R.dimen.margin_vertical_between));
                setLayoutParams(params);
                setRadius(getResources().getDimensionPixelSize(R.dimen.cardview_default_radius));
                setElevation(getResources().getDimensionPixelSize(R.dimen.cardview_default_elevation));
                //setCardBackgroundColor(getResources().getColor(R.color.primary_darkest));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                attributes.recycle();
            }
        }
    }

    public void setImage(int resid) {
        mImageView.setImageResource(resid);
    }

    public void setTitle(int resid) {
        mTitleTextView.setText(resid);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public static class MainMenuWrapper {

        public Class className;
        public int imageId;
        public int titleId;

        public MainMenuWrapper(Class className, int imageId, int titleId) {
            this.className = className;
            this.imageId = imageId;
            this.titleId = titleId;
        }
    }
}
