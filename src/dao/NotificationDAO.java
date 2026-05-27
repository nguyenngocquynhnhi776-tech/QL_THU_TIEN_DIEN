package dao;

import model.Notification;
import java.util.List;

/**
 * Data Access Object (DAO) interface for NOTIFICATION table.
 */
public interface NotificationDAO {

    /**
     * Inserts a new notification.
     */
    boolean insert(Notification notif);

    /**
     * Deletes a notification by its ID.
     */
    boolean delete(int notifId);

    /**
     * Deletes all notifications.
     */
    boolean deleteAll();

    /**
     * Marks a single notification as read.
     */
    boolean markAsRead(int notifId);

    /**
     * Marks all notifications as read.
     */
    boolean markAllAsRead();

    /**
     * Gets a list of notifications, filtered and paginated.
     */
    List<Notification> getFiltered(String type, Boolean unreadOnly, int limit, int offset);

    /**
     * Gets the count of unread notifications.
     */
    int getUnreadCount();
}
