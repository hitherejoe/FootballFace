package com.hitherejoe.nflwear.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.hitherejoe.nflwear.R;
import com.hitherejoe.nflwear.util.WatchFaceUtil;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class WatchFaceService extends CanvasWatchFaceService {

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private Bitmap mBackgroundBitmap;
    private Bitmap mGreyBackgroundBitmap;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        private GoogleApiClient mGoogleApiClient;
        private Handler mUpdateTimeHandler;

        private BroadcastReceiver mTimeZoneReceiver;

        private boolean mRegisteredTimeZoneReceiver;

        private static final float STROKE_WIDTH = 3f;

        private static final float HOUR_STROKE_WIDTH = 5f;
        private static final float MINUTE_STROKE_WIDTH = 3f;
        private static final float SECOND_STROKE_WIDTH = 2f;
        private static final float CIRCLE_STROKE_WIDTH = 2f;
        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;
        private static final int SHADOW_RADIUS = 6;

        private Calendar mCalendar;

        private Paint mBackgroundPaint;
        private Paint mHandPaint;
        private int mBackgroundColor;

        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        private float mHourHandLength;
        private float mMinuteHandLength;
        private float mSecondHandLength;

        private int mWidth;
        private int mHeight;
        private float mCenterX;
        private float mCenterY;

        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mCirclePaint;

        private int mWatchHandColor;
        private int mWatchHandHightlightColor;
        private int mWatchHandShadowColor;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            mGoogleApiClient = new GoogleApiClient.Builder(WatchFaceService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();

            mRegisteredTimeZoneReceiver = false;
            mTimeZoneReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mCalendar.setTimeZone(TimeZone.getDefault());
                    invalidate();
                }
            };

            mUpdateTimeHandler = new Handler() {
                @Override
                public void handleMessage(Message message) {
                    if (R.id.message_update == message.what) {
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(R.id.message_update, delayMs);
                        }
                    }
                }
            };

            setupPaintStyles();
            mCalendar = Calendar.getInstance();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(R.id.message_update);
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            if (mLowBitAmbient || mBurnInProtection) mHandPaint.setAntiAlias(!inAmbientMode);
            updateWatchHandStyle();
            invalidate();
            updateTimer();
        }

        private void setupPaintStyles() {
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);
            mBackgroundColor =
                    ContextCompat.getColor(WatchFaceService.this, R.color.default_color);
            mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_face);

            mHandPaint = new Paint();
            mHandPaint.setColor(Color.WHITE);
            mHandPaint.setStrokeWidth(STROKE_WIDTH);
            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mWatchHandColor = Color.WHITE;
            mWatchHandHightlightColor = Color.RED;
            mWatchHandShadowColor = Color.BLACK;

            mHourPaint = new Paint();
            mHourPaint.setColor(mWatchHandColor);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mWatchHandColor);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mWatchHandHightlightColor);
            mSecondPaint.setStrokeWidth(SECOND_STROKE_WIDTH);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);
            mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mCirclePaint = new Paint();
            mCirclePaint.setColor(mWatchHandColor);
            mCirclePaint.setStrokeWidth(CIRCLE_STROKE_WIDTH);
            mCirclePaint.setAntiAlias(true);
            mCirclePaint.setStyle(Paint.Style.STROKE);
            mCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
        }

        private void updateWatchHandStyle() {
            if (mAmbient) {
                mHourPaint.setColor(Color.WHITE);
                mMinutePaint.setColor(Color.WHITE);
                mSecondPaint.setColor(Color.WHITE);
                mCirclePaint.setColor(Color.WHITE);

                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
                mSecondPaint.setAntiAlias(false);
                mCirclePaint.setAntiAlias(false);

                mHourPaint.clearShadowLayer();
                mMinutePaint.clearShadowLayer();
                mSecondPaint.clearShadowLayer();
                mCirclePaint.clearShadowLayer();
            } else {
                mHourPaint.setColor(mWatchHandColor);
                mMinutePaint.setColor(mWatchHandColor);
                mSecondPaint.setColor(mWatchHandHightlightColor);
                mCirclePaint.setColor(mWatchHandColor);

                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
                mSecondPaint.setAntiAlias(true);
                mCirclePaint.setAntiAlias(true);

                mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mWidth = width;
            mHeight = height;
            mCenterX = mWidth / 2f;
            mCenterY = mHeight / 2f;

            mHourHandLength = mCenterX - 80;
            mMinuteHandLength = mCenterX - 40;
            mSecondHandLength = mCenterX - 20;
            float scale = ((float) width) / (float) mBackgroundBitmap.getWidth();
            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (int) (mBackgroundBitmap.getWidth() * scale),
                    (int) (mBackgroundBitmap.getHeight() * scale), true);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(mBackgroundColor);
            } else {
                Bitmap backgroundBitmap = mAmbient ? mGreyBackgroundBitmap : mBackgroundBitmap;
                canvas.drawColor(mBackgroundColor);
                canvas.drawBitmap(backgroundBitmap, 0, 0, mBackgroundPaint);
            }
            mHandPaint.setAntiAlias(!mAmbient);

            if (!mAmbient) {
                Palette palette = Palette.from(mBackgroundBitmap).generate();
                mWatchHandHightlightColor = palette.getVibrantColor(Color.RED);
                mWatchHandColor = palette.getLightVibrantColor(Color.WHITE);
                mWatchHandShadowColor = palette.getDarkMutedColor(Color.BLACK);
                updateWatchHandStyle();
            }

            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mMinuteHandLength,
                    mMinutePaint);

            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondPaint);
            }

            canvas.drawCircle(
                    mCenterX,
                    mCenterY,
                    CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCirclePaint);

            canvas.restore();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();
                registerReceiver();
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }
            updateTimer();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            for (DataEvent dataEvent : dataEvents) {
                if (dataEvent.getType() != DataEvent.TYPE_CHANGED) continue;

                DataItem dataItem = dataEvent.getDataItem();
                if (!dataItem.getUri().getPath().equals(WatchFaceUtil.PATH_WITH_FEATURE)) continue;

                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap config = dataMapItem.getDataMap();
                updateUiForConfigDataMap(config);
            }
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
            updateConfigDataItemAndUiOnStartup();
        }

        private void updateConfigDataItemAndUiOnStartup() {
            WatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
                    new WatchFaceUtil.FetchConfigDataMapCallback() {
                        @Override
                        public void onConfigDataMapFetched(DataMap startupConfig) {
                            WatchFaceUtil.putConfigDataItem(mGoogleApiClient, startupConfig);
                            updateUiForConfigDataMap(startupConfig);
                        }
                    }
            );
        }

        private void updateUiForConfigDataMap(final DataMap config) {
            boolean uiUpdated = false;
            for (String configKey : config.keySet()) {
                if (!config.containsKey(configKey)) continue;
                if (updateUiForKey(configKey, config)) uiUpdated = true;
            }
            if (uiUpdated) {
                initGreyBackgroundBitmap();
                invalidate();
            }
        }

        private boolean updateUiForKey(String configKey, DataMap config) {
            if (configKey.equals(WatchFaceUtil.KEY_TEAM_COLOR)) {
                config.getInt(configKey);
                mBackgroundColor =
                        ContextCompat.getColor(WatchFaceService.this, config.getInt(configKey));
            } else if (configKey.equals(WatchFaceUtil.KEY_TEAM_LOGO)) {
                mBackgroundBitmap =
                        BitmapFactory.decodeResource(getResources(), config.getInt(configKey));
                float scale = ((float) mWidth) / (float) mBackgroundBitmap.getWidth();
                mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        (int) (mBackgroundBitmap.getWidth() * scale),
                        (int) (mBackgroundBitmap.getHeight() * scale), true);
                Palette palette = Palette.from(mBackgroundBitmap).generate();
                mHandPaint.setColor(palette.getLightMutedColor(Color.GRAY));
            } else {
                return false;
            }
            return true;
        }

        @Override
        public void onConnectionSuspended(int cause) { }

        @Override
        public void onConnectionFailed(ConnectionResult result) { }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) return;
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            WatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) return;
            mRegisteredTimeZoneReceiver = false;
            WatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(R.id.message_update);
            if (shouldTimerBeRunning()) mUpdateTimeHandler.sendEmptyMessage(R.id.message_update);
        }

        private void initGreyBackgroundBitmap() {
            mGreyBackgroundBitmap = Bitmap.createBitmap(mBackgroundBitmap.getWidth(),
                    mBackgroundBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mGreyBackgroundBitmap);
            Paint greyPaint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            greyPaint.setColorFilter(filter);
            canvas.drawColor(convertBackgroundColorToGreyscale(mBackgroundColor));
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, greyPaint);
        }

        private int convertBackgroundColorToGreyscale(int color) {
            final double GS_RED = 0.299;
            final double GS_GREEN = 0.587;
            final double GS_BLUE = 0.114;

            int A = Color.alpha(color);
            int R = Color.red(color);
            int G = Color.green(color);
            int B = Color.blue(color);

            R = G = B = (int) (GS_RED * R + GS_GREEN * G + GS_BLUE * B);
            return Color.argb(A, R, G, B);
        }

        public boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }

}