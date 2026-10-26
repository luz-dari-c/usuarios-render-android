package co.edu.unicolombo.usuariosrender.data;

import org.json.JSONException;
import org.json.JSONObject;

public final class User {
    public final String id, name, email, role, status;

    public User(JSONObject json) throws JSONException {
        id = json.getString("id");
        name = json.getString("name");
        email = json.getString("email");
        role = json.getString("role");
        status = json.getString("status");
    }

    public String iniciales() {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "U";
        String first = parts[0].substring(0, 1);
        return (first + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : ""))
                .toUpperCase(java.util.Locale.ROOT);
    }
}
