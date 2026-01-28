package mg.eqima.jiramacashless.utilitaire;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

public class AppUtils {

    public static String getAppVersion(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Pour API 33 et plus
                pi = pm.getPackageInfo(context.getPackageName(), PackageManager.PackageInfoFlags.of(0));
            } else {
                // Pour les versions plus anciennes
                pi = pm.getPackageInfo(context.getPackageName(), 0);
            }

            long versionCode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = pi.getLongVersionCode(); // API 28+
            } else {
                versionCode = pi.versionCode; // Déprécié après API 28
            }

            //return "Version " + pi.versionName + " (Code " + versionCode + ")";
            return  pi.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "Version inconnue";
        }
    }
}
