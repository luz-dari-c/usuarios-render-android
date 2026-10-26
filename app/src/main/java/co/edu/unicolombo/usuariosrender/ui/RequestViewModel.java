package co.edu.unicolombo.usuariosrender.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import co.edu.unicolombo.usuariosrender.data.ApiClient;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RequestViewModel extends ViewModel {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<ApiClient.Result> result = new MutableLiveData<>();

    public LiveData<Boolean> ocupado() {
        return busy;
    }

    public LiveData<ApiClient.Result> resultado() {
        return result;
    }

    public boolean estaOcupado() {
        return Boolean.TRUE.equals(busy.getValue());
    }

    public void ejecutar(String action, String method, String path, JSONObject payload, String token) {
        if (estaOcupado()) return;
        result.setValue(null);
        busy.setValue(true);
        worker.execute(
                () -> {
                    ApiClient.Result response = ApiClient.solicitar(action, method, path, payload, token);
                    busy.postValue(false);
                    result.postValue(response);
                });
    }

    @Override
    protected void onCleared() {
        worker.shutdown();
    }
}
