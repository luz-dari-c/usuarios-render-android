package co.edu.unicolombo.usuariosrender.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import co.edu.unicolombo.usuariosrender.R;
import co.edu.unicolombo.usuariosrender.data.ApiClient;
import co.edu.unicolombo.usuariosrender.data.SessionStore;
import com.google.android.material.appbar.MaterialToolbar;

public abstract class BaseActivity extends AppCompatActivity {
    protected SessionStore session;
    protected RequestViewModel requests;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        session = new SessionStore(this);
        requests = new ViewModelProvider(this).get(RequestViewModel.class);
    }

    protected void mostrarPantalla(int layout, String title, boolean back) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(layout);
        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, windowInsets) -> {
                    Insets bars =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.displayCutout()
                                            | WindowInsetsCompat.Type.ime());
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return windowInsets;
                });
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(title);
            if (back)
                toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    protected void mostrarMensaje(String message) {
        TextView label = findViewById(R.id.message);
        label.setText(message);
        label.setVisibility(View.VISIBLE);
    }

    protected void ocultarMensaje() {
        findViewById(R.id.message).setVisibility(View.GONE);
    }

    protected void ponerTexto(int id, String value) {
        ((TextView) findViewById(id)).setText(value);
    }

    protected boolean requiereSesion() {
        if (session.obtenerToken().isEmpty()) {
            cerrarSesion(true);
            return false;
        }
        return true;
    }

    protected boolean manejarAutenticacion(ApiClient.Result result) {
        if (result.code == 401) {
            cerrarSesion(true);
            return true;
        }
        return false;
    }

    protected void cerrarSesion(boolean expired) {
        session.limpiar();
        if (expired) Toast.makeText(this, R.string.session_expired, Toast.LENGTH_LONG).show();
        startActivity(
                new Intent(this, LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    protected void irA(Class<?> activity) {
        startActivity(new Intent(this, activity));
    }
}
