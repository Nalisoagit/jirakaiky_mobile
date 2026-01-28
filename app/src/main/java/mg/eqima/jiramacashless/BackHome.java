package mg.eqima.jiramacashless;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.widget.Toast;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;

import mg.eqima.jiramacashless.database.DatabaseManager;

public class BackHome extends AppCompatActivity {

    private static final int ID_HISTORIQUE = 0;
    private static final int ID_SYNCHRONISATION = 1;

    AHBottomNavigation bottomNavigation;
    public static BackHome backHome;

    //NOMBRE DE SYNCHRONISATION:
    String synchroNb;

    // Nom item
    String nom;

    //DATABASE MANAGER:
    DatabaseManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_back_home);
        backHome = this;

        //INSTANCIATION DATABASE MANAGER:
        dbManager = new DatabaseManager(BackHome.this);
        synchroNb = String.valueOf(dbManager.getListeForSynchronization().size());
        dbManager.close();

        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Créer les items de navigation
        AHBottomNavigationItem historiqueItem = new AHBottomNavigationItem(
                "Historique",
                R.drawable.ic_baseline_history
        );

        AHBottomNavigationItem synchronisationItem = new AHBottomNavigationItem(
                "Synchronisation",
                R.drawable.ic_sync
        );

        // Ajouter les items à la navigation
        bottomNavigation.addItem(historiqueItem);
        bottomNavigation.addItem(synchronisationItem);

        // Configurer les couleurs (similaire à Meow Bottom Navigation)
        bottomNavigation.setDefaultBackgroundColor(getResources().getColor(R.color.backgroundBottomColor));
        bottomNavigation.setAccentColor(getResources().getColor(R.color.selected_icon_color));
        bottomNavigation.setInactiveColor(getResources().getColor(R.color.defaultIconColor));

        // Ajouter un badge pour la synchronisation
        if (Integer.parseInt(synchroNb) > 0) {
            bottomNavigation.setNotification(synchroNb, ID_SYNCHRONISATION);
        }

        // Gestion des événements de navigation
        bottomNavigation.setOnTabSelectedListener((position, wasSelected) -> {
            switch (position) {
                case ID_HISTORIQUE:
                    nom = "Historique";
                    replaceFragmentDynamically(new HistoriqueFragment(dbManager, BackHome.this));
                    break;

                case ID_SYNCHRONISATION:
                    nom = "Synchronisation";
                    replaceFragmentDynamically(new SynchronizationFragment(dbManager, BackHome.this, backHome));
                    break;
            }

            // Afficher un toast avec le nom de l'item
            Toast.makeText(BackHome.this, nom, Toast.LENGTH_SHORT).show();

            return true;
        });

        // Sélectionner l'onglet Historique par défaut
        bottomNavigation.setCurrentItem(ID_HISTORIQUE);
    }

    public void replaceFragmentDynamically(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentDynamicLayout, fragment);
        fragmentTransaction.commit();
    }
}