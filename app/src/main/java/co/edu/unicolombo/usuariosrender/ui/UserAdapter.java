package co.edu.unicolombo.usuariosrender.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import co.edu.unicolombo.usuariosrender.R;
import co.edu.unicolombo.usuariosrender.data.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UserAdapter extends BaseAdapter {
    private final List<User> all = new ArrayList<>(), visible = new ArrayList<>();
    private String query = "";

    public void reemplazar(List<User> users) {
        all.clear();
        all.addAll(users);
        filtrar(query);
    }

    public void filtrar(String value) {
        query = value.toLowerCase(Locale.ROOT).trim();
        visible.clear();
        for (User user : all)
            if ((user.name + " " + user.email).toLowerCase(Locale.ROOT).contains(query)) visible.add(user);
        notifyDataSetChanged();
    }

    public int totalUsuarios() {
        return all.size();
    }

    @Override
    public int getCount() {
        return visible.size();
    }

    @Override
    public User getItem(int position) {
        return visible.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null)
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        User user = getItem(position);
        ((TextView) view.findViewById(R.id.name)).setText(user.name);
        ((TextView) view.findViewById(R.id.email)).setText(user.email);
        ((TextView) view.findViewById(R.id.initials)).setText(user.iniciales());
        ((TextView) view.findViewById(R.id.badge)).setText(user.role + "  ·  " + user.status);
        return view;
    }
}
