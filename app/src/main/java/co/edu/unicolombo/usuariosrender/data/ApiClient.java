package co.edu.unicolombo.usuariosrender.data;

import co.edu.unicolombo.usuariosrender.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public final class ApiClient {
    private ApiClient() {}

    public static final class Result {
        public final String action;
        public final int code;
        public final String body, error;
        public boolean handled;

        Result(String action, int code, String body, String error) {
            this.action = action;
            this.code = code;
            this.body = body;
            this.error = error;
        }

        public boolean fueExitoso() {
            return code >= 200 && code < 300 && error == null;
        }

        public JSONObject comoObjeto() throws JSONException {
            return new JSONObject(body);
        }

        public JSONArray comoArreglo() throws JSONException {
            return new JSONArray(body);
        }
    }

    public static Result solicitar(
            String action, String method, String path, JSONObject payload, String token) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BuildConfig.API_BASE_URL + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(25000);
            connection.setReadTimeout(120000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setUseCaches(false);
            if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
            if (payload != null) {
                byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream stream = connection.getOutputStream()) {
                    stream.write(bytes);
                }
            }
            int code = connection.getResponseCode();
            String body =
                    leerRespuesta(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            return new Result(
                    action, code, body, code >= 200 && code < 300 ? null : describirError(code, body));
        } catch (UnknownHostException exception) {
            return new Result(
                    action,
                    0,
                    "",
                    "No se encontró el servidor. Comprueba Internet y que el servicio de Render siga disponible.");
        } catch (SocketTimeoutException exception) {
            return new Result(
                    action,
                    0,
                    "",
                    "El servidor tardó demasiado en responder. Si guardabas datos, consulta la lista antes de volver a enviarlos.");
        } catch (Exception exception) {
            return new Result(
                    action,
                    0,
                    "",
                    "No se pudo completar la conexión. Comprueba Internet. Si guardabas datos, verifica primero si ya aparecen en la lista.");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String leerRespuesta(InputStream source) throws Exception {
        if (source == null) return "";
        try (InputStream stream = source;
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = stream.read(buffer)) != -1) {
                if (output.size() + length > 2_000_000)
                    throw new IllegalStateException("Respuesta demasiado grande");
                output.write(buffer, 0, length);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static String describirError(int code, String body) {
        String fallback =
                switch (code) {
                    case 400 -> "Revisa los datos enviados.";
                    case 401 -> "Credenciales inválidas, cuenta no activa o sesión vencida.";
                    case 403 -> "Tu rol no tiene permiso para realizar esta operación.";
                    case 404 -> "El usuario solicitado no existe.";
                    case 409 -> "Ya existe un usuario con ese correo o identificador.";
                    case 429 -> "Demasiadas solicitudes. Espera un momento y vuelve a intentarlo.";
                    default ->
                            code >= 500
                                    ? "El servidor presentó un error. Inténtalo más tarde."
                                    : "La solicitud no pudo completarse.";
                };
        try {
            JSONObject json = new JSONObject(body);
            String message = json.optString("message", "");
            if (!message.trim().isEmpty() && !"null".equals(message) && message.length() < 600)
                return message + " (HTTP " + code + ")";
        } catch (JSONException ignored) {
        }
        return fallback + " (HTTP " + code + ")";
    }
}
