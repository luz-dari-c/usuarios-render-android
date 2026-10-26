package co.edu.unicolombo.usuariosrender.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SessionStore {
    private static final String ALIAS = "usuarios_render_session_v1";
    private final SharedPreferences preferences;

    public SessionStore(Context context) {
        preferences = context.getSharedPreferences("render_session", Context.MODE_PRIVATE);
    }

    private SecretKey claveCifrado() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(
                new KeyGenParameterSpec.Builder(
                                ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build());
        return generator.generateKey();
    }

    public void guardar(String accessToken, String email, long expiresIn) throws Exception {
        JSONObject claims = datosToken(accessToken);
        if (claims.optString("sub").isEmpty() || claims.optString("role").isEmpty() || expiresIn <= 0)
            throw new IllegalArgumentException("El servidor devolvió una sesión inválida.");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, claveCifrado());
        String encrypted =
                Base64.encodeToString(
                        cipher.doFinal(accessToken.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        long expiry = Math.min(System.currentTimeMillis() + expiresIn * 1000L, claims.getLong("exp") * 1000L);
        if (!preferences
                .edit()
                .putString("token", encrypted)
                .putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .putLong("expiry", expiry)
                .putString("email", email)
                .putString("role", claims.getString("role"))
                .putString("subject", claims.getString("sub"))
                .commit()) throw new IllegalStateException("No se pudo guardar la sesión en el dispositivo.");
    }

    public String obtenerToken() {
        if (!sesionActiva()) return "";
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    claveCifrado(),
                    new GCMParameterSpec(
                            128, Base64.decode(preferences.getString("iv", ""), Base64.NO_WRAP)));
            return new String(
                    cipher.doFinal(Base64.decode(preferences.getString("token", ""), Base64.NO_WRAP)),
                    StandardCharsets.UTF_8);
        } catch (Exception exception) {
            limpiar();
            return "";
        }
    }

    private static JSONObject datosToken(String token) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("JWT inválido");
        return new JSONObject(
                new String(
                        Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP), StandardCharsets.UTF_8));
    }

    public boolean sesionActiva() {
        return preferences.contains("token")
                && preferences.getLong("expiry", 0) > System.currentTimeMillis() + 5000;
    }

    public boolean esAdministrador() {
        return sesionActiva() && "ADMIN".equals(rol());
    }

    public boolean puedeConsultar() {
        return sesionActiva() && (esAdministrador() || "REVIEWER".equals(rol()));
    }

    public String correo() {
        return preferences.getString("email", "");
    }

    public String rol() {
        return preferences.getString("role", "");
    }

    public String identificadorUsuario() {
        return preferences.getString("subject", "");
    }

    public void limpiar() {
        preferences.edit().clear().apply();
    }
}
