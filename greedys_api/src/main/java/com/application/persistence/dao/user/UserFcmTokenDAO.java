package com.application.persistence.dao.user;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.application.persistence.model.user.UserFcmToken;
import com.application.persistence.model.user.User;

@Repository
public interface UserFcmTokenDAO extends JpaRepository<UserFcmToken, Long> {
    UserFcmToken findByUser(User user);

    UserFcmToken findByFcmToken(String oldToken);

    List<UserFcmToken> findByUserId(Long id);
}
