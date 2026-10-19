package com.example.orderimport.support;

import com.example.orderimport.application.port.NotificationGateway;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 送信された通知を覚えておくだけの {@link NotificationGateway}。
 *
 * <p>「異常終了したら運用チームへ通知する」という仕様を、通知手段に依存せずに
 * 検証するためのテスト用実装。
 */
public class RecordingNotificationGateway implements NotificationGateway {

    private final List<Notification> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(Notification notification) {
        sent.add(notification);
    }

    public List<Notification> sent() {
        return List.copyOf(sent);
    }

    public void clear() {
        sent.clear();
    }
}
