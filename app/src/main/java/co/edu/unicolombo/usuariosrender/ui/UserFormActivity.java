package co.edu.unicolombo.usuariosrender.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import co.edu.unicolombo.usuariosrender.R;
import co.edu.unicolombo.usuariosrender.data.InputValidator;
import co.edu.unicolombo.usuariosrender.data.User;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;
import java.util.UUID;

public final class UserFormActivity extends BaseActivity {
    private static final String[] ROLES = {"MEMBER", "REVIEWER", "ADMIN"};
    private static final String[] STATUSES = {"PENDING", "ACTIVE", "INACTIVE", "BLOCKED"};
    private EditText name, email, password, identifier;
    private Spinner role, status;
    private String id;
    private boolean editing, bound, managed;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        editing = getIntent().hasExtra("id");
        managed = editing || (saved != null ? saved.getBoolean("form_managed") : session.esAdministrador());
        if (managed && (!requiereSesion() || !session.esAdministrador())) {
            finish();
            return;
        }
        id =
                editing
                        ? getIntent().getStringExtra("id")
                        : saved != null ? saved.getString("form_id") : UUID.randomUUID().toString();
        if (!InputValidator.identificadorValido(id)) {
            finish();
            return;
        }
        bound = saved != null && saved.getBoolean("form_bound");
        mostrarPantalla(
                R.layout.activity_user_form, getString(editing ? R.string.edit : R.string.new_user), true);
        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        identifier = findViewById(R.id.identifier);
        role = findViewById(R.id.role);
        status = findViewById(R.id.status);
        identifier.setText(id);
        role.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ROLES));
        status.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, STATUSES));
        ponerTexto(R.id.title, getString(editing ? R.string.edit : R.string.new_user));
        ponerTexto(R.id.help, getString(editing ? R.string.edit_help : R.string.form_help));
        ((TextView) findViewById(R.id.save)).setText(editing ? R.string.save : R.string.create);
        if (editing) {
            findViewById(R.id.statusGroup).setVisibility(View.VISIBLE);
            ((TextInputLayout) findViewById(R.id.passwordLayout)).setHint(getString(R.string.new_password));
        } else if (!session.esAdministrador()) {
            role.setEnabled(false);
            ponerTexto(R.id.roleHelp, getString(R.string.registration_help));
        }
        findViewById(R.id.save).setOnClickListener(view -> guardarUsuario());
        findViewById(R.id.retry).setOnClickListener(view -> cargarUsuario());
        requests.ocupado()
                .observe(
                        this,
                        busy -> {
                            findViewById(R.id.progress).setVisibility(busy ? View.VISIBLE : View.GONE);
                            habilitarFormulario(!busy && (!editing || bound));
                        });
        if (editing && !bound && !requests.estaOcupado() && requests.resultado().getValue() == null)
            cargarUsuario();
    }

    @Override
    protected void onPostCreate(Bundle saved) {
        super.onPostCreate(saved);
        if (identifier == null) return;
        requests.resultado()
                .observe(
                        this,
                        result -> {
                            if (result == null) return;
                            if (editing && manejarAutenticacion(result)) return;
                            if (!result.fueExitoso()) {
                                mostrarMensaje(result.error);
                                if ("load_form".equals(result.action))
                                    findViewById(R.id.retry).setVisibility(View.VISIBLE);
                                return;
                            }
                            if ("load_form".equals(result.action)) {
                                if (bound) return;
                                try {
                                    User user = new User(result.comoObjeto());
                                    name.setText(user.name);
                                    email.setText(user.email);
                                    seleccionar(role, ROLES, user.role);
                                    seleccionar(status, STATUSES, user.status);
                                    bound = true;
                                    habilitarFormulario(true);
                                    ocultarMensaje();
                                    findViewById(R.id.retry).setVisibility(View.GONE);
                                } catch (Exception exception) {
                                    mostrarMensaje("No se pudieron cargar los datos del usuario.");
                                }
                                return;
                            }
                            if (result.handled) return;
                            result.handled = true;
                            password.setText("");
                            Toast.makeText(
                                            this,
                                            editing
                                                    ? "Cambios guardados en el servidor."
                                                    : "Usuario creado. Estado inicial: PENDING. Un administrador debe activarlo para que pueda iniciar sesión.",
                                            Toast.LENGTH_LONG)
                                    .show();
                            if (editing && id.equals(session.identificadorUsuario())) cerrarSesion(false);
                            else finish();
                        });
    }

    private void cargarUsuario() {
        if (requiereSesion())
            requests.ejecutar("load_form", "GET", "/api/users/" + id, null, session.obtenerToken());
    }

    private void habilitarFormulario(boolean enabled) {
        name.setEnabled(enabled);
        email.setEnabled(enabled);
        password.setEnabled(enabled);
        role.setEnabled(enabled && session.esAdministrador());
        status.setEnabled(enabled);
        findViewById(R.id.save).setEnabled(enabled);
    }

    private static void seleccionar(Spinner spinner, String[] values, String target) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(target)) spinner.setSelection(i);
    }

    private void guardarUsuario() {
        if (requests.estaOcupado() || (editing && !bound)) return;
        if (managed && !requiereSesion()) return;
        String fullName = name.getText().toString().trim();
        String mail = email.getText().toString().trim();
        String secret = password.getText().toString();
        TextInputLayout nameLayout = findViewById(R.id.nameLayout),
                emailLayout = findViewById(R.id.emailLayout),
                passwordLayout = findViewById(R.id.passwordLayout);
        nameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        if (!InputValidator.nombreValido(fullName)) {
            nameLayout.setError(getString(R.string.validation_name));
            return;
        }
        if (!InputValidator.correoValido(mail)) {
            emailLayout.setError(getString(R.string.validation_email));
            return;
        }
        if (!InputValidator.contrasenaValida(secret, editing)) {
            passwordLayout.setError(getString(R.string.validation_password));
            return;
        }
        try {
            JSONObject body =
                    new JSONObject()
                            .put("name", fullName)
                            .put("email", mail)
                            .put("role", managed ? role.getSelectedItem().toString() : "MEMBER");
            if (!secret.isEmpty()) body.put("password", secret);
            if (editing) body.put("status", status.getSelectedItem().toString());
            else body.put("id", id);
            ocultarMensaje();
            requests.ejecutar(
                    editing ? "update" : "create",
                    editing ? "PUT" : "POST",
                    "/api/users" + (editing ? "/" + id : ""),
                    body,
                    editing ? session.obtenerToken() : "");
        } catch (Exception exception) {
            mostrarMensaje("No se pudieron preparar los datos del usuario.");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putString("form_id", id);
        out.putBoolean("form_bound", bound);
        out.putBoolean("form_managed", managed);
        super.onSaveInstanceState(out);
    }
}
