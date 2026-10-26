package co.edu.unicolombo.usuariosrender.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import co.edu.unicolombo.usuariosrender.R;
import co.edu.unicolombo.usuariosrender.data.InputValidator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;

public final class LoginActivity extends BaseActivity {
    private TextInputEditText email, password;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!session.obtenerToken().isEmpty()) {
            irA(UsersActivity.class);
            finish();
            return;
        }
        mostrarPantalla(R.layout.activity_login, "", false);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        if (state != null) email.setText(state.getString("login_email", ""));
        findViewById(R.id.login).setOnClickListener(view -> iniciarSesion());
        findViewById(R.id.register).setOnClickListener(view -> irA(UserFormActivity.class));
        password.setOnEditorActionListener(
                (view, action, event) -> {
                    if (action == EditorInfo.IME_ACTION_DONE) {
                        iniciarSesion();
                        return true;
                    }
                    return false;
                });
        requests.ocupado()
                .observe(
                        this,
                        busy -> {
                            findViewById(R.id.progress).setVisibility(busy ? View.VISIBLE : View.GONE);
                            findViewById(R.id.login).setEnabled(!busy);
                            findViewById(R.id.register).setEnabled(!busy);
                            email.setEnabled(!busy);
                            password.setEnabled(!busy);
                            if (busy)
                                mostrarMensaje(
                                        "Conectando con Render… El primer acceso puede tardar cerca de un minuto.");
                        });
        requests.resultado()
                .observe(
                        this,
                        result -> {
                            if (result == null || result.handled) return;
                            result.handled = true;
                            if (!result.fueExitoso()) {
                                mostrarMensaje(result.error);
                                return;
                            }
                            try {
                                JSONObject json = result.comoObjeto();
                                session.guardar(
                                        json.getString("accessToken"),
                                        email.getText().toString().trim(),
                                        json.getLong("expiresIn"));
                                password.setText("");
                                startActivity(new Intent(this, UsersActivity.class));
                                finish();
                            } catch (Exception exception) {
                                mostrarMensaje(
                                        "El servidor respondió, pero no se pudo abrir una sesión válida.");
                            }
                        });
    }

    private void iniciarSesion() {
        if (requests.estaOcupado()) return;
        String mail = email.getText().toString().trim();
        String secret = password.getText().toString();
        TextInputLayout mailLayout = findViewById(R.id.emailLayout),
                passwordLayout = findViewById(R.id.passwordLayout);
        mailLayout.setError(null);
        passwordLayout.setError(null);
        if (!InputValidator.correoValido(mail)) {
            mailLayout.setError(getString(R.string.validation_email));
            return;
        }
        if (secret.isEmpty()) {
            passwordLayout.setError("Escribe tu contraseña.");
            return;
        }
        ocultarMensaje();
        try {
            requests.ejecutar(
                    "login",
                    "POST",
                    "/api/auth/login",
                    new JSONObject().put("email", mail).put("password", secret),
                    "");
        } catch (Exception exception) {
            mostrarMensaje("No se pudo preparar el inicio de sesión.");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        if (email != null) out.putString("login_email", email.getText().toString());
        super.onSaveInstanceState(out);
    }
}
