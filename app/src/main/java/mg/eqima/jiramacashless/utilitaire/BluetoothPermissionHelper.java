package mg.eqima.jiramacashless.utilitaire;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BluetoothPermissionHelper {
    private static final int REQUEST_CODE_BLUETOOTH = 1001;
    private final Activity activity;

    public interface PermissionCallback {
        void onPermissionResult(boolean granted);
    }

    public BluetoothPermissionHelper(Activity activity) {
        this.activity = activity;
    }

    @SuppressLint("InlinedApi")
    private String[] getBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else {
            return new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }
    }

    public boolean needsBluetoothPermission() {
        for (String permission : getBluetoothPermissions()) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    public void requestBluetoothPermission() {
        ActivityCompat.requestPermissions(activity, getBluetoothPermissions(), REQUEST_CODE_BLUETOOTH);
    }

    public void handlePermissionsResult(int requestCode, int[] grantResults, PermissionCallback callback) {
        if (requestCode == REQUEST_CODE_BLUETOOTH) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            callback.onPermissionResult(allGranted);
        }
    }
}