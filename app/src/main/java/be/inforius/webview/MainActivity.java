package be.inforius.webview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.WebView;
import android.widget.EditText;

import java.util.Date;

public class MainActivity extends Activity {

    private long pressureMoment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Seule fonction fonctionnelle sur SDK17 Android 4.2.2 pour cacher la barre de navigation en bas de façon permanente sans utiliser timer ou events
        // Pas nécéssaire pour la tablette vu qu'elle à une option prévue pour cacher la barre de navigation, et que la tablette n'a pas de barre de statut au-dessus
        getWindow().getDecorView().setSystemUiVisibility(8); // On peut quand-même laisser

        setContentView(R.layout.activity_main);

        WebView webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://suivi.inforius.be/inforius/");

        // récup taille écran
        final Point size = new Point();
        (getWindowManager().getDefaultDisplay()).getSize(size);

        webView.setOnTouchListener(new OnTouchListener() {
            // Préparation zone touchable (15% de largeur et hauteur, dans le coin en bas à droite)
            private int minX = size.x - (int)(size.x * .15),
                        minY = size.y - (int)(size.y * .15);

            private Rect touchZone  = new Rect(minX, minY, size.x, size.y);

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int touchX = (int)event.getX(),
                        touchY = (int) event.getY();

                // si le click est éffectué dans la zone touchable
//                if(touchX >= minX && touchY >= minY) // si jamais il y a problème dans la tablette aussi, vautdrait peut-etre mieux utiliser ça
                if(touchZone.contains(touchX, touchY)) {
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_DOWN: pressureMoment = new Date().getTime(); break;
                        case MotionEvent.ACTION_UP:
                            if(new Date().getTime() > pressureMoment + 1000) // si une seconde après
                                askForPassword();
                            break;
                    }
                }

                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if(!getPackageName().equals(findLauncherPackageName()))
            resetPreferredLauncherAndOpenChooser(this);
    }

    private void askForPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sortir ?");
        builder.setMessage("Tappez le mot de passe");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Sortir", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(input.getText().toString().equals("123456"))
                    letChooseDefaultLauncher();
            }
        });

        builder.show();
    }

    private void letChooseDefaultLauncher() {
        getPackageManager().clearPackagePreferredActivities(getPackageName());

        Intent i = new Intent();
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        MainActivity.this.startActivity(i);
    }

    private static void resetPreferredLauncherAndOpenChooser(Context context) {
        PackageManager packageManager = context.getPackageManager();

        // à modifier si on change le nom du package et/ou de la classe du fake activity
        ComponentName componentName = new ComponentName(context, FakeActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        Intent selector = new Intent(Intent.ACTION_MAIN);
        selector.addCategory(Intent.CATEGORY_HOME);
        selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(selector);

        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
    }

    private String findLauncherPackageName(){
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        final ResolveInfo res = getPackageManager().resolveActivity(intent, 0);

        return res.activityInfo.packageName;
    }
}
