package com.example.start4me_2.cameratouchfocus;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements SurfaceHolder.Callback{

        Camera camera;
        Camera1 cameraSurfaceView;
        SurfaceHolder surfaceHolder;
        boolean previewing = false;
        LayoutInflater controlInflater = null;

        Button buttonTakePicture;
        TextView prompt;

        DrawingView drawingView;
        android.hardware.Camera.Face[] detectedFaces;

final int RESULT_SAVEIMAGE = 0;

private ScheduledExecutorService myScheduledExecutorService;

/** Called when the activity is first created. */
@Override
public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getWindow().setFormat(PixelFormat.UNKNOWN);
        cameraSurfaceView = (Camera1) findViewById(R.id.camerapreview);
        surfaceHolder = cameraSurfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        drawingView = new DrawingView(this);
        FrameLayout.LayoutParams layoutParamsDrawing
        = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT,
        FrameLayout.LayoutParams.FILL_PARENT);
        this.addContentView(drawingView, layoutParamsDrawing);

        controlInflater = LayoutInflater.from(getBaseContext());
        View viewControl = controlInflater.inflate(R.layout.control, null);
        FrameLayout.LayoutParams layoutParamsControl
        = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT,
        FrameLayout.LayoutParams.FILL_PARENT);
        this.addContentView(viewControl, layoutParamsControl);

        buttonTakePicture = (Button)findViewById(R.id.takepicture);
        buttonTakePicture.setOnClickListener(new Button.OnClickListener(){

@Override
public void onClick(View arg0) {
        // TODO Auto-generated method stub
        camera.takePicture(myShutterCallback,
        myPictureCallback_RAW, myPictureCallback_JPG);
        }});

        /*
        LinearLayout layoutBackground = (LinearLayout)findViewById(R.id.background);
        layoutBackground.setOnClickListener(new LinearLayout.OnClickListener(){

   @Override
   public void onClick(View arg0) {
    // TODO Auto-generated method stub

    buttonTakePicture.setEnabled(false);
    camera.autoFocus(myAutoFocusCallback);
   }});
  */

        prompt = (TextView)findViewById(R.id.prompt);
        }

public void touchFocus(final Rect tfocusRect){

        buttonTakePicture.setEnabled(false);

        camera.stopFaceDetection();

//Convert from View's width and height to +/- 1000
final Rect targetFocusRect = new Rect(
        tfocusRect.left * 2000/drawingView.getWidth() - 1000,
        tfocusRect.top * 2000/drawingView.getHeight() - 1000,
        tfocusRect.right * 2000/drawingView.getWidth() - 1000,
        tfocusRect.bottom * 2000/drawingView.getHeight() - 1000);

final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
        Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
        focusList.add(focusArea);

        android.hardware.Camera.Parameters para = camera.getParameters();
        para.setFocusAreas(focusList);
        para.setMeteringAreas(focusList);
        camera.setParameters(para);

        camera.autoFocus(myAutoFocusCallback);

        drawingView.setHaveTouch(true, tfocusRect);
        drawingView.invalidate();
        }

        android.hardware.Camera.FaceDetectionListener faceDetectionListener
        = new android.hardware.Camera.FaceDetectionListener(){

@Override
public void onFaceDetection(android.hardware.Camera.Face[] faces, Camera tcamera) {

        if (faces.length == 0){
        //prompt.setText(" No Face Detected! ");
        drawingView.setHaveFace(false);
        }else{
        //prompt.setText(String.valueOf(faces.length) + " Face Detected :) ");
        drawingView.setHaveFace(true);
        detectedFaces = faces;

        //Set the FocusAreas using the first detected face
        List<Camera.Area> focusList = new ArrayList<Camera.Area>();
        Camera.Area firstFace = new Camera.Area(faces[0].rect, 1000);
        focusList.add(firstFace);

        Camera.Parameters para = camera.getParameters();

        if(para.getMaxNumFocusAreas()>0){
        para.setFocusAreas(focusList);
        }

        if(para.getMaxNumMeteringAreas()>0){
        para.setMeteringAreas(focusList);
        }

        camera.setParameters(para);

        buttonTakePicture.setEnabled(false);

        //Stop further Face Detection
        camera.stopFaceDetection();

        buttonTakePicture.setEnabled(false);

    /*
     * Allways throw java.lang.RuntimeException: autoFocus failed
     * if I call autoFocus(myAutoFocusCallback) here!
     *
     camera.autoFocus(myAutoFocusCallback);
    */

        //Delay call autoFocus(myAutoFocusCallback)
        myScheduledExecutorService = Executors.newScheduledThreadPool(1);
        myScheduledExecutorService.schedule(new Runnable(){
public void run() {
        camera.autoFocus(myAutoFocusCallback);
        }
        }, 500, TimeUnit.MILLISECONDS);

        }

        drawingView.invalidate();

        }};

        Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback(){

@Override
public void onAutoFocus(boolean arg0, Camera arg1) {
        // TODO Auto-generated method stub
        if (arg0){
        buttonTakePicture.setEnabled(true);
        camera.cancelAutoFocus();
        }

        float focusDistances[] = new float[3];
        arg1.getParameters().getFocusDistances(focusDistances);
        prompt.setText("Optimal Focus Distance(meters): "
        + focusDistances[Camera.Parameters.FOCUS_DISTANCE_OPTIMAL_INDEX]);

        }};

        Camera.ShutterCallback myShutterCallback = new Camera.ShutterCallback(){

@Override
public void onShutter() {
        // TODO Auto-generated method stub

        }};

        Camera.PictureCallback myPictureCallback_RAW = new Camera.PictureCallback(){

@Override
public void onPictureTaken(byte[] arg0, Camera arg1) {
        // TODO Auto-generated method stub

        }
        };

        Camera.PictureCallback myPictureCallback_JPG = new Camera.PictureCallback(){

@Override
public void onPictureTaken(byte[] arg0, Camera arg1) {
        // TODO Auto-generated method stub
   /*Bitmap bitmapPicture
    = BitmapFactory.decodeByteArray(arg0, 0, arg0.length); */

        Uri uriTarget = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());

        OutputStream imageFileOS;
        try {
        imageFileOS = getContentResolver().openOutputStream(uriTarget);
        imageFileOS.write(arg0);
        imageFileOS.flush();
        imageFileOS.close();

        prompt.setText("Image saved: " + uriTarget.toString());

        } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        }

        camera.startPreview();
        camera.startFaceDetection();
        }};

