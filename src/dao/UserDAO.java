package dao;

import java.util.List;
import model.User;

/**
 * Data Access Object (DAO) interface for USERS table.
 */
public interface UserDAO {

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return the User object, or null if not found
     */
    User findByUsername(String username);

    /**
     * Finds a user by their UserID.
     *
     * @param id the user ID to search for
     * @return the User object, or null if not found
     */
    User findById(int id);

    /**
     * Inserts a new user into the database.
     *
     * @param user the User to insert (User ID is identity, auto-generated)
     * @return true if insertion was successful, false otherwise
     */
    boolean insert(User user);

    /**
     * Updates an existing user's information (FullName, Role, Status).
     *
     * @param user the User to update
     * @return true if update was successful, false otherwise
     */
    boolean update(User user);

    /**
     * Deletes a user permanently from the database (hard delete).
     *
     * @param userId the ID of the user to delete
     * @return true if successful, false otherwise
     */
    boolean delete(int userId);

    /**
     * Returns a list of all active or locked users (excludes soft-deleted/INACTIVE users).
     *
     * @return a List of User objects
     */
    List<User> getAll();

    /**
     * Authenticates a user by checking credentials in the database.
     *
     * @param username the username input
     * @param password the plaintext password input
     * @return the authenticated User object, or null if authentication failed
     */
    User authenticate(String username, String password);
}
