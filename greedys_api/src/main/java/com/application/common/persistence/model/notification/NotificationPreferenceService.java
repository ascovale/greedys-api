package com.application.common.persistence.model.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.NotificationPreferencesDAO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceService {

    private final NotificationPreferencesDAO preferencesRepository;

    public NotificationPreferences getPreferences(Long userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
    }

    public NotificationPreferences updatePreferences(NotificationPreferences preferences) {
        return preferencesRepository.save(preferences);
    }

    private NotificationPreferences createDefaultPreferences(Long userId) {
        log.info("Creating default notification preferences for user {}", userId);

        NotificationPreferences prefs = NotificationPreferences.builder()
                .userId(userId)
                .build();

        return preferencesRepository.save(prefs);
    }
}
