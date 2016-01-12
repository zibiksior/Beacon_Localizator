package com.zbigniew.beacon_localizator;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.LocationListener;
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
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.lang.Math.*;

public class LocalizationActivity extends AppCompatActivity implements BeaconConsumer, RangeNotifier {

    private static final double EPSILON = 0.2;
    private static final int READ_SDCARD_REQUEST = 123;
    private static final int FINE_LOCATION_REQUEST = 123;
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private BeaconManager mBeaconManager;

    private int tab[][] = new int[6][10];
    private Point[] punktyBeaconow = new Point[6];
    private double model[]=new double[50];
    private double avgRssi[]=new double[6], avgDistance[] = new double[6];

    private double intersectionPoint1_x, intersectionPoint2_x, intersectionPoint1_y, intersectionPoint2_y;

    private TextView message;
    private static final int liczbaKombinacji = silnia(4)/(silnia(3) * silnia(4 - 3));;
    private static int[][] result = new int[liczbaKombinacji][3];
    private static final int[] arr = {0,1,2,3};

    private double[][] punkty = new double[liczbaKombinacji][2];
    private static int counter =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localization);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        message = (TextView) LocalizationActivity.this.findViewById(R.id.message);

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

        punktyBeaconow[0]=new Point(0,0);
        punktyBeaconow[1]=new Point(4,0);
        punktyBeaconow[2]=new Point(4,4);
        punktyBeaconow[3]=new Point(0,4);



        combinations(arr, 3, 0, new int[3]);
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

    private boolean calculateTwoCirclesIntersectionPoints(Point p0, final double r0,
                                                       Point p1, final double r1,
                                                          Point p2, double r2, int iter){
        double a, dx, dy, d, h, rx, ry;
        double point2_x, point2_y;

    /* dx and dy are the vertical and horizontal distances between
    * the circle centers.
    */
        dx = p1.x - p0.x;
        dy = p1.y - p0.y;

    /* Determine the straight-line distance between the centers. */
        d = sqrt((dy * dy) + (dx * dx));

    /* Check for solvability. */
        if (d > (r0 + r1)) {
        /* no solution. circles do not intersect. */
            if(calculateTwoCirclesIntersectionPoints(p0, (r0+0.2),p1,(r1+0.2),p2,r2, iter)){
                calculateThreeCircleIntersection(p2,r2, iter);
            }
            return  false;
        }
        if (d < abs(r0 - r1)) {
        /* no solution. one circle is contained in the other */
            return false;
        }

    /* 'point 2' is the point where the line through the circle
    * intersection points crosses the line between the circle
    * centers.
    */

    /* Determine the distance from point 0 to point 2. */
        a = ((r0 * r0) - (r1 * r1) + (d * d)) / (2.0 * d);

    /* Determine the coordinates of point 2. */
        point2_x = p0.x + (dx * a / d);
        point2_y = p0.y + (dy * a / d);

    /* Determine the distance from point 2 to either of the
    * intersection points.
    */
        h = sqrt((r0 * r0) - (a * a));

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


    private boolean calculateThreeCircleIntersection(Point p, double r, int iter) {
        double dx, dy;

    /* Lets determine if circle 3 intersects at either of the above intersection points. */
        dx = intersectionPoint1_x - p.x;
        dy = intersectionPoint1_y - p.y;
        double d1 = sqrt((dy * dy) + (dx * dx));

        dx = intersectionPoint2_x - p.x;
        dy = intersectionPoint2_y - p.y;
        double d2 = sqrt((dy * dy) + (dx * dx));

        if (abs(d1 - r) < EPSILON) {
            punkty[iter][0]=intersectionPoint1_x;
            punkty[iter][1]=intersectionPoint1_y;
            /*Log.d("TAG", "INTERSECTION Circle1 AND Circle2 AND Circle3: " + "(" + intersectionPoint1_x + "," + intersectionPoint1_y + ")");
            runOnUiThread(new Runnable() {
                public void run() {
                    message.setText("INTERSECTION Circle1 AND Circle2 AND Circle3: " + "(\n" + intersectionPoint1_x + ",\n" + intersectionPoint1_y + ")");
                }
            });*/
        } else if (abs(d2 - r) < EPSILON) {
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
            calculateThreeCircleIntersection(p, (r + 0.1), iter);
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
                Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                double sum=0;
                switch (instanceId.toString()) {
                    case "0x6d767674636e":
                        for (int i = 1; i < 10; i++) {
                            tab[0][i - 1] = tab[0][i];
                        }
                        tab[0][9] = beacon.getRssi();
                        sum=0;
                        for (int i = 0; i < 10; i++) {
                            sum+=tab[0][i];
                        }
                        avgRssi[0]=sum/10;
                        break;
                    case "0x6f4334313146":
                        for (int i = 1; i < 10; i++) {
                            tab[1][i - 1] = tab[1][i];
                        }
                        tab[1][9] = beacon.getRssi();
                        sum=0;
                        for (int i = 0; i < 10; i++) {
                            sum+=tab[1][i];
                        }
                        avgRssi[1]=sum/10;
                        break;
                    case "0x724335666650":
                        for (int i = 1; i < 10; i++) {
                            tab[2][i - 1] = tab[2][i];
                        }
                        tab[2][9] = beacon.getRssi();
                        sum=0;
                        for (int i = 0; i < 10; i++) {
                            sum+=tab[2][i];
                        }
                        avgRssi[2]=sum/10;
                        break;
                    case "0x506b444b4c48":
                        for (int i = 1; i < 10; i++) {
                            tab[3][i - 1] = tab[3][i];
                        }
                        tab[3][9] = beacon.getRssi();

                        sum=0;
                        for (int i = 0; i < 10; i++) {
                            sum+=tab[3][i];
                        }
                        avgRssi[3]=sum/10;
                        //dane[0][counter2] = beacon.getRssi();
                        break;
                    case "0x30636169506c":
                        for (int i = 1; i < 10; i++) {
                            tab[4][i - 1] = tab[4][i];
                        }
                        tab[4][9] = beacon.getRssi();
                        sum=0;
                        for (int i = 0; i < 10; i++) {
                            sum+=tab[4][i];
                        }
                        avgRssi[4]=sum/10;
                        break;
                    case "0x72796a446a62":
                        for (int i = 1; i < 10; i++) {
                            tab[5][i - 1] = tab[5][i];
                        }
                        tab[5][9] = beacon.getRssi();
                        sum=0;
                        for (int i = 0; i < 10; i++) {
                            sum+=tab[5][i];
                        }
                        avgRssi[5]=sum/10;
                        break;
                }
                /*Log.d("RangingActivity", "I see a beacon transmitting namespace id: " + namespaceId +
                        " and instance id: " + instanceId +
                        " approximately " + beacon.getDistance() + " meters away.");
                */
                /*runOnUiThread(new Runnable() {
                    public void run() {
                        ((TextView) LocalizationActivity.this.findViewById(R.id.message)).setText("Hello world, and welcome to Eddystone!");
                    }
                });*/
            }
        }

        for(int j=0;j<6;j++){
            double myNumber = avgRssi[j];
            double distance = Math.abs(model[0] - myNumber);
            double idx = 0;
            for(int c = 1; c < model.length; c++){
                double cdistance = Math.abs(model[c] - myNumber);
                if(cdistance < distance){
                    idx = c;
                    distance = cdistance;
                }
            }
            avgDistance[j] = (idx+1)/10;
        }

        /*runOnUiThread(new Runnable() {
            public void run() {
                ((TextView) LocalizationActivity.this.findViewById(R.id.message)).setText("avgDistance 1 (Hf6n): " + avgDistance[0] +
                        "\nAvg Distance 2 (aYJn): " + avgDistance[1] +
                        "\nAvg Distance 3 (zUUe): " + avgDistance[2] +
                        "\nAvg Distance 4 (luH8): " + avgDistance[3] +
                        "\nAvg Distance 5 (wxSM): " + avgDistance[4] +
                        "\nAvg Distance 5 (f5P9): " + avgDistance[5]);
            }
        });*/
        for (int i=0;i<liczbaKombinacji;i++) {
            if (calculateTwoCirclesIntersectionPoints(punktyBeaconow[result[i][0]], avgDistance[result[i][0]],
                    punktyBeaconow[result[i][1]] , avgDistance[result[i][1]], punktyBeaconow[result[i][2]],avgDistance[result[i][2]],i)) {
                calculateThreeCircleIntersection(punktyBeaconow[result[i][2]], avgDistance[result[i][2]],i);
            }
        }

        runOnUiThread(new Runnable() {
            public void run() {
                message.setText("");
                for(int i=0;i<4;i++) {
                    message.append("Punkt "+i+": \n"+punkty[i][0]+", \n"+punkty[i][1]+"\n");
                }
            }
        });
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
}
