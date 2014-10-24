package com.alexvasilkov.foldablelayout.shading;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class GlanceFoldShading implements FoldShading, Target {

    private static final int SHADOW_COLOR = Color.BLACK;
    private static final int SHADOW_MAX_ALPHA = 192;

    private final Paint mSolidShadow;
    private final Paint mGlancePaint;
    private final Rect mGlanceFrom;
    private final Rect mGlanceTo;

    private Bitmap mGlance;

    /**
     * @param context        Application, or activity context.
     * @param glanceResource Loads the resource id as a bitmap using {@link com.squareup.picasso.Picasso}
     *                       cache. NOTICE: this is irreversable. We cannot obtain resource id, to load,
     *                       from already created bitmap, so this is *not backwards compatibile*
     *
     *                       TODO: Maybe save the delivered bitmap into application local cache,
     *                             from where it could be loaded easily
     */
    public GlanceFoldShading(Context context, int glanceResource) {
        mSolidShadow = new Paint();
        mSolidShadow.setColor(SHADOW_COLOR);

        // Should happen on current thread.
        // BONUS effect: we can use the same routine to load remote bitmaps.
        Picasso.with(context).load(glanceResource).into(this);

        mGlancePaint = new Paint();
        mGlancePaint.setDither(true);
        mGlancePaint.setAntiAlias(false);
        mGlancePaint.setFilterBitmap(true);
        mGlanceFrom = new Rect();
        mGlanceTo = new Rect();
    }

    @Override
    public void onPreDraw(Canvas canvas, Rect bounds, float rotation, int gravity) {
        // NO-OP
    }

    @Override
    public void onPostDraw(Canvas canvas, Rect bounds, float rotation, int gravity) {
        final float intensity = getShadowIntencity(rotation, gravity);

        if (intensity > 0) {
            int alpha = (int) (SHADOW_MAX_ALPHA * intensity);
            mSolidShadow.setAlpha(alpha);
            canvas.drawRect(bounds, mSolidShadow);
        }

        boolean isDrawGlance = computeGlance(bounds, rotation, gravity);
        if (isDrawGlance) {
            canvas.drawBitmap(mGlance, mGlanceFrom, mGlanceTo, mGlancePaint);
        }
    }

    private float getShadowIntencity(float rotation, int gravity) {
        float intensity = 0;
        if (gravity == Gravity.TOP) {
            if (rotation > -90 && rotation < 0) { // (-90; 0) - rotation is applied
                intensity = -rotation / 90f;
            }
        }
        return intensity;
    }

    private boolean computeGlance(Rect bounds, float rotation, int gravity) {

        // The bitmap may have failed to load for any reason - IO, memory, or others.
        if (mGlance == null)
            return false;

        if (gravity == Gravity.BOTTOM) {
            if (rotation > 0 && rotation < 90) { // (0; 90) - rotation is applied
                final float aspect = (float) mGlance.getWidth() / (float) bounds.width();

                // computing glance offset
                final int distance = (int) (bounds.height() * ((rotation - 60f) / 15f));
                final int distanceOnGlance = (int) (distance * aspect);

                // computing "to" bounds
                int scaledGlanceHeight = (int) (mGlance.getHeight() / aspect);
                mGlanceTo.set(bounds.left, bounds.top + distance, bounds.right, bounds.top + distance + scaledGlanceHeight);
                if (!mGlanceTo.intersect(bounds)) {
                    // glance is not visible
                    return false;
                }

                // computing "from" bounds
                int scaledBoundsHeight = (int) (bounds.height() * aspect);
                mGlanceFrom.set(0, -distanceOnGlance, mGlance.getWidth(), -distanceOnGlance + scaledBoundsHeight);
                // glance is not visible, should not happen due to previous check
                return mGlanceFrom.intersect(0, 0, mGlance.getWidth(), mGlance.getHeight());
            }
        }
        return false;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        mGlance = bitmap;
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) { }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) { }
}