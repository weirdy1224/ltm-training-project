package com.examly.springapp.repository;

import com.examly.springapp.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepo extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Transactional
    void deleteByIdAndUserId(Long id, Long userId);

    @Modifying
    @Transactional
    @Query("""
           UPDATE Notification n
           SET n.isRead = true
           WHERE n.userId = :userId
           AND n.isRead = false
           """)
    int markAllAsRead(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("""
           DELETE FROM Notification n
           WHERE n.userId = :userId
           """)
    void deleteAllByUserId(@Param("userId") Long userId);
}