@Override
public void surfaceChanged(SurfaceHolder holder, int format, int width,
        int height) {
        // TODO Auto-generated method stub
        if(previewing){
        camera.stopFaceDetection();
        camera.stopPreview();
        previewing = false;
        }

        if (camera != null){
        try {
        camera.setPreviewDisplay(surfaceHolder);
        camera.startPreview();

        prompt.setText(String.valueOf(
        "Max Face: " + camera.getParameters().getMaxNumDetectedFaces()));
        camera.startFaceDetection();
        previewing = true;
        } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        }
        }
        }

@Override
public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        camera = Camera.open();
        camera.setFaceDetectionListener(faceDetectionListener);
        }

@Override
public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        camera.stopFaceDetection();
        camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
        }
    private class DrawingView extends View{

        boolean haveFace;
        Paint drawingPaint;

        boolean haveTouch;
        Rect touchArea;

        public DrawingView(Context context) {
            super(context);
            haveFace = false;
            drawingPaint = new Paint();
            drawingPaint.setColor(Color.GREEN);
            drawingPaint.setStyle(Paint.Style.STROKE);
            drawingPaint.setStrokeWidth(2);

            haveTouch = false;
        }

        public void setHaveFace(boolean h){
            haveFace = h;
        }

        public void setHaveTouch(boolean t, Rect tArea){
            haveTouch = t;
            touchArea = tArea;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // TODO Auto-generated method stub
            if(haveFace){

                // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
                // UI coordinates range from (0, 0) to (width, height).

                int vWidth = getWidth();
                int vHeight = getHeight();

                for(int i=0; i<detectedFaces.length; i++){

                    if(i == 0){
                        drawingPaint.setColor(Color.GREEN);
                    }else{
                        drawingPaint.setColor(Color.RED);
                    }

                    int l = detectedFaces[i].rect.left;
                    int t = detectedFaces[i].rect.top;
                    int r = detectedFaces[i].rect.right;
                    int b = detectedFaces[i].rect.bottom;
                    int left = (l+1000) * vWidth/2000;
                    int top  = (t+1000) * vHeight/2000;
                    int right = (r+1000) * vWidth/2000;
                    int bottom = (b+1000) * vHeight/2000;
                    canvas.drawRect(
                            left, top, right, bottom,
                            drawingPaint);
                }
            }else{
                canvas.drawColor(Color.TRANSPARENT);
            }

            if(haveTouch){
                drawingPaint.setColor(Color.BLUE);
                canvas.drawRect(
                        touchArea.left, touchArea.top, touchArea.right, touchArea.bottom,
                        drawingPaint);
            }
        }

    }
}