package co.edu.unicolombo.usuariosrender.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.app.AlertDialog;
import co.edu.unicolombo.usuariosrender.R;
import co.edu.unicolombo.usuariosrender.data.InputValidator;
import co.edu.unicolombo.usuariosrender.data.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class UsersActivity extends BaseActivity {
    private UserAdapter adapter;
    private EditText search;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!requiereSesion()) return;
        mostrarPantalla(R.layout.activity_users, getString(R.string.app_name), false);
        ponerTexto(R.id.sessionInfo, session.correo() + " · " + session.rol());
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (session.puedeConsultar()) {
            toolbar.getMenu().add(Menu.NONE, 1, 1, R.string.refresh);
            toolbar.getMenu().add(Menu.NONE, 2, 2, R.string.lookup);
        }
        toolbar.getMenu().add(Menu.NONE, 3, 3, R.string.logout);
        toolbar.setOnMenuItemClickListener(
                item -> {
                    if (item.getItemId() == 1) cargarUsuarios();
                    if (item.getItemId() == 2 && !requests.estaOcupado()) buscarPorId();
                    if (item.getItemId() == 3) cerrarSesion(false);
                    return true;
                });
        adapter = new UserAdapter();
        ListView list = findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (requiereSesion())
                        startActivity(
                                new Intent(this, UserDetailActivity.class)
                                        .putExtra("id", adapter.getItem(position).id));
                });
        search = findViewById(R.id.search);
        search.addTextChangedListener(
                new TextWatcher() {
                    public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

                    public void onTextChanged(CharSequence text, int start, int before, int count) {
                        adapter.filtrar(text.toString());
                        mostrarConteo();
                    }

                    public void afterTextChanged(Editable text) {}
                });
        findViewById(R.id.create)
                .setOnClickListener(
                        view -> {
                            if (requiereSesion()) irA(UserFormActivity.class);
                        });
        findViewById(R.id.retry).setOnClickListener(view -> cargarUsuarios());
        requests.ocupado()
                .observe(
                        this,
                        busy -> {
                            findViewById(R.id.progress).setVisibility(busy ? View.VISIBLE : View.GONE);
                            findViewById(R.id.retry).setEnabled(!busy);
                            if (busy) findViewById(R.id.statePanel).setVisibility(View.GONE);
                        });
        requests.resultado()
                .observe(
                        this,
                        result -> {
                            if (result == null) return;
                            if (manejarAutenticacion(result)) return;
                            if (!result.fueExitoso()) {
                                mostrarEstado(result.error, true);
                                return;
                            }
                            if ("lookup".equals(result.action)) {
                                if (result.handled) return;
                                result.handled = true;
                                try {
                                    startActivity(
                                            new Intent(this, UserDetailActivity.class)
                                                    .putExtra("id", new User(result.comoObjeto()).id));
                                } catch (Exception exception) {
                                    mostrarEstado("El servidor devolvió un usuario inválido.", true);
                                }
                                return;
                            }
                            try {
                                JSONArray array = result.comoArreglo();
                                List<User> users = new ArrayList<>();
                                for (int i = 0; i < array.length(); i++)
                                    users.add(new User(array.getJSONObject(i)));
                                users.sort(
                                        Comparator.comparing(
                                                user -> user.name.toLowerCase(java.util.Locale.ROOT)));
                                adapter.reemplazar(users);
                                adapter.filtrar(search.getText().toString());
                                mostrarConteo();
                            } catch (Exception exception) {
                                mostrarEstado(
                                        "La respuesta del servidor no tiene el formato esperado.", true);
                            }
                        });
        if (!session.puedeConsultar()) {
            findViewById(R.id.searchLayout).setVisibility(View.GONE);
            findViewById(R.id.count).setVisibility(View.GONE);
            mostrarEstado(
                    "Tu sesión MEMBER está activa. El backend permite consultar usuarios a ADMIN y REVIEWER; la edición y eliminación requieren ADMIN.",
                    false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && requiereSesion() && session.puedeConsultar() && !requests.estaOcupado())
            cargarUsuarios();
    }

    private void cargarUsuarios() {
        if (!requiereSesion() || !session.puedeConsultar() || requests.estaOcupado()) return;
        requests.ejecutar("list", "GET", "/api/users", null, session.obtenerToken());
    }

    private void mostrarEstado(String message, boolean retry) {
        findViewById(R.id.statePanel).setVisibility(View.VISIBLE);
        mostrarMensaje(message);
        findViewById(R.id.retry).setVisibility(retry ? View.VISIBLE : View.GONE);
    }

    private void mostrarConteo() {
        ponerTexto(R.id.count, adapter.getCount() + " de " + adapter.totalUsuarios() + " usuarios");
        if (adapter.getCount() == 0)
            mostrarEstado(
                    getString(adapter.totalUsuarios() == 0 ? R.string.empty : R.string.no_matches), false);
        else findViewById(R.id.statePanel).setVisibility(View.GONE);
    }

    private void buscarPorId() {
        EditText input = new EditText(this);
        input.setHint("UUID del usuario");
        input.setSingleLine(true);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.lookup)
                        .setView(input)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.consult, null)
                        .create();
        dialog.setOnShowListener(
                ignored ->
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setOnClickListener(
                                        view -> {
                                            String id = input.getText().toString().trim();
                                            if (!InputValidator.identificadorValido(id)) {
                                                input.setError("Escribe un UUID completo y válido.");
                                                return;
                                            }
                                            dialog.dismiss();
                                            if (requiereSesion())
                                                requests.ejecutar(
                                                        "lookup",
                                                        "GET",
                                                        "/api/users/" + id,
                                                        null,
                                                        session.obtenerToken());
                                        }));
        dialog.show();
    }
}
