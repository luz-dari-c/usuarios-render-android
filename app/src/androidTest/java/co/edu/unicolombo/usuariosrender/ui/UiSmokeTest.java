package co.edu.unicolombo.usuariosrender.ui;

import android.content.Context;
import android.util.Base64;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import co.edu.unicolombo.usuariosrender.R;
import co.edu.unicolombo.usuariosrender.data.InputValidator;
import co.edu.unicolombo.usuariosrender.data.SessionStore;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class UiSmokeTest {
    private Context context;

    @Before
    public void limpiarSesionDePrueba() {
        context = ApplicationProvider.getApplicationContext();
        new SessionStore(context).limpiar();
    }

    @Test
    public void rechazaInicioInvalidoSinConexion() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        activity.findViewById(R.id.login).performClick();
                        assertNotNull(((TextInputLayout) activity.findViewById(R.id.emailLayout)).getError());
                        assertFalse(activity.requests.estaOcupado());
                    });
        }
    }

    @Test
    public void conservaDatosDelFormularioAlGirar() {
        AtomicReference<String> id = new AtomicReference<>();
        try (ActivityScenario<UserFormActivity> scenario = ActivityScenario.launch(UserFormActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        id.set(((EditText) activity.findViewById(R.id.identifier)).getText().toString());
                        assertTrue(InputValidator.identificadorValido(id.get()));
                        ((EditText) activity.findViewById(R.id.name)).setText("Prueba local de interfaz");
                        ((EditText) activity.findViewById(R.id.email)).setText("interfaz@example.com");
                        assertFalse(((Spinner) activity.findViewById(R.id.role)).isEnabled());
                        assertFalse(activity.findViewById(R.id.password).isSaveEnabled());
                    });
            scenario.recreate();
            scenario.onActivity(
                    activity -> {
                        assertEquals(
                                id.get(),
                                ((EditText) activity.findViewById(R.id.identifier)).getText().toString());
                        assertEquals(
                                "Prueba local de interfaz",
                                ((EditText) activity.findViewById(R.id.name)).getText().toString());
                        assertEquals(
                                "interfaz@example.com",
                                ((EditText) activity.findViewById(R.id.email)).getText().toString());
                    });
        }
    }

    @Test
    public void cifraTokenYRechazaSesionVencida() throws Exception {
        SessionStore session = new SessionStore(context);
        String token = tokenLocalDePrueba();
        session.guardar(token, "interfaz@example.com", 3600);
        assertEquals(token, new SessionStore(context).obtenerToken());
        assertTrue(session.esAdministrador());
        String saved =
                context.getSharedPreferences("render_session", Context.MODE_PRIVATE).getString("token", "");
        assertFalse(saved.isEmpty());
        assertNotEquals(token, saved);
        context.getSharedPreferences("render_session", Context.MODE_PRIVATE)
                .edit()
                .putLong("expiry", 1L)
                .commit();
        assertFalse(session.sesionActiva());
        assertEquals("", session.obtenerToken());
        session.limpiar();
    }

    @Test
    public void administradorVencidoNoCreaMiembro() throws Exception {
        SessionStore session = new SessionStore(context);
        session.guardar(tokenLocalDePrueba(), "interfaz@example.com", 3600);
        try (ActivityScenario<UserFormActivity> scenario = ActivityScenario.launch(UserFormActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        ((EditText) activity.findViewById(R.id.name)).setText("Prueba sin envío");
                        ((EditText) activity.findViewById(R.id.email)).setText("interfaz@example.com");
                        ((EditText) activity.findViewById(R.id.password)).setText("SoloPrueba2026!");
                        ((Spinner) activity.findViewById(R.id.role)).setSelection(1);
                        context.getSharedPreferences("render_session", Context.MODE_PRIVATE)
                                .edit()
                                .putLong("expiry", 1L)
                                .commit();
                        activity.findViewById(R.id.save).performClick();
                        assertFalse(activity.requests.estaOcupado());
                        assertTrue(activity.isFinishing());
                    });
        }
    }

    private static String tokenLocalDePrueba() throws Exception {
        JSONObject payload =
                new JSONObject()
                        .put("sub", "00000000-0000-0000-0000-000000000099")
                        .put("role", "ADMIN")
                        .put("exp", System.currentTimeMillis() / 1000 + 3600);
        return "eyJhbGciOiJIUzI1NiJ9."
                + Base64.encodeToString(
                        payload.toString().getBytes(StandardCharsets.UTF_8),
                        Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING)
                + ".local-test-signature";
    }
}
