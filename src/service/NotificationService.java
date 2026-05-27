package service;

import model.Notification;
import java.util.List;

/**
 * Service interface for notifications and system audits.
 */
public interface NotificationService {

    /**
     * Adds/saves a notification.
     */
    void addNotification(Notification notif);

    /**
     * Helper to create and save a notification easily.
     */
    void addNotification(String title, String content, String type, String icon);

    /**
     * Easy helper with related entity fields.
     */
    void addNotification(String title, String content, String type, String icon, String relatedEntity, Integer relatedId);

    /**
     * Deletes a notification by its ID.
     */
    boolean deleteNotification(int notifId);

    /**
     * Deletes all notifications.
     */
    boolean clearAll();

    /**
     * Marks a notification as read.
     */
    boolean markAsRead(int notifId);

    /**
     * Marks all notifications as read.
     */
    boolean markAllRead();

    /**
     * Retrieves notifications.
     */
    List<Notification> getNotifications(String type, Boolean unreadOnly, int limit, int offset);

    /**
     * Returns unread notifications count.
     */
    int getUnreadCount();

    /**
     * Runs a comprehensive, database-backed system health check/audit
     * to dynamically generate real alerts based on actual database states.
     */
    void runSystemHealthAudit();
}
