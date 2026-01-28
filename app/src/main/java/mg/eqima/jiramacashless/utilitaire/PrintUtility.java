package mg.eqima.jiramacashless.utilitaire;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import mg.eqima.jiramacashless.R;
import mg.eqima.jiramacashless.recu.Recu;

public class PrintUtility {
    private static final String TAG = "PrintUtility";
    private static final int MAX_PRINT_RETRY = 2;

    @SuppressLint("MissingPermission")
    public static void doPrint(View view, String recu, String combinaison, String duplicata, String id, Activity activity, Context context) {
        // Vérifier les permissions Bluetooth avant d'imprimer
        if (!checkBluetoothPermissions(activity, context)) {
            return;
        }

        Utilitaire utilitaire = new Utilitaire();
        Bitmap qrCode = utilitaire.generateBitmap(combinaison);

        // Exécution dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                // BLUETOOTH:
                BluetoothDevice printerDevice = AnotherHelper.findBluetoothprinter(new BluetoothPermissionHelper(activity), context, activity);
                if (printerDevice == null) {
                    showToast(context, "Aucune imprimante trouvée");
                    return;
                }

                // Attendre un peu que la connexion soit stable
                Thread.sleep(500);
//
//                // Vérifier que le socket est bien connecté
//                BluetoothSocket socket = AnotherHelper.getBluetoothSocket();
//                if (socket == null || !socket.isConnected()) {
//                    showToast(context, "Erreur: imprimante non connectée");
//                    return;
//                }

                // Créer la connexion pour ESC/POS
                BluetoothConnection connection = new BluetoothConnection(printerDevice);

                if (connection != null) {
                    // Définir l'encodage UTF-8
                    EscPosCharsetEncoding charsetEncoding = new EscPosCharsetEncoding("UTF-8", 6);

                    // Tentative d'impression avec plusieurs essais si nécessaire
                    tryPrintingWithRetry(connection, recu, qrCode, duplicata, activity, context, 0);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur générale d'impression: " + e.getMessage(), e);
                showToast(context, "Erreur d'impression: " + e.getMessage());
            }
        }).start();
    }

    private static boolean checkBluetoothPermissions(Activity activity, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1001);
                showToast(context, "Permission Bluetooth requise");
                return false;
            }
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, 1001);
            showToast(context, "Permission Bluetooth requise");
            return false;
        }
        return true;
    }

    private static void tryPrintingWithRetry(BluetoothConnection connection, String recu, Bitmap qrCode,
                                             String duplicata, Activity activity, Context context, int retryCount) {
        try {
            if (retryCount > 0) {
                Log.d(TAG, "Tentative d'impression #" + (retryCount + 1));
                // Petit délai entre les tentatives
                Thread.sleep(1000);
            }

            // Création de l'imprimante
            EscPosCharsetEncoding charsetEncoding = new EscPosCharsetEncoding("UTF-8", 6);
            EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32, charsetEncoding);

            // S'assurer que le texte est correctement encodé en UTF-8
            String textToPrint = new String(recu.getBytes("UTF-8"), "UTF-8");

            // Construction du contenu à imprimer
            StringBuilder logoBuilder = new StringBuilder();
            logoBuilder.append("[C]<img>")
                    .append(PrinterTextParserImg.bitmapToHexadecimalString(printer,
                            activity.getApplicationContext().getResources().getDrawableForDensity(
                                    R.drawable.logojirama2, DisplayMetrics.DENSITY_MEDIUM)))
                    .append("</img>\n")
                    .append("[L]\n")
                    .append(textToPrint)
                    .append("\n");

            if (duplicata != null && duplicata.equals("1")) {
                logoBuilder.append("[C]<img>")
                        .append(PrinterTextParserImg.bitmapToHexadecimalString(printer,
                                activity.getApplicationContext().getResources().getDrawableForDensity(
                                        R.drawable.duplicata, DisplayMetrics.DENSITY_MEDIUM)))
                        .append("</img>\n");
            }

            logoBuilder.append("[C]<img>")
                    .append(PrinterTextParserImg.bitmapToHexadecimalString(printer, qrCode))
                    .append("</img>\n")
                    .append("[L]");

            // Impression du texte formaté
            printer.printFormattedText(logoBuilder.toString());

            // Déconnexion propre
            connection.disconnect();

            showToast(context, "Impression réussie");

        } catch (EscPosConnectionException e) {
            Log.e(TAG, "Erreur de connexion: " + e.getMessage(), e);

            if (retryCount < MAX_PRINT_RETRY) {
                // Nouvelle tentative
                tryPrintingWithRetry(connection, recu, qrCode, duplicata, activity, context, retryCount + 1);
            } else {
                showToast(context, "Échec de connexion à l'imprimante après plusieurs essais");

                // Fermer proprement la connexion en cas d'échec
                try {
                    connection.disconnect();
                } catch (Exception ex) {
                    Log.e(TAG, "Erreur lors de la déconnexion: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur d'impression: " + e.getMessage(), e);
            showToast(context, "Erreur: " + e.getMessage());

            // Fermer proprement la connexion
            try {
                connection.disconnect();
            } catch (Exception ex) {
                Log.e(TAG, "Erreur lors de la déconnexion: " + ex.getMessage());
            }
        }
    }

    @SuppressLint("MissingPermission")
    public static void doPrintPayement(View view, Recu recuS, Activity activity, Context context) {
        // Vérifier les permissions Bluetooth avant d'imprimer
        if (!checkBluetoothPermissions(activity, context)) {
            return;
        }

        String recu = recuS.contruireMonRecu();
        String combinaison = recuS.getRefFacture() + "_" + recuS.getNumeroRecuJirama();
        Utilitaire utilitaire = new Utilitaire();
        Bitmap qrCode = utilitaire.generateBitmap(combinaison);

        // Exécution dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                // BLUETOOTH:
                BluetoothDevice printerDevice = AnotherHelper.findBluetoothprinter(new BluetoothPermissionHelper(activity), context, activity);
                if (printerDevice == null) {
                    showToast(context, "Aucune imprimante trouvée");
                    return;
                }

                // Attendre un peu que la connexion soit stable
                Thread.sleep(500);

//                // Vérifier que le socket est bien connecté
//                BluetoothSocket socket = AnotherHelper.getBluetoothSocket();
//                if (socket == null || !socket.isConnected()) {
//                    showToast(context, "Erreur: imprimante non connectée");
//                    return;
//                }

                // Créer la connexion pour ESC/POS
                BluetoothConnection connection = new BluetoothConnection(printerDevice);

                if (connection != null) {
                    // Tentative d'impression avec plusieurs essais si nécessaire
                    tryPrintingPayementWithRetry(connection, recu, qrCode, activity, context, 0);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur générale d'impression: " + e.getMessage(), e);
                showToast(context, "Erreur d'impression: " + e.getMessage());
            }
        }).start();
    }

    private static void tryPrintingPayementWithRetry(BluetoothConnection connection, String recu, Bitmap qrCode,
                                                     Activity activity, Context context, int retryCount) {
        try {
            if (retryCount > 0) {
                Log.d(TAG, "Tentative d'impression du paiement #" + (retryCount + 1));
                // Petit délai entre les tentatives
                Thread.sleep(1000);
            }

            // Création de l'imprimante
            EscPosCharsetEncoding charsetEncoding = new EscPosCharsetEncoding("UTF-8", 6);
            EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32, charsetEncoding);

            // S'assurer que le texte est correctement encodé en UTF-8
            String textToPrint = new String(recu.getBytes("UTF-8"), "UTF-8");

            // Construction du contenu à imprimer
            StringBuilder logoBuilder = new StringBuilder();
            logoBuilder.append("[C]<img>")
                    .append(PrinterTextParserImg.bitmapToHexadecimalString(printer,
                            activity.getApplicationContext().getResources().getDrawableForDensity(
                                    R.drawable.logojirama2, DisplayMetrics.DENSITY_MEDIUM)))
                    .append("</img>\n")
                    .append("[L]\n")
                    .append(textToPrint)
                    .append("\n")
                    .append("[C]<img>")
                    .append(PrinterTextParserImg.bitmapToHexadecimalString(printer, qrCode))
                    .append("</img>\n")
                    .append("[L]\n");

            // Impression du texte formaté
            printer.printFormattedText(logoBuilder.toString());

            // Déconnexion propre
            connection.disconnect();

            showToast(context, "Impression réussie");

        } catch (EscPosConnectionException e) {
            Log.e(TAG, "Erreur de connexion: " + e.getMessage(), e);

            if (retryCount < MAX_PRINT_RETRY) {
                // Nouvelle tentative
                tryPrintingPayementWithRetry(connection, recu, qrCode, activity, context, retryCount + 1);
            } else {
                showToast(context, "Échec de connexion à l'imprimante après plusieurs essais");

                // Fermer proprement la connexion en cas d'échec
                try {
                    connection.disconnect();
                } catch (Exception ex) {
                    Log.e(TAG, "Erreur lors de la déconnexion: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur d'impression: " + e.getMessage(), e);
            showToast(context, "Erreur: " + e.getMessage());

            // Fermer proprement la connexion
            try {
                connection.disconnect();
            } catch (Exception ex) {
                Log.e(TAG, "Erreur lors de la déconnexion: " + ex.getMessage());
            }
        }
    }

    // Méthode utilitaire pour afficher des toasts depuis un thread secondaire
    private static void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }
}