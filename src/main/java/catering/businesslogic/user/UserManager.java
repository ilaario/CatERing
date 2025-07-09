package catering.businesslogic.user;

import java.util.logging.Logger;
import catering.businesslogic.UseCaseLogicException;
import catering.util.LogManager;

public class UserManager {
    private static final Logger LOGGER = LogManager.getLogger(UserManager.class);

    private User currentUser;

    public void fakeLogin(String username) throws UseCaseLogicException {
        LOGGER.info("Attempting login for user: " + username);
        this.currentUser = User.load(username);
        LOGGER.info("User successfully logged in: " + username);
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}
