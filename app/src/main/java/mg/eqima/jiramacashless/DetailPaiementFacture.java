package mg.eqima.jiramacashless;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import mg.eqima.jiramacashless.database.DatabaseManager;
import mg.eqima.jiramacashless.utilitaire.PrintUtility;
import mg.eqima.jiramacashless.utilitaire.Utilitaire;

public class DetailPaiementFacture extends AppCompatActivity {

    TextView contenuDetail;
    Button annulationButton;
    Button imprimerButton;
    DatabaseManager myDatabaseManager;

    /*** BLUETOOTH ILAINA com.github.DantSu:ESCPOS-ThermalPrinter ****/
    public static final int PERMISSION_BLUETOOTH = 1;
    private static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    private static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    private static final int PERMISSION_BLUETOOTH_SCAN = 4;

    private static final int PERMISSION_BLUETOOTH_ADVERTISE = 5;
    private final Locale locale = new Locale("id", "ID");
    private final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a", locale);
    private final NumberFormat nf = NumberFormat.getCurrencyInstance(locale);

    BluetoothConnection btConnection;

    String recuString;
    String depart;
    String combinaison;
    String duplicata;
    String id;
    Context context ;

    /*** BLUETOOTH ILAINA com.github.DantSu:ESCPOS-ThermalPrinter FIN ****/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_paiement_facture);

        contenuDetail = findViewById(R.id.contenuDetail);
        annulationButton = findViewById(R.id.annulationButton);
        imprimerButton = findViewById(R.id.imprimerBoutton);
        myDatabaseManager = new DatabaseManager(DetailPaiementFacture.this);
        context = this ;

        // Get de l'intent:
        Intent intent = getIntent();
        if(intent!=null)
        {
            Bundle bundle = intent.getExtras();
            recuString = bundle.getString("recu_detail");
            depart = bundle.getString("depart");
            combinaison = bundle.getString("combinaison");
            duplicata = bundle.getString("duplicata");
            id = bundle.getString("idInterneReel");
            contenuDetail.setText(recuString);
        }

        Log.e("RECU", recuString);

        annulationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(depart.equals("1"))
                {
                    Intent intent1 = new Intent(DetailPaiementFacture.this, MainActivity.class);
                    intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent1);
                }
                else if(depart.equals("2"))
                {
                    Intent intent1 = new Intent(DetailPaiementFacture.this, BackHome.class);
                    intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent1);
                }

            }
        });

        imprimerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Print","tafiditra print") ;
                //PrinterManager.checkAndPrint(context);
                PrintUtility.doPrint(view,recuString,combinaison,duplicata,id,DetailPaiementFacture.this,context);
                //doPrint(view, recuString, combinaison, duplicata, id);
            }
        });

    }

    /******** BLUETOOTH VERSION FARANY MODIFICATION: Dantsu:ESCPOS-ThermalPrinter ******************/
    public void doPrint(View view, String recu, String combinaison, String duplicata, String id) {

        Utilitaire utilitaire = new Utilitaire();
        Bitmap qrCode = utilitaire.generateBitmap(combinaison);

        // Check and request Bluetooth permissions for Android 12 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request BLUETOOTH_CONNECT permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        PERMISSION_BLUETOOTH_CONNECT);
                return; // Exit method to wait for permission result
            }
        }

        // Proceed with Bluetooth connection and printing
        BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();

        try {
            if (connection != null) {

                EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);

                if(duplicata.equals("1")) {
                    String logo = "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, this.getApplicationContext().getResources().getDrawableForDensity(R.drawable.logojirama2, DisplayMetrics.DENSITY_MEDIUM)) + "</img>\n"
                            +"[L]\n" + recu +"\n"+
                            "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, this.getApplicationContext().getResources().getDrawableForDensity(R.drawable.duplicata, DisplayMetrics.DENSITY_MEDIUM)) + "</img>\n"+
                            "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, qrCode) + "</img>\n"
                            +"[L]";

                    printer.printFormattedText(logo);
                    connection.disconnect();
                }
                else if(duplicata.equals("0")) {
                    String logo = "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, this.getApplicationContext().getResources().getDrawableForDensity(R.drawable.logojirama2, DisplayMetrics.DENSITY_MEDIUM)) + "</img>\n"
                            +"[L]\n" + recu +"\n"+
                            "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, qrCode) + "</img>\n"
                            +"[L]";
                    printer.printFormattedText(logo);
                    myDatabaseManager.miseEnDuplicata(id);
                    myDatabaseManager.close();
                    connection.disconnect();
                }
            } else {
                Log.d("PRINT", "Imprimante trouvée: " + connection.getDevice().getName());

                Toast.makeText(this, DetailPaiementFacture.this.getString(R.string.no_connected_imprimante), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("APP", "Can't print", e);
            Toast.makeText(this, "Printing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry printing
                doPrint(null, recuString, combinaison, duplicata, id);
            } else {
                Toast.makeText(this, "Bluetooth connect permission is required to print", Toast.LENGTH_SHORT).show();
            }
        }
    }


}