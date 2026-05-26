package session;

import model.User;

/**
 * UserSession singleton to store information of the currently logged-in user.
 */
public class UserSession {

    private static UserSession instance;

    private int    userId;
    private String username;
    private String fullName;
    private String role;
    private boolean isLoggedIn;

    private UserSession() {
        this.isLoggedIn = false;
    }

    /**
     * Get the single instance of UserSession.
     */
    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /**
     * Populate session data upon successful login.
     *
     * @param user the authenticated User model
     */
    public void login(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Cannot login with null user");
        }
        this.userId     = user.getUserId();
        this.username   = user.getUsername();
        this.fullName   = user.getFullName();
        this.role       = user.getRole();
        this.isLoggedIn = true;
    }

    /**
     * Clear all session data upon logout.
     */
    public void logout() {
        this.userId     = 0;
        this.username   = null;
        this.fullName   = null;
        this.role       = null;
        this.isLoggedIn = false;
    }

    /**
     * Returns a snapshot User object representing the currently logged in user.
     */
    public User getCurrentUser() {
        if (!isLoggedIn) {
            return null;
        }
        User user = new User();
        user.setUserId(this.userId);
        user.setUsername(this.username);
        user.setFullName(this.fullName);
        user.setRole(this.role);
        // Password hash and status are omitted/null for safety in UI/session layer
        return user;
    }

    // -------------------------------------------------------
    // Getters
    // -------------------------------------------------------

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }
}
