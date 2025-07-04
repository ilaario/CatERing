package catering.businesslogic.recipe;

import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Recipe represents a complete dish that can be prepared and served.
 * It implements KitchenProcess and may include multiple Preparations.
 */
public class Recipe implements KitchenProcess {

    private int id;
    private String name;
    private String description;

    private ArrayList<Preparation> preparations; // Associated preparation steps

    /**
     * Default constructor for loading from DB
     */
    private Recipe() {
        this.preparations = new ArrayList<>();
    }

    /**
     * Creates a new recipe with the given name
     * 
     * @param name The recipe name
     */
    public Recipe(String name) {
        id = 0;
        this.name = name;
        this.description = "";
        this.preparations = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this recipe
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
        return true; // This is a recipe
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description for this recipe
     * 
     * @param description The description text
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Adds a preparation step to this recipe
     * 
     * @param preparation The preparation to add
     */
    public void addPreparation(Preparation preparation) {
        if (!preparations.contains(preparation)) {
            preparations.add(preparation);
        }
    }

    /**
     * Removes a preparation step from this recipe
     * 
     * @param preparation The preparation to remove
     * @return true if found and removed, false otherwise
     */
    public boolean removePreparation(Preparation preparation) {
        return preparations.remove(preparation);
    }

    /**
     * Gets all preparation steps for this recipe
     * 
     * @return List of preparations
     */
    public ArrayList<Preparation> getPreparations() {
        return new ArrayList<>(preparations);
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Loads all recipes from the database
     * 
     * @return List of all recipes
     */
    public static ArrayList<Recipe> loadAllRecipes() {
        ArrayList<Recipe> recipes = new ArrayList<>();

        String query = "SELECT * FROM Recipes";
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Recipe rec = new Recipe(rs.getString("name"));
                rec.id = rs.getInt("id");

                // Load additional properties if they exist in DB
                try {
                    rec.description = rs.getString("description");
                } catch (SQLException e) {
                    rec.description = "";
                }
                recipes.add(rec);
            }
        });

        // Load preparations for each recipe
        for (Recipe recipe : recipes) {
            loadPreparationsForRecipe(recipe);
        }

        // Sort recipes by name
        Collections.sort(recipes, new Comparator<Recipe>() {
            @Override
            public int compare(Recipe o1, Recipe o2) {
                return (o1.getName().compareTo(o2.getName()));
            }
        });

