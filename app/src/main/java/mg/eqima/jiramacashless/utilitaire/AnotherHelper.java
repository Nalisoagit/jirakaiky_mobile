package mg.eqima.jiramacashless.utilitaire;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AnotherHelper {
    private static final int REQUEST_ENABLE_BLUETOOTH = 1002;

    public static BluetoothDevice findBluetoothprinter(BluetoothPermissionHelper permissionHelper, Context context, Activity activity) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Vérifier si l'adaptateur Bluetooth existe
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Cet appareil ne supporte pas le Bluetooth", Toast.LENGTH_SHORT).show();
            return null;
        }

        // Vérifier si le Bluetooth est activé, sinon demander à l'utilisateur de l'activer
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            Toast.makeText(context, "Veuillez activer le Bluetooth", Toast.LENGTH_SHORT).show();
            return null; // ✅ stop execution and wait for user to enable
        }

        // Vérifier les permissions
        if (permissionHelper.needsBluetoothPermission()) {
            permissionHelper.requestBluetoothPermission();
            return null; // ✅ wait for permission result before continuing
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context,android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1001);
                return null;
            }
        }

        // Récupérer les périphériques appairés
        @SuppressLint("MissingPermission")
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.isEmpty()) {
            Toast.makeText(context, "Aucun périphérique Bluetooth appairé", Toast.LENGTH_SHORT).show();
            return null;
        }


        Set<String> printerNames = new HashSet<>(Arrays.asList("InnerPrinter", "vBtPrinter"));

        for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            if (deviceName != null && printerNames.contains(deviceName)) {
                Log.e("TAG", "Imprimante trouvée : " + deviceName);
                return device;
            }
        }

        Toast.makeText(context, "Imprimante  non trouvée", Toast.LENGTH_SHORT).show();
        Log.d("TAG", "Aucune imprimante correspondante trouvée");
        return null;
    }


}