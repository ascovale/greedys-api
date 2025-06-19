package com.application.service.notification;

import java.util.Collection;
import java.util.Map;

public interface INotificationService {
    String getTitle ();
    String getBody ();
    Map<String, String> getData ();
    Collection <String> getFcmTokens ();
}
