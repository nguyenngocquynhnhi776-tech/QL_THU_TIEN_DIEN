package service.impl;

import dao.UserDAO;
import dao.impl.UserDAOImpl;
import model.User;
import service.AuthenticationService;
import session.UserSession;
import util.PasswordUtil;

/**
 * Implementation of AuthenticationService interface.
 */
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserDAO userDAO;

    public AuthenticationServiceImpl() {
        this.userDAO = new UserDAOImpl();
    }

    /**
     * Test/DI constructor.
     */
    public AuthenticationServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public boolean login(String username, String password) {
        if (!validateUser(username, password)) {
            return false;
        }
        
        User user = userDAO.authenticate(username, password);
        if (user != null) {
            // Set UserSession details
            UserSession.getInstance().login(user);
            return true;
        }
        return false;
    }

    @Override
    public void logout() {
        UserSession.getInstance().logout();
    }

    @Override
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        if (username == null || oldPassword == null || newPassword == null || newPassword.trim().isEmpty()) {
            return false;
        }

        User user = userDAO.findByUsername(username);
        if (user != null) {
            // Verify old password
            if (PasswordUtil.checkPassword(oldPassword, user.getPasswordHash())) {
                // Update with new password hash
                user.setPasswordHash(PasswordUtil.hashPassword(newPassword));
                boolean ok = userDAO.update(user);
                if (ok) {
                    new NotificationServiceImpl().addNotification(
                        "Thay đổi mật khẩu",
                        "Tài khoản " + username + " đã thay đổi mật khẩu bảo mật.",
                        "info", "info"
                    );
                }
                return ok;
            }
        }
        return false;
    }

    @Override
    public boolean validateUser(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        return true;
    }
}
