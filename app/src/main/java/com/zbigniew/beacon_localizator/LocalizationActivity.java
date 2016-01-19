package com.zbigniew.beacon_localizator;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;


import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;

public class LocalizationActivity extends AppCompatActivity implements BeaconConsumer, RangeNotifier {

    private static final double EPSILON = 0.2;
    private static final int READ_SDCARD_REQUEST = 123;
    private static final int FINE_LOCATION_REQUEST = 123;
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private BeaconManager mBeaconManager;

    private Map<String,Punkt> punktyBeaconow;
    private double model[]=new double[50];

    private ArrayList<MyBeacon> beacony = new ArrayList<>();
    private ArrayList<Kalman2> kalmanFilters = new ArrayList<Kalman2>();//(0.01,0.5, 50, 1);

    private TextView message;
    private static final int liczbaKombinacji = silnia(4)/(silnia(3) * silnia(4 - 3));;
    private static int[][] result = new int[liczbaKombinacji][3];
    private static final int[] liczbaBeaconow = {0,1,2,3};

    private double[][] punkty = new double[liczbaKombinacji][2];
    private static int counter =0;

    RelativeLayout layout;
    DrawingView dv;

    private double[][] positions;
    private double[] distances;
    private double[] centroid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localization);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        message = (TextView) LocalizationActivity.this.findViewById(R.id.message);

        layout = (RelativeLayout) findViewById(R.id.layout);

        dv = new DrawingView(LocalizationActivity.this);
        //dv.setBackgroundColor(Color.WHITE);
        layout.addView(dv);

        try {
            FileInputStream fis = new FileInputStream("/sdcard/Beacon_Kalibracja_smooth.dat");
            ObjectInputStream iis = new ObjectInputStream(fis);
            model = (double[]) iis.readObject();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        punktyBeaconow = new HashMap<>();
        punktyBeaconow.put("0x6d767674636e", new Punkt(0, 0));//zUUe
        punktyBeaconow.put("0x6f4334313146", new Punkt(2.75,0));//f5p9
        punktyBeaconow.put("0x506b444b4c48", new Punkt(5.20,0.40));//Hf6n
        punktyBeaconow.put("0x72796a446a62", new Punkt(5.5,4.3));//luH8
        punktyBeaconow.put("0x30636169506c", new Punkt(2.75,4.75));//aYjn
        punktyBeaconow.put("0x724335666650", new Punkt(0,4.75));//WxSM





        combinations(liczbaBeaconow, 3, 0, new int[3]);
    }

    public static int silnia(int n) {
        int fact = 1; // this  will be the result
        for (int i = 1; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_localization, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(LocalizationActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(LocalizationActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(LocalizationActivity.this, "Musisz przyznać pozwolenie lokalizacji!", Toast.LENGTH_LONG).show();
                    }
                });
                ActivityCompat.requestPermissions(LocalizationActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        FINE_LOCATION_REQUEST);

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(LocalizationActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        FINE_LOCATION_REQUEST);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        if (ContextCompat.checkSelfPermission(LocalizationActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(LocalizationActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(LocalizationActivity.this, "Musisz przyznać pozwolenie odczytu z karty!", Toast.LENGTH_LONG).show();
                    }
                });
                ActivityCompat.requestPermissions(LocalizationActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_SDCARD_REQUEST);

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(LocalizationActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_SDCARD_REQUEST);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        enableBluetooth();
        initializeBeaconManager();
    }

    private void enableBluetooth() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(LocalizationActivity.this);

        // Setting Dialog Title
        alertDialog.setTitle("Bluetooth wyłączony...");

        // Setting Dialog Message
        alertDialog.setMessage("Czy włączyć?");

        // Setting Positive "Yes" Button
        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                mBluetoothAdapter.enable();
                //initializeBeaconManager();
            }
        });

        // Setting Negative "NO" Button
        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Write your code here to invoke NO event
                Toast.makeText(getApplicationContext(), "Dla poprawnego działania aplikacji Bluetooth musi być włączony!", Toast.LENGTH_LONG).show();
                dialog.cancel();
            }
        });

        if (!isBlEnabled()) {
            alertDialog.show();
        }

    }

    private boolean isBlEnabled() {
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(LocalizationActivity.this, "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
            this.finish();
            System.exit(0);
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                // Bluetooth is not enable :)
                return false;
            }
        }
        return true;
    }


    @Override
    public void onPause() {
        super.onPause();
        mBeaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {

        Region region = new Region("all-beacons-region", null, null, null);
        try {
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.setRangeNotifier(this);
    }

    static void combinations(int[] arr, int len, int startPosition, int[] wynik){
        if (len == 0){
            result[counter][0]=wynik[0];
            result[counter][1]=wynik[1];
            result[counter][2]=wynik[2];
            counter++;
            Log.d("Kombinacje: ", Arrays.toString(result));
            return;
        }
        for (int i = startPosition; i <= arr.length-len; i++){
            wynik[wynik.length - len] = arr[i];
            combinations(arr, len-1, i+1, wynik);
        }
    }

    public void kalmanUpdate(int index, MyBeacon myBeacon){
        Kalman2 kalman = kalmanFilters.get(index);
        myBeacon.setAvgRssi(kalman.update(myBeacon.getRssi()));
    }


    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        for (Beacon beacon : beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                // This is a Eddystone-UID frame
                //Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                MyBeacon myBeacon;
                myBeacon = new MyBeacon(beacon);

                int index;

                if(!beacony.contains(myBeacon)){
                    //myBeacon.addToLastTenRSSi(beacon.getRssi());
                    myBeacon.setPunktBeacona(punktyBeaconow.get(instanceId.toString()));
                    beacony.add(myBeacon);
                    //kalmanFilters.add(new Kalman(0.01, 0.5, 5, 1));
                    kalmanFilters.add(new Kalman2());
                    index = beacony.indexOf(myBeacon);
                    kalmanUpdate(index, myBeacon);
                    myBeacon.setAvgDistance(beaconDistanceFromModel(myBeacon));
                }
                else{
                    index = beacony.indexOf(myBeacon);
                    myBeacon = beacony.get(index);
                    kalmanUpdate(index, myBeacon);
                    //myBeacon.addToLastTenRSSi(beacon.getRssi());

                    myBeacon.setAvgDistance(beaconDistanceFromModel(myBeacon));
                }

            }
        }



        if(beacony.size()>0){
            positions = new double[beacony.size()][2];
            distances = new double[beacony.size()];

            for(int i=0;i<beacony.size();i++){
                positions[i][0]=beacony.get(i).getPunktBeacona().x;
                positions[i][1]=beacony.get(i).getPunktBeacona().y;
                distances[i]=beacony.get(i).getAvgDistance();
            }

            NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
            Optimum optimum = solver.solve();

            // the answer
            centroid = optimum.getPoint().toArray();

            runOnUiThread(new Runnable() {
                public void run() {
                    message.setText("Punkt : \n"+centroid[0]+", \n"+centroid[1]);
                }
            });
        }

        runOnUiThread(new Runnable() {
            public void run() {
                dv.invalidate();
            }
        });

        /*if(beacony.size()==4){
            for (int i=0;i<liczbaKombinacji;i++) {
                if (calculateTwoCirclesIntersectionPoints(beacony.get(result[i][0]),beacony.get(result[i][1]),beacony.get(result[i][2]),i)) {
                    calculateThreeCircleIntersection(beacony.get(result[i][2]),i);
                }
            }
        }
*/

    }

    private double beaconDistanceFromModel(MyBeacon beacon){
            double myNumber = beacon.getAvgRssi();
            double distance = Math.abs(model[0] - myNumber);
            double idx = 0;
            for(int c = 1; c < model.length; c++){
                double cdistance = Math.abs(model[c] - myNumber);
                if(cdistance < distance){
                    idx = c;
                    distance = cdistance;
                }
            }

        double avgDistance = (idx+1)/10;
        return avgDistance;
    }

    private void initializeBeaconManager() {
        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        // Detect the main Eddystone-UID frame:
        //mBeaconManager.getBeaconParsers().add(new BeaconParser()
        //      .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));
        mBeaconManager.bind(this);
    }

    class DrawingView extends View{

        // setup initial color
        private final int paintColor = Color.BLACK;
        // defines paint and canvas
        private Paint drawPaint;

        public DrawingView(Context context) {
            super(context);
            setFocusable(true);
            setFocusableInTouchMode(true);
            setupPaint();
        }

        // Setup paint with color and stroke styles
        private void setupPaint() {
            drawPaint = new Paint();
            drawPaint.setColor(paintColor);
            drawPaint.setAntiAlias(true);
            drawPaint.setStrokeWidth(5);
            drawPaint.setStyle(Paint.Style.STROKE);
            drawPaint.setStrokeJoin(Paint.Join.ROUND);
            drawPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            //float ratioX = canvas.getWidth()/5, ratioY = canvas.getHeight()/7;
            if(beacony.size()>0){
                for(MyBeacon mb : beacony){
                    switch (mb.getId2().toString()){
                        case "0x6d767674636e":

                            float r = (float)mb.getAvgDistance()*200;
                            canvas.drawCircle(30, 150, r, drawPaint);
                            break;
                        case "0x6f4334313146":
                            drawPaint.setColor(Color.GREEN);
                            r = (float)mb.getAvgDistance()*200;
                            canvas.drawCircle(30, 700, r, drawPaint);
                            break;
                        case "0x506b444b4c48":
                            drawPaint.setColor(Color.BLUE);
                            r = (float)mb.getAvgDistance()*200;
                            canvas.drawCircle(30, 1250, r, drawPaint);
                            break;
                        case "0x72796a446a62":
                            drawPaint.setColor(Color.CYAN);
                            r = (float)mb.getAvgDistance()*200;
                            canvas.drawCircle(980, 1250, r, drawPaint);
                            break;
                        case "0x30636169506c":
                            drawPaint.setColor(Color.MAGENTA);
                            r = (float)mb.getAvgDistance()*200;
                            canvas.drawCircle(980, 700, r, drawPaint);
                            break;
                        case "0x724335666650":
                            drawPaint.setColor(Color.RED);
                            r = (float)mb.getAvgDistance()*200;
                            canvas.drawCircle(980, 150, r, drawPaint);
                            break;
                    }

                }

                drawPaint.setColor(Color.BLACK);
                float x = (float)centroid[0]*200+30, y=(float)centroid[1]*200+150;
                canvas.drawCircle(x,y, 5, drawPaint);
            }

        }
    }
}
