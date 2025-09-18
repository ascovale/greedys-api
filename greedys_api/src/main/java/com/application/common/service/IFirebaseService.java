package com.application.common.service;

import java.util.Collection;
import java.util.Map;

import com.google.firebase.auth.FirebaseToken;

/**
 * Interfaccia comune per FirebaseService e il suo mock
 */
public interface IFirebaseService {
    
    FirebaseToken verifyToken(String idToken);
    
    void sendNotification(String title, String body, Map<String, String> data, Collection<String> tokens);
}
