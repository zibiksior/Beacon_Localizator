package com.zbigniew.beacon_localizator;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
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

    private static final double EPSILON = 0.000001;
    private static final int READ_SDCARD_REQUEST = 123;
    private static final int FINE_LOCATION_REQUEST = 123;
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private BeaconManager mBeaconManager;

    private int tab[][] = new int[3][10];
    private double model[]=new double[50];
    private double avgRssi[]=new double[3], avgDistance[] = new double[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localization);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

    private boolean calculateThreeCircleIntersection(double x0, double y0, double r0,
                                                     double x1, double y1, double r1,
                                                     double x2, double y2, double r2) {
        double a, dx, dy, d, h, rx, ry;
        double point2_x, point2_y;

    /* dx and dy are the vertical and horizontal distances between
    * the circle centers.
    */
        dx = x1 - x0;
        dy = y1 - y0;

    /* Determine the straight-line distance between the centers. */
        d = sqrt((dy * dy) + (dx * dx));

    /* Check for solvability. */
        if (d > (r0 + r1)) {
        /* no solution. circles do not intersect. */
            return false;
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
        point2_x = x0 + (dx * a / d);
        point2_y = y0 + (dy * a / d);

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
        double intersectionPoint1_x = point2_x + rx;
        double intersectionPoint2_x = point2_x - rx;
        double intersectionPoint1_y = point2_y + ry;
        double intersectionPoint2_y = point2_y - ry;

        Log.d("TAG", "INTERSECTION Circle1 AND Circle2: (" + intersectionPoint1_x + "," + intersectionPoint1_y + ")" + " AND (" + intersectionPoint2_x + "," + intersectionPoint2_y + ")");

    /* Lets determine if circle 3 intersects at either of the above intersection points. */
        dx = intersectionPoint1_x - x2;
        dy = intersectionPoint1_y - y2;
        double d1 = sqrt((dy * dy) + (dx * dx));

        dx = intersectionPoint2_x - x2;
        dy = intersectionPoint2_y - y2;
        double d2 = sqrt((dy * dy) + (dx * dx));

        if (abs(d1 - r2) < EPSILON) {
            Log.d("TAG", "INTERSECTION Circle1 AND Circle2 AND Circle3: " + "(" + intersectionPoint1_x + "," + intersectionPoint1_y + ")");
        } else if (abs(d2 - r2) < EPSILON) {
            Log.d("TAG", "INTERSECTION Circle1 AND Circle2 AND Circle3: (" + intersectionPoint2_x + "," + intersectionPoint2_y + ")"); //here was an error
        } else {
            Log.d("TAG", "INTERSECTION Circle1 AND Circle2 AND Circle3: NONE");
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

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        for (Beacon beacon : beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                // This is a Eddystone-UID frame
                Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                double sum=0;
                switch (instanceId.toString()) {
                    case "0x506b444b4c48":
                        for (int i = 1; i < 10; i++) {
                            tab[0][i - 1] = tab[0][i];
                        }
                        tab[0][9] = beacon.getRssi();

                        sum=0;
                        for (int i = 0; i < 10; i++) {
                            sum+=tab[0][i];
                        }
                        avgRssi[0]=sum/10;
                        //dane[0][counter2] = beacon.getRssi();
                        break;
                    case "0x30636169506c":
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
                    case "0x6d767674636e":
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
                }
                Log.d("RangingActivity", "I see a beacon transmitting namespace id: " + namespaceId +
                        " and instance id: " + instanceId +
                        " approximately " + beacon.getDistance() + " meters away.");
                /*runOnUiThread(new Runnable() {
                    public void run() {
                        ((TextView) LocalizationActivity.this.findViewById(R.id.message)).setText("Hello world, and welcome to Eddystone!");
                    }
                });*/
            }
        }

        for(int j=0;j<3;j++){
            double myNumber = avgRssi[j];
            double distance = Math.abs(model[0] - myNumber);
            int idx = 0;
            for(int c = 1; c < model.length; c++){
                double cdistance = Math.abs(model[c] - myNumber);
                if(cdistance < distance){
                    idx = c;
                    distance = cdistance;
                }
            }
            avgDistance[j] = idx;
        }

        runOnUiThread(new Runnable() {
            public void run() {
                ((TextView) LocalizationActivity.this.findViewById(R.id.message)).setText("avgDistance 0:"+avgDistance[0]+
                        "\nAvg Distance 1: "+avgDistance[1]+
                        "\nAvg Distance 2: "+avgDistance[2]);
            }
        });
        calculateThreeCircleIntersection(0,0,avgDistance[0],3,0,avgDistance[1],0,3,avgDistance[2]);
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
