package chat.rocket.android.model;

import bolts.Task;
import bolts.TaskCompletionSource;
import chat.rocket.android.helper.LogcatIfError;
import chat.rocket.android.helper.TextUtils;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.annotations.PrimaryKey;
import java.util.UUID;
import jp.co.crowdworks.realm_java_helpers.RealmObjectObserver;
import jp.co.crowdworks.realm_java_helpers_bolts.RealmHelperBolts;
import org.json.JSONObject;

public class MethodCall extends RealmObject {

  @PrimaryKey private String methodCallId;
  private String serverConfigId; //not ServerConfig!(not to be notified the change of ServerConfig)
  private int syncstate;
  private String name;
  private String paramsJson;
  private String resultJson;
  private long timeout;

  public String getMethodCallId() {
    return methodCallId;
  }

  public void setMethodCallId(String methodCallId) {
    this.methodCallId = methodCallId;
  }

  public String getServerConfigId() {
    return serverConfigId;
  }

  public void setServerConfigId(String serverConfigId) {
    this.serverConfigId = serverConfigId;
  }

  public int getSyncstate() {
    return syncstate;
  }

  public void setSyncstate(int syncstate) {
    this.syncstate = syncstate;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getParamsJson() {
    return paramsJson;
  }

  public void setParamsJson(String paramsJson) {
    this.paramsJson = paramsJson;
  }

  public String getResultJson() {
    return resultJson;
  }

  public void setResultJson(String resultJson) {
    this.resultJson = resultJson;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public static class Error extends Exception {
    public Error(String message) {
      super(message);
    }

    public Error(Throwable exception) {
      super(exception);
    }
  }

  public static class Timeout extends Exception {
    public Timeout() {
      super("MethodCall.Timeout");
    }
  }

  /**
   * insert a new record to request a method call.
   */
  public static Task<String> execute(String serverConfigId, String name, String paramsJson,
      long timeout) {
    final String newId = UUID.randomUUID().toString();
    TaskCompletionSource<String> task = new TaskCompletionSource<>();
    RealmHelperBolts.executeTransaction(realm -> {
      MethodCall call = realm.createObjectFromJson(MethodCall.class, new JSONObject()
          .put("methodCallId", newId)
          .put("serverConfigId", serverConfigId)
          .put("syncstate", SyncState.NOT_SYNCED)
          .put("timeout", timeout)
          .put("name", name));
      call.setParamsJson(paramsJson);
      return null;
    }).continueWith(_task -> {
      if (_task.isFaulted()) {
        task.setError(_task.getError());
      } else {
        new RealmObjectObserver<MethodCall>() {
          @Override protected RealmQuery<MethodCall> query(Realm realm) {
            return realm.where(MethodCall.class).equalTo("methodCallId", newId);
          }

          @Override protected void onChange(MethodCall methodCall) {
            int syncstate = methodCall.getSyncstate();
            if (syncstate == SyncState.SYNCED) {
              String resultJson = methodCall.getResultJson();
              if (TextUtils.isEmpty(resultJson)) {
                task.setResult(null);
              }
              task.setResult(resultJson);
              exit(methodCall.getMethodCallId());
            } else if (syncstate == SyncState.FAILED) {
              task.setError(new Error(methodCall.getResultJson()));
              exit(methodCall.getMethodCallId());
            }
          }

          private void exit(String newId) {
            unsub();
            remove(newId).continueWith(new LogcatIfError());
          }
        }.sub();
      }
      return null;
    });
    return task.getTask();
  }

  /**
   * remove the request.
   */
  public static final Task<Void> remove(String methodCallId) {
    return RealmHelperBolts.executeTransaction(realm ->
        realm.where(MethodCall.class)
            .equalTo("methodCallId", methodCallId)
            .findAll()
            .deleteAllFromRealm());
  }
}
