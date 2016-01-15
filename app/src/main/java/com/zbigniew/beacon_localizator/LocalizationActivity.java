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

    private double intersectionPoint1_x, intersectionPoint2_x, intersectionPoint1_y, intersectionPoint2_y;

    private TextView message;
    private static final int liczbaKombinacji = silnia(4)/(silnia(3) * silnia(4 - 3));;
    private static int[][] result = new int[liczbaKombinacji][3];
    private static final int[] liczbaBeaconow = {0,1,2,3};

    private double[][] punkty = new double[liczbaKombinacji][2];
    private static int counter =0;

    RelativeLayout layout;
    DrawingView dv;

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        try {
            FileInputStream fis = new FileInputStream("/sdcard/Beacon_Kalibracja_smooth.dat");
            ObjectInputStream iis = new ObjectInputStream(fis);
            model = (double[]) iis.readObject();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        punktyBeaconow = new HashMap<>();
        punktyBeaconow.put("0x6d767674636e", new Punkt(0, 0));//zUUe
        punktyBeaconow.put("0x6f4334313146", new Punkt(0,3.5));//f5p9
        punktyBeaconow.put("0x506b444b4c48", new Punkt(0,7));//Hf6n
        punktyBeaconow.put("0x72796a446a62", new Punkt(7,5));//luH8
        punktyBeaconow.put("0x30636169506c", new Punkt(3.5,5));//aYjn
        punktyBeaconow.put("0x724335666650", new Punkt(0,5));//WxSM





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

    private boolean calculateTwoCirclesIntersectionPoints(MyBeacon b0, MyBeacon b1, MyBeacon b2, int iter){
        double a, dx, dy, d, h, rx, ry;
        double point2_x, point2_y;

    /* dx and dy are the vertical and horizontal distances between
    * the circle centers.
    */
        dx = b1.getPunktBeacona().x - b0.getPunktBeacona().x;
        dy = b1.getPunktBeacona().y - b0.getPunktBeacona().y;

    /* Determine the straight-line distance between the centers. */
        d = sqrt((dy * dy) + (dx * dx));

    /* Check for solvability. */
        if (d > (b0.getAvgDistance() + b1.getAvgDistance())) {
        /* no solution. circles do not intersect. */
            b0.addToAvgDistance(0.2);
            b1.addToAvgDistance(0.2);
            if(calculateTwoCirclesIntersectionPoints(b0,b1,b2, iter)){
                calculateThreeCircleIntersection(b2, iter);
            }
            return  false;
        }
        if (d < abs(b0.getAvgDistance() - b1.getAvgDistance())) {
        /* no solution. one circle is contained in the other */
            return false;
        }

    /* 'point 2' is the point where the line through the circle
    * intersection points crosses the line between the circle
    * centers.
    */

    /* Determine the distance from point 0 to point 2. */
        a = ((b0.getAvgDistance() * b0.getAvgDistance()) - (b1.getAvgDistance() * b1.getAvgDistance()) + (d * d)) / (2.0 * d);

    /* Determine the coordinates of point 2. */
        point2_x = b0.getPunktBeacona().x + (dx * a / d);
        point2_y = b0.getPunktBeacona().y + (dy * a / d);

    /* Determine the distance from point 2 to either of the
    * intersection points.
    */
        h = sqrt((b0.getAvgDistance() * b0.getAvgDistance()) - (a * a));

    /* Now determine the offsets of the intersection points from
    * point 2.
    */
        rx = -dy * (h / d);
        ry = dx * (h / d);

    /* Determine the absolute intersection points. */
        intersectionPoint1_x = point2_x + rx;
        intersectionPoint2_x = point2_x - rx;
        intersectionPoint1_y = point2_y + ry;
        intersectionPoint2_y = point2_y - ry;

        //Log.d("TAG", "INTERSECTION Circle1 AND Circle2: (" + intersectionPoint1_x + "," + intersectionPoint1_y + ")" + " AND (" + intersectionPoint2_x + "," + intersectionPoint2_y + ")");
        /*runOnUiThread(new Runnable() {
            public void run() {
                ((TextView) LocalizationActivity.this.findViewById(R.id.message)).setText("r0= "+r0+"\n r1= "+r1);
            }
        });*/
        return true;
    }


    private boolean calculateThreeCircleIntersection(MyBeacon beacon, int iter) {
        double dx, dy;

    /* Lets determine if circle 3 intersects at either of the above intersection points. */
        dx = intersectionPoint1_x - beacon.getPunktBeacona().x;
        dy = intersectionPoint1_y - beacon.getPunktBeacona().y;
        double d1 = sqrt((dy * dy) + (dx * dx));

        dx = intersectionPoint2_x - beacon.getPunktBeacona().x;
        dy = intersectionPoint2_y - beacon.getPunktBeacona().y;
        double d2 = sqrt((dy * dy) + (dx * dx));

        if (abs(d1 - beacon.getAvgDistance()) < EPSILON) {
            punkty[iter][0]=intersectionPoint1_x;
            punkty[iter][1]=intersectionPoint1_y;
            /*Log.d("TAG", "INTERSECTION Circle1 AND Circle2 AND Circle3: " + "(" + intersectionPoint1_x + "," + intersectionPoint1_y + ")");
            runOnUiThread(new Runnable() {
                public void run() {
                    message.setText("INTERSECTION Circle1 AND Circle2 AND Circle3: " + "(\n" + intersectionPoint1_x + ",\n" + intersectionPoint1_y + ")");
                }
            });*/
        } else if (abs(d2 - beacon.getAvgDistance()) < EPSILON) {
            punkty[iter][0]=intersectionPoint2_x;
            punkty[iter][1]=intersectionPoint2_y;
            //Log.d("TAG", "INTERSECTION Circle1 AND Circle2 AND Circle3: (" + intersectionPoint2_x + "," + intersectionPoint2_y + ")");
            /*runOnUiThread(new Runnable() {
                public void run() {
                    message.setText("INTERSECTION Circle1 AND Circle2 AND Circle3: (\n" + intersectionPoint2_x + ",\n" + intersectionPoint2_y + ")");
                }
            });*/
        } else {
            /*Log.d("TAG", "INTERSECTION Circle1 AND Circle2 AND Circle3: NONE");
            runOnUiThread(new Runnable() {
                public void run() {
                    message.setText("INTERSECTION Circle1 AND Circle2 AND Circle3: NONE");
                }
            });*/

            if(abs(d1 - beacon.getAvgDistance())<abs(d2 - beacon.getAvgDistance())){
                if(d1<beacon.getAvgDistance()){
                    beacon.addToAvgDistance(0.1);
                }
                else{
                    beacon.minusToAvgDistance(0.1);
                }
                calculateThreeCircleIntersection(beacon, iter);
            }
            else{
                if(d2<beacon.getAvgDistance()){
                    beacon.addToAvgDistance(0.1);
                }
                else{
                    beacon.minusToAvgDistance(0.1);
                }
                calculateThreeCircleIntersection(beacon, iter);
            }
            return false;
        }
        return true;
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


    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        for (Beacon beacon : beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                // This is a Eddystone-UID frame
                //Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                MyBeacon myBeacon;
                myBeacon = new MyBeacon(beacon);

                if(!beacony.contains(myBeacon)){
                    myBeacon.addToLastTenRSSi(beacon.getRssi());
                    myBeacon.setPunktBeacona(punktyBeaconow.get(instanceId.toString()));
                    beacony.add(myBeacon);
                }
                else{
                    myBeacon = beacony.get(beacony.indexOf(myBeacon));
                    myBeacon.addToLastTenRSSi(beacon.getRssi());

                    myBeacon.setAvgDistance(beaconDistanceFromModel(myBeacon));
                }
            }
        }

        double[][] positions;
        double[] distances;
        final double[] centroid;

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
            float ratioX = canvas.getWidth()/5, ratioY = canvas.getHeight()/7;
            if(beacony.size()>0){
                for(MyBeacon mb : beacony){
                    switch (mb.getId2().toString()){
                        case "0x6d767674636e":

                            float r = (float)mb.getAvgDistance()*100;
                            canvas.drawCircle(0, 0, r, drawPaint);
                            break;
                        case "0x6f4334313146":
                            drawPaint.setColor(Color.GREEN);
                            r = (float)mb.getAvgDistance()*100;
                            canvas.drawCircle(0, 350, r, drawPaint);
                            break;
                        case "0x506b444b4c48":
                            drawPaint.setColor(Color.BLUE);
                            r = (float)mb.getAvgDistance()*100;
                            canvas.drawCircle(0, 700, r, drawPaint);
                            break;
                        case "0x72796a446a62":
                            drawPaint.setColor(Color.CYAN);
                            r = (float)mb.getAvgDistance()*100;
                            canvas.drawCircle(500, 700, r, drawPaint);
                            break;
                        case "0x30636169506c":
                            drawPaint.setColor(Color.MAGENTA);
                            r = (float)mb.getAvgDistance()*100;
                            canvas.drawCircle(500, 350, r, drawPaint);
                            break;
                        case "0x724335666650":
                            drawPaint.setColor(Color.RED);
                            r = (float)mb.getAvgDistance()*100;
                            canvas.drawCircle(500, 0, r, drawPaint);
                            break;
                    }

                }






            }

        }
    }
}
