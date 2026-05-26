package service;

/**
 * Service interface for handling authentication, login, logout, and credential validation.
 */
public interface AuthenticationService {

    /**
     * Attempts to log in a user with the provided credentials.
     * If successful, populates the UserSession singleton.
     *
     * @param username the username
     * @param password the plaintext password
     * @return true if login was successful, false otherwise
     */
    boolean login(String username, String password);

    /**
     * Logs out the current user, clearing the UserSession singleton.
     */
    void logout();

    /**
     * Changes the password of a user.
     *
     * @param username the username of the user
     * @param oldPassword the current plaintext password
     * @param newPassword the new plaintext password
     * @return true if password change succeeded, false otherwise
     */
    boolean changePassword(String username, String oldPassword, String newPassword);

    /**
     * Validates if the username and password are not empty and are valid format/length.
     *
     * @param username the username
     * @param password the password
     * @return true if inputs are valid, false otherwise
     */
    boolean validateUser(String username, String password);
}
