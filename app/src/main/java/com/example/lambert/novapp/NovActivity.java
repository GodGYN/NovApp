package com.example.lambert.novapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class NovActivity extends AppCompatActivity implements SensorEventListener{

    private Button start;
    private Button end;
    private Button calRMWorldToBody;

    private SensorManager sensorManager;
    private Sensor gravSensor;
    private Sensor magSensor;
    private Sensor rotaVectSensor;

    private boolean hasCalWorldToBody = false;

    private boolean showResult = true;

    float[] gravValues = new float[3];
    float[] magValues = new float[3];
    float[] rotationVectorValues = new float[4];

    float[] xAxis = {1, 0, 0};
    float[] yAxis = {0, 1, 0};
    float[] zAxis = {0, 0, 1};

    float[] worldToBodyRotationMatrix = new float[9];
    float[] watchToWorldRotationMatrix = new float[9];

    Thread showThetaData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nov);

        start = (Button) findViewById(R.id.start);
        end = (Button) findViewById(R.id.end);
        calRMWorldToBody = (Button) findViewById(R.id.calWorldToBody);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //show all sensors
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for(Sensor sensor:sensorList){
            Log.d("allSensorList:", "Sensor type "+sensor.getType()+" name="+sensor.getName());
        }

        gravSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        rotaVectSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("sensor status:", "start");
                sensorManager.registerListener(NovActivity.this, gravSensor, SensorManager.SENSOR_DELAY_GAME);
                sensorManager.registerListener(NovActivity.this, magSensor, SensorManager.SENSOR_DELAY_GAME);
                sensorManager.registerListener(NovActivity.this, rotaVectSensor, SensorManager.SENSOR_DELAY_GAME);
                showThetaData = new Thread(new showTheta());
                showResult = true;
                showThetaData.start();
            }
        });

        end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("sensor status:", "end");
                showResult = false;
                sensorManager.unregisterListener(NovActivity.this);
            }
        });


        //calculate world to body rotation matrix
        calRMWorldToBody.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hasCalWorldToBody = true;
                float[] bodyToWorldMatrix = new float[9];
                SensorManager.getRotationMatrix(bodyToWorldMatrix, null, gravValues, magValues);
                worldToBodyRotationMatrix[0] = bodyToWorldMatrix[0];
                worldToBodyRotationMatrix[1] = bodyToWorldMatrix[3];
                worldToBodyRotationMatrix[2] = bodyToWorldMatrix[6];
                worldToBodyRotationMatrix[3] = bodyToWorldMatrix[1];
                worldToBodyRotationMatrix[4] = bodyToWorldMatrix[4];
                worldToBodyRotationMatrix[5] = bodyToWorldMatrix[7];
                worldToBodyRotationMatrix[6] = bodyToWorldMatrix[2];
                worldToBodyRotationMatrix[7] = bodyToWorldMatrix[5];
                worldToBodyRotationMatrix[8] = bodyToWorldMatrix[8];
            }
        });

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY){
            for(int i = 0; i < gravValues.length; i++){
                gravValues[0] = sensorEvent.values[0];
                gravValues[1] = sensorEvent.values[1];
                gravValues[2] = sensorEvent.values[2];
            }
            SaveData(gravValues, "gravValues.csv");
        }else if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            for(int i = 0; i < magValues.length; i++){
                magValues[0] = sensorEvent.values[0];
                magValues[1] = sensorEvent.values[1];
                magValues[2] = sensorEvent.values[2];
            }
            SaveData(magValues, "magValues.csv");
        }
        else if(sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            for(int i = 0; i < rotationVectorValues.length; i++){
                rotationVectorValues[0] = sensorEvent.values[0];
                rotationVectorValues[1] = sensorEvent.values[1];
                rotationVectorValues[2] = sensorEvent.values[2];
                rotationVectorValues[3] = sensorEvent.values[3];
            }
            SaveData(rotationVectorValues, "rotationVectorValues.csv");
        }
    }

    //calculate the angle between the axis and the gravity
    private float calTheta(float[] gravValues, float[] axis) {
        float result;
        float x= gravValues[0];
        float y = gravValues[1];
        float z = gravValues[2];
        result = (float) (Math.acos((axis[0] * x + axis[1] * y + axis[2] * z) / Math.sqrt(x*x + y*y + z * z)) * 180 / Math.PI);
        return result;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    class showTheta implements Runnable {
        @Override
        public void run(){
            while(showResult){
                SensorManager.getRotationMatrix(watchToWorldRotationMatrix, null, gravValues, magValues);

                //calculate the conversion from x-axis to world coordinate system
                float[] worldAxis = new float[3];
                worldAxis[0] = watchToWorldRotationMatrix[0];
                worldAxis[1] = watchToWorldRotationMatrix[3];
                worldAxis[2] = watchToWorldRotationMatrix[6];

                float xTheta;
                float yTheta;
                float zTheta;
                if(!hasCalWorldToBody){
                    xTheta = calTheta(worldAxis, xAxis);
                    yTheta = calTheta(worldAxis, yAxis);
                    zTheta = calTheta(worldAxis, zAxis);

                    Log.d("worldAxisAngle:", "xTheta = " + xTheta + ", yTheta = " + yTheta + ", zTheta = " + zTheta);
                    float[] worldAxisAngle = {xTheta, yTheta, zTheta};
                    SaveData(worldAxis, "worldAxis.csv");
                    SaveData(worldAxisAngle, "worldAxisAngle.csv");
                }else{
                    float[] bodyAxis = new float[3];
                    bodyAxis[0] = worldAxis[0] * worldToBodyRotationMatrix[0] + worldAxis[1] * worldToBodyRotationMatrix[1] + worldAxis[2] * worldToBodyRotationMatrix[2];
                    bodyAxis[1] = worldAxis[0] * worldToBodyRotationMatrix[3] + worldAxis[1] * worldToBodyRotationMatrix[4] + worldAxis[2] * worldToBodyRotationMatrix[5];
                    bodyAxis[2] = worldAxis[0] * worldToBodyRotationMatrix[6] + worldAxis[1] * worldToBodyRotationMatrix[7] + worldAxis[2] * worldToBodyRotationMatrix[8];

                    SaveData(bodyAxis, "bodyAxis.csv");

                    xTheta = calTheta(bodyAxis, xAxis);
                    yTheta = calTheta(bodyAxis, yAxis);
                    zTheta = calTheta(bodyAxis, zAxis);

                    Log.d("bodyAxisAngle:", "xTheta = " + xTheta + ", yTheta = " + yTheta + ", zTheta = " + zTheta);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void SaveData(float[] data, String fileName) {
        File myfile = new File(Environment.getExternalStorageDirectory(), fileName);
        try {
            FileOutputStream outputStream = new FileOutputStream(myfile, true);
            String bys = "" + data[0];
            for(int i = 1; i < data.length; i++){
                bys = bys + "," +data[i];
            }
            bys = bys + "\n";
            outputStream.write(bys.getBytes());
            outputStream.close();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "数据存储失败", Toast.LENGTH_SHORT).show();
        }
    }
}
