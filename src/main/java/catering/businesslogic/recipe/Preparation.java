package catering.businesslogic.recipe;

import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Preparation represents an intermediate food preparation step.
 * It implements KitchenProcess and has attributes specific to intermediate
 * steps.
 */
public class Preparation implements KitchenProcess {

    private int id;
    private String name;
    private String description;

    /**
     * Default constructor for loading from DB
     */
    private Preparation() {
    }

    /**
     * Creates a new preparation with the given name
     * 
     * @param name The preparation name
     */
    public Preparation(String name) {
        id = 0;
        this.name = name;
        this.description = "";
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this preparation
     * 
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean isRecipe() {
        return false; // This is not a recipe
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description for this preparation
     * 
     * @param description The description text
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Preparation other = (Preparation) obj;

        // If both preparations have valid IDs, compare by ID
        if (this.id > 0 && other.id > 0) {
            return this.id == other.id;
        }

        // Otherwise, compare by name and description
        boolean nameMatch = (this.name == null && other.name == null) ||
                (this.name != null && this.name.equals(other.name));

        boolean descMatch = (this.description == null && other.description == null) ||
                (this.description != null && this.description.equals(other.description));

        return nameMatch && descMatch;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        // Use ID if it's valid
        if (id > 0) {
            result = prime * result + id;
        } else {
            // Otherwise use name and description
            result = prime * result + (name != null ? name.hashCode() : 0);
            result = prime * result + (description != null ? description.hashCode() : 0);
        }

        return result;
    }

    /**
     * Loads all preparations from the database
     * 
     * @return List of all preparations
     */
    public static ArrayList<Preparation> loadAllPreparations() {
        ArrayList<Preparation> preparations = new ArrayList<>();

        String query = "SELECT * FROM Preparations";
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Preparation prep = new Preparation(rs.getString("name"));
                prep.id = rs.getInt("id");

                // Load additional properties if they exist in DB
                try {
                    prep.description = rs.getString("description");
                } catch (SQLException e) {
                    prep.description = "";
                }

                preparations.add(prep);
            }
        });

        // Sort preparations by name
        Collections.sort(preparations, new Comparator<Preparation>() {
            @Override
            public int compare(Preparation o1, Preparation o2) {
                return (o1.getName().compareTo(o2.getName()));
            }
        });

        return preparations;
    }

    /**
     * Gets all preparations from the database
     * 
     * @return List of all preparations
     */
    public static ArrayList<Preparation> getAllPreparations() {
        return loadAllPreparations();
    }

    /**
     * Loads a preparation by its ID
     * 
     * @param id The preparation ID
     * @return The loaded preparation or null if not found
     */
    public static Preparation loadPreparationById(int id) {
        Preparation[] prepHolder = new Preparation[1]; // Use array to allow modification in lambda
        String query = "SELECT * FROM Preparations WHERE id = ?";

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Preparation prep = new Preparation();
                prep.name = rs.getString("name");
                prep.id = id;
                // Load additional properties if they exist in DB
                try {
                    prep.description = rs.getString("description");
                } catch (SQLException e) {
                    prep.description = "";
                }
                prepHolder[0] = prep;
            }
        }, id); // Pass id as parameter

        return prepHolder[0];
    }

    /**
     * Saves a new preparation to the database
     * 
     * @return true if successful, false otherwise
     */
    public boolean save() {
        if (id != 0)
            return false; // Already exists

        String query = "INSERT INTO Preparations (name, description) VALUES(?, ?)";

        PersistenceManager.executeUpdate(query, name, description);
        id = PersistenceManager.getLastId();
        return true;
    }

    /**
     * Updates an existing preparation in the database
     * 
     * @return true if successful, false otherwise
     */
    public boolean update() {
        if (id == 0)
            return false; // Not in DB

        String query = "UPDATE Preparations SET name = ?, description = ? WHERE id = ?";

        int rows = PersistenceManager.executeUpdate(query, name, description, id);
        return rows > 0;
    }

    /**
     * Gets recipes that use this preparation
     * 
     * @return List of recipes using this preparation
     */
    public List<Recipe> getUsedInRecipes() {
        List<Recipe> result = new ArrayList<>();

        if (id == 0)
            return result; // Not in DB

        String query = "SELECT recipe_id FROM RecipePreparations WHERE preparation_id = ?";
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                int recipeId = rs.getInt("recipe_id");
                Recipe recipe = Recipe.loadRecipe(recipeId);
                if (recipe != null) {
                    result.add(recipe);
                }
            }
        }, id); // Pass id as parameter

        return result;
    }
}
