package chat.rocket.android.service.observer;

import android.content.Context;
import chat.rocket.android.service.Registerable;
import chat.rocket.android.ws.RocketChatWebSocketAPI;
import io.realm.RealmObject;
import jp.co.crowdworks.realm_java_helpers.RealmListObserver;

abstract class AbstractModelObserver<T extends RealmObject> extends RealmListObserver<T>
    implements Registerable {

  protected final Context context;
  protected final String serverConfigId;
  protected final RocketChatWebSocketAPI webSocketAPI;

  protected AbstractModelObserver(Context context, String serverConfigId,
      RocketChatWebSocketAPI api) {
    this.context = context;
    this.serverConfigId = serverConfigId;
    webSocketAPI = api;
  }

  @Override public void register() {
    sub();
  }

  @Override public void unregister() {
    unsub();
  }
}
