package co.edu.unicolombo.usuariosrender.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import co.edu.unicolombo.usuariosrender.R;
import co.edu.unicolombo.usuariosrender.data.InputValidator;
import co.edu.unicolombo.usuariosrender.data.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class UserDetailActivity extends BaseActivity {
    private String id;
    private User user;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!requiereSesion()) return;
        id = getIntent().getStringExtra("id");
        if (!InputValidator.identificadorValido(id) || !session.puedeConsultar()) {
            finish();
            return;
        }
        mostrarPantalla(R.layout.activity_user_detail, getString(R.string.detail), true);
        findViewById(R.id.retry).setOnClickListener(view -> cargarDetalle());
        findViewById(R.id.edit)
                .setOnClickListener(
                        view -> {
                            if (requiereSesion() && session.esAdministrador())
                                startActivity(new Intent(this, UserFormActivity.class).putExtra("id", id));
                        });
        findViewById(R.id.delete).setOnClickListener(view -> confirmarEliminacion());
        requests.ocupado()
                .observe(
                        this,
                        busy -> {
                            findViewById(R.id.progress).setVisibility(busy ? View.VISIBLE : View.GONE);
                            findViewById(R.id.edit).setEnabled(!busy);
                            findViewById(R.id.delete).setEnabled(!busy);
                            findViewById(R.id.retry).setEnabled(!busy);
                        });
        requests.resultado()
                .observe(
                        this,
                        result -> {
                            if (result == null || manejarAutenticacion(result)) return;
                            if (!result.fueExitoso()) {
                                mostrarMensaje(result.error);
                                findViewById(R.id.retry)
                                        .setVisibility(
                                                "detail".equals(result.action) ? View.VISIBLE : View.GONE);
                                return;
                            }
                            if ("delete".equals(result.action)) {
                                if (result.handled) return;
                                result.handled = true;
                                Toast.makeText(this, "Usuario eliminado del servidor.", Toast.LENGTH_LONG)
                                        .show();
                                if (id.equals(session.identificadorUsuario())) cerrarSesion(false);
                                else finish();
                                return;
                            }
                            try {
                                user = new User(result.comoObjeto());
                                ponerTexto(R.id.name, user.name);
                                ponerTexto(R.id.email, user.email);
                                ponerTexto(R.id.identifier, user.id);
                                ponerTexto(R.id.role, user.role);
                                ponerTexto(R.id.status, user.status);
                                ponerTexto(R.id.initials, user.iniciales());
                                ocultarMensaje();
                                findViewById(R.id.retry).setVisibility(View.GONE);
                                findViewById(R.id.edit)
                                        .setVisibility(session.esAdministrador() ? View.VISIBLE : View.GONE);
                                findViewById(R.id.delete)
                                        .setVisibility(session.esAdministrador() ? View.VISIBLE : View.GONE);
                            } catch (Exception exception) {
                                mostrarMensaje("El servidor devolvió datos no válidos.");
                            }
                        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (id != null && requiereSesion() && !requests.estaOcupado()) cargarDetalle();
    }

    private void cargarDetalle() {
        if (requiereSesion())
            requests.ejecutar("detail", "GET", "/api/users/" + id, null, session.obtenerToken());
    }

    private void confirmarEliminacion() {
        if (user == null || requests.estaOcupado() || !requiereSesion() || !session.esAdministrador()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("¿Eliminar a " + user.name + "?")
                .setMessage(
                        "Se eliminará este registro de la base de datos remota. Esta acción no se puede deshacer."
                                + (id.equals(session.identificadorUsuario())
                                        ? " Es tu propia cuenta y se cerrará la sesión."
                                        : ""))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(
                        R.string.delete_user,
                        (dialog, which) -> {
                            if (requiereSesion())
                                requests.ejecutar(
                                        "delete", "DELETE", "/api/users/" + id, null, session.obtenerToken());
                        })
                .show();
    }
}