        return recipes;
    }

    /**
     * Gets all recipes from the database
     * 
     * @return List of all recipes
     */
    public static ArrayList<Recipe> getAllRecipes() {
        return loadAllRecipes();
    }

    /**
     * Loads a recipe by its ID
     * 
     * @param id The recipe ID
     * @return The loaded recipe or null if not found
     */
    public static Recipe loadRecipe(int id) {
        Recipe[] recHolder = new Recipe[1]; // Use array to allow modification in lambda
        String query = "SELECT * FROM Recipes WHERE id = ?";

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Recipe rec = new Recipe();
                rec.name = rs.getString("name");
                rec.id = id;
                // Load additional properties if they exist in DB
                try {
                    rec.description = rs.getString("description");
                } catch (SQLException e) {
                    rec.description = "";
                }
                recHolder[0] = rec;
            }
        }, id); // Pass id as parameter

        // Load preparations for this recipe
        Recipe recipe = recHolder[0];
        if (recipe != null) {
            loadPreparationsForRecipe(recipe);
        }

        return recipe;
    }

    /**
     * Loads a recipe by its name
     * 
     * @param name The recipe name
     * @return The loaded recipe or null if not found
     */
    public static Recipe loadRecipe(String name) {
        Recipe[] recHolder = new Recipe[1]; // Use array to allow modification in lambda
        String query = "SELECT * FROM Recipes WHERE name = ?";

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Recipe rec = new Recipe();
                rec.name = rs.getString("name");
                rec.id = rs.getInt("id");

                try {
                    rec.description = rs.getString("description");
                } catch (SQLException e) {
                    rec.description = "";
                }
                recHolder[0] = rec;
            }
        }, name); // Pass name as parameter

        // Load preparations for this recipe
        Recipe recipe = recHolder[0];
        if (recipe != null) {
            loadPreparationsForRecipe(recipe);
        }

        return recipe;
    }

    /**
     * Loads preparations for a specific recipe
     * 
     * @param recipe The recipe to load preparations for
     */
    private static void loadPreparationsForRecipe(Recipe recipe) {
        String query = "SELECT preparation_id FROM RecipePreparations WHERE recipe_id = ?";
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                int prepId = rs.getInt("preparation_id");
                Preparation prep = Preparation.loadPreparationById(prepId);
                if (prep != null) {
                    recipe.addPreparation(prep);
                }
            }
        }, recipe.getId()); // Pass recipe.getId() as parameter
    }

    /**
     * Saves a new recipe to the database
     * 
     * @return true if successful, false otherwise
     */
    public boolean save() {
        if (id != 0)
            return false; // Already exists

        String query = "INSERT INTO Recipes (name, description) VALUES(?, ?)";

        PersistenceManager.executeUpdate(query, name, description);
        id = PersistenceManager.getLastId();

        // Save recipe-preparation relationships
        savePreparationRelationships();

        return true;
    }

    /**
     * Updates an existing recipe in the database
     * 
     * @return true if successful, false otherwise
     */
    public boolean update() {
        if (id == 0)
            return false; // Not in DB

        String query = "UPDATE Recipes SET name = ?, description = ? WHERE id = ?";

        int rows = PersistenceManager.executeUpdate(query, name, description, id);

        // Update recipe-preparation relationships
        savePreparationRelationships();

        return rows > 0;
    }

    /**
     * Saves the recipe-preparation relationships to the database
     */
    private void savePreparationRelationships() {
        if (id == 0)
            return; // Recipe not saved yet

        // First delete existing relationships
        String deleteQuery = "DELETE FROM RecipePreparations WHERE recipe_id = ?";
        PersistenceManager.executeUpdate(deleteQuery, id);

        // Then insert new relationships
        for (Preparation prep : preparations) {
            if (prep.getId() != 0) { // Only save if preparation is in DB
                String insertQuery = "INSERT INTO RecipePreparations (recipe_id, preparation_id) VALUES(?, ?)";
                PersistenceManager.executeUpdate(insertQuery, id, prep.getId());
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Recipe other = (Recipe) obj;

        // If both recipes have valid IDs, compare by ID
        if (this.id > 0 && other.id > 0) {
            return this.id == other.id;
        }

        // Otherwise, compare by name and description
        boolean nameMatch = (this.name == null && other.name == null) ||
                (this.name != null && this.name.equals(other.name));

        boolean descMatch = (this.description == null && other.description == null) ||
                (this.description != null && this.description.equals(other.description));

        // Basic equality check on name and description
        if (!nameMatch || !descMatch)
            return false;

        // Deeper equality check on preparations
        // First compare sizes
        if (this.preparations.size() != other.preparations.size())
            return false;

        // Then check if all preparations in this recipe are in the other recipe
        // Note: We don't check order here since preparation order might not matter
        for (Preparation prep : this.preparations) {
            if (!other.preparations.contains(prep))
                return false;
        }

        return true;
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

            // Include a representation of preparations
            result = prime * result + preparations.size();

            // Add hash codes of all preparations - be careful with potential circular
            // references
            for (Preparation prep : preparations) {
                // Just use the preparation ID or name to avoid circular reference issues
                if (prep.getId() > 0) {
                    result = prime * result + prep.getId();
                } else if (prep.getName() != null) {
                    result = prime * result + prep.getName().hashCode();
                }
            }
        }

        return result;
    }
}
