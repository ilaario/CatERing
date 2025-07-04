package catering.businesslogic.user;

import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class User {

    public static enum Role {
        CUOCO, CHEF, ORGANIZZATORE, SERVIZIO
    };

    private int id;
    private String username;
    private Set<Role> roles;

    public User() {
        this(null);
    }

    public User(String username) {
        id = 0;
        this.username = username;
        this.roles = new HashSet<>();
    }

    public boolean isCook() {
        return roles.contains(Role.CUOCO);
    }

    public boolean isChef() {
        return roles.contains(Role.CHEF);
    }

    public String getUserName() {
        return username;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(" ").append(username);

        if (!roles.isEmpty()) {
            sb.append(" : ");
            for (User.Role r : roles) {
                sb.append(r.toString()).append(" ");
            }
        }

        return sb.toString();
    }

    /**
     * Sets the username for this user
     * 
     * @param username The new username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Adds a role to this user
     * 
     * @param role The role to add
     * @return true if the role was added, false if it was already present
     */
    public boolean addRole(Role role) {
        return this.roles.add(role);
    }

    /**
     * Removes a role from this user
     * 
     * @param role The role to remove
     * @return true if the role was removed, false if it wasn't present
     */
    public boolean removeRole(Role role) {
        return this.roles.remove(role);
    }

    /**
     * Checks if the user has a specific role
     * 
     * @param role The role to check
     * @return true if the user has the role, false otherwise
     */
    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }

    /**
     * Gets all roles assigned to this user
     * 
     * @return A set containing all user roles
     */
    public Set<Role> getRoles() {
        return new HashSet<>(this.roles); // Return a copy to prevent external modification
    }

    // STATIC METHODS FOR PERSISTENCE

    public static User load(int uid) {
        User load = new User();
        String userQuery = "SELECT * FROM Users WHERE id = ?";

        PersistenceManager.executeQuery(userQuery, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                load.id = rs.getInt("id");
                load.username = rs.getString("username");
            }
        }, uid); // Pass uid as parameter

        if (load.id > 0) {
            loadRolesForUser(load);
        }
        return load;
    }

    public static User load(String username) {
        User u = new User();
        String userQuery = "SELECT * FROM Users WHERE username = ?";

        PersistenceManager.executeQuery(userQuery, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                u.id = rs.getInt("id");
                u.username = rs.getString("username");
            }
        }, username); // Pass username as parameter

        if (u.id > 0) {
            loadRolesForUser(u);
        }
        return u;
    }

    public static ArrayList<User> loadAllUsers() {
        String userQuery = "SELECT * FROM Users";
        ArrayList<User> users = new ArrayList<>();

        PersistenceManager.executeQuery(userQuery, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                User u = new User();
                u.id = rs.getInt("id");
                u.username = rs.getString("username");

                // Load roles for this user
                loadRolesForUser(u);
                users.add(u);
            }
        });

        return users;
    }

    // Helper method to load roles for a user
    private static void loadRolesForUser(User u) {
        String roleQuery = "SELECT * FROM UserRoles WHERE user_id = ?";

        PersistenceManager.executeQuery(roleQuery, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                int role = rs.getInt("role_id");
                switch (role) {
                    case 0:
                        u.roles.add(User.Role.CUOCO);
                        break;
                    case 1:
                        u.roles.add(User.Role.CHEF);
                        break;
                    case 2:
                        u.roles.add(User.Role.ORGANIZZATORE);
                        break;
                    case 3:
                        u.roles.add(User.Role.SERVIZIO);
                        break;
                }
            }
        }, u.id); // Pass u.id as parameter
    }

    /**
     * Saves a new user to the database
     * 
     * @return true if successful, false otherwise
     */
    public boolean save() {
        if (id != 0)
            return false; // Already exists

        String query = "INSERT INTO Users (username) VALUES(?)";

        PersistenceManager.executeUpdate(query, username);
        id = PersistenceManager.getLastId();

        if (id > 0) {
            // Save roles
            saveUserRoles();
            return true;
        }
        return false;
    }

    /**
     * Updates an existing user in the database
     * 
     * @return true if successful, false otherwise
     */
    public boolean update() {
        if (id == 0)
            return false; // Not in DB

        String query = "UPDATE Users SET username = ? WHERE id = ?";

        int rows = PersistenceManager.executeUpdate(query, username, id);

        // Update user roles
        saveUserRoles();

        return rows > 0;
    }

    /**
     * Deletes a user from the database
     * 
     * @return true if successful, false otherwise
     */
    public boolean delete() {
        if (id == 0)
            return false; // Not in DB

        // First delete user roles
        String deleteRolesQuery = "DELETE FROM UserRoles WHERE user_id = ?";
        PersistenceManager.executeUpdate(deleteRolesQuery, id);

        // Then delete user
        String deleteUserQuery = "DELETE FROM Users WHERE id = ?";
        int rows = PersistenceManager.executeUpdate(deleteUserQuery, id);

        if (rows > 0) {
            id = 0;
            return true;
        }
        return false;
    }

    /**
     * Saves user roles to the database
     */
    private void saveUserRoles() {
        if (id == 0)
            return; // User not saved yet

        // First delete existing roles
        String deleteQuery = "DELETE FROM UserRoles WHERE user_id = ?";
        PersistenceManager.executeUpdate(deleteQuery, id);

        // Then insert new roles
        for (Role role : roles) {
            String roleId = getRoleStringId(role);
            String insertQuery = "INSERT INTO UserRoles (user_id, role_id) VALUES(?, ?)";
            PersistenceManager.executeUpdate(insertQuery, id, roleId);
        }
    }

    /**
     * Converts Role enum to string ID for database
     */
    private String getRoleStringId(Role role) {
        switch (role) {
            case CUOCO:
                return "c";
            case CHEF:
                return "h";
            case ORGANIZZATORE:
                return "o";
            case SERVIZIO:
                return "s";
            default:
                return "";
        }
    }

    /**
     * Determines if this user is equal to another object.
     * Two users are considered equal if they have the same ID or, if ID is 0,
     * the same username.
     *
     * @param obj The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        User other = (User) obj;

        // If both users have valid IDs, compare by ID
        if (this.id > 0 && other.id > 0) {
            return this.id == other.id;
        }

        // Otherwise, if either ID is 0, compare by username
        return this.username != null && this.username.equals(other.username);
    }

    /**
     * Generates a hash code for this user.
     * The hash code is based on ID if it's valid (> 0), or username otherwise.
     *
     * @return A hash code value for this user
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        // Use ID if it's valid
        if (id > 0) {
            result = prime * result + id;
        } else {
            // Otherwise use username
            result = prime * result + (username != null ? username.hashCode() : 0);
        }

        return result;
    }
}
