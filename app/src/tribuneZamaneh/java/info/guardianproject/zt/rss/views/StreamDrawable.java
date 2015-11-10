package info.guardianproject.zt.rss.views;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.Gravity;

public class StreamDrawable extends Drawable {
    private static boolean useVignette = true;

    private static final float DEFAULT_CORNER_RADIUS = 0.07f; // 8%
    private final float mCornerRadius; // in range 0.0f - 0.5f

    private final RectF mRect = new RectF();
    private final BitmapShader mBitmapShader;
    private final Paint mPaint;

    private int originalWidth = -1;
    private int originalHeight = -1;

    /**
     * Round corners drawable (0.07f) without vignette (false)
     *
     * @param bitmap
     */
    public StreamDrawable(Bitmap bitmap) {
        this(bitmap, DEFAULT_CORNER_RADIUS, false);
    }

    /**
     * Round corners drawable (0.07f) with vignette
     *
     * @param bitmap
     */
    public StreamDrawable(Bitmap bitmap, boolean useVignette) {
        this(bitmap, DEFAULT_CORNER_RADIUS, useVignette);
    }

    /**
     * Round corners drawable without vignette (false)
     *
     * @param bitmap
     * @param cornerRadius - radius of curvature relative to the image size (must be in range 0.0f - 0.5f)
     */
    public StreamDrawable(Bitmap bitmap, float cornerRadius) {
        this(bitmap, cornerRadius, false);
    }

    /**
     * Round corners drawable with vignette
     *
     * @param bitmap
     * @param cornerRadius - radius of curvature relative to the image size (must be in range 0.0f - 0.5f)
     * @param useVignette
     */
    public StreamDrawable(Bitmap bitmap, float cornerRadius, boolean useVignette) {
        // check corner radius range
        if (0 > cornerRadius){
            cornerRadius = 0;
        }else if (0.5f < cornerRadius){
            cornerRadius = 0.5f;
        }
        this.mCornerRadius = cornerRadius;
        this.useVignette = useVignette;
        this.originalWidth = bitmap.getWidth();
        this.originalHeight = bitmap.getHeight();

        mBitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setShader(mBitmapShader);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mRect.set(0, 0, bounds.width(), bounds.height());

        // gravity for shader
        Rect mBitmapRect = new Rect();
        Gravity.apply(Gravity.CENTER, originalWidth, originalHeight, new Rect((int) mRect.left, (int) mRect.top, (int) mRect.width(), (int) mRect.height()), mBitmapRect);
        Matrix mMatrix = new Matrix();
        mBitmapShader.getLocalMatrix(mMatrix);
        mMatrix.setTranslate(mBitmapRect.left, mBitmapRect.top);
        mBitmapShader.setLocalMatrix(mMatrix);

        if (useVignette) {
            LinearGradient vignette = new LinearGradient(mRect.width() / 2, 0, mRect.width() / 2, mRect.height() / 3, 0xCCFFFFFF, 0, Shader.TileMode.CLAMP);
            Matrix oval = new Matrix();
            oval.setScale(1.0f, 0.7f);
            vignette.setLocalMatrix(oval);

            mPaint.setShader(new ComposeShader(mBitmapShader, vignette, PorterDuff.Mode.SRC_OVER));
        }
    }

    @Override
    public int getIntrinsicWidth() {
        if (-1 != originalWidth && -1 != originalHeight) {
            return Math.min(originalWidth, originalHeight);
        } else {
            return super.getIntrinsicWidth();
        }
    }

    @Override
    public int getIntrinsicHeight() {
        if (-1 != originalWidth && -1 != originalHeight) {
            return Math.min(originalWidth, originalHeight);
        } else {
            return super.getIntrinsicHeight();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        float radiusInPixels = mCornerRadius * Math.min(mRect.width(), mRect.height());
        canvas.drawRoundRect(mRect, radiusInPixels, radiusInPixels, mPaint);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }
}