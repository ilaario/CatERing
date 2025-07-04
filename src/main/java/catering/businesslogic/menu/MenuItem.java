package catering.businesslogic.menu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import catering.businesslogic.recipe.Recipe;
import catering.persistence.BatchUpdateHandler;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

public class MenuItem {

    public static void create(int menuid, int sectionid, ArrayList<MenuItem> items) {

        String itemInsert = "INSERT INTO MenuItems (menu_id, section_id, description, recipe_id, position) VALUES (?, ?, ?, ?, ?);";

        PersistenceManager.executeBatchUpdate(itemInsert, items.size(), new BatchUpdateHandler() {
            @Override
            public void handleBatchItem(PreparedStatement ps, int batchCount) throws SQLException {
                ps.setInt(1, menuid);
                ps.setInt(2, sectionid);
                ps.setString(3, items.get(batchCount).description);
                ps.setInt(4, items.get(batchCount).recipe.getId());
                ps.setInt(5, batchCount);
            }

            @Override
            public void handleGeneratedIds(ResultSet rs, int count) throws SQLException {
                items.get(count).id = rs.getInt(1);
            }
        });
    }

    public static void create(int menuid, int sectionid, MenuItem mi, int pos) {

        String itemInsert = "INSERT INTO MenuItems (menu_id, section_id, description, recipe_id, position) VALUES (?, ?, ?, ?, ?)";

        PersistenceManager.executeUpdate(itemInsert, menuid, sectionid, mi.description, mi.recipe.getId(), pos);

        mi.id = PersistenceManager.getLastId();
    }

    public static ArrayList<MenuItem> loadMenuItems(int menu_id, int sec_id) {

        ArrayList<MenuItem> result = new ArrayList<>();
        ArrayList<Integer> recids = new ArrayList<>();

        String query = "SELECT * FROM MenuItems WHERE menu_id = ? AND section_id = ? ORDER BY position";

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                MenuItem mi = new MenuItem();
                mi.id = rs.getInt("id");
                mi.description = rs.getString("description");
                result.add(mi);
                recids.add(rs.getInt("recipe_id"));
            }
        }, menu_id, sec_id);

        for (int i = 0; i < result.size(); i++) {
            result.get(i).recipe = Recipe.loadRecipe(recids.get(i));
        }

        return result;
    }

    public static void saveSection(int sec_id, MenuItem mi) {
        String upd = "UPDATE MenuItems SET section_id = ? WHERE id = ?";
        PersistenceManager.executeUpdate(upd, sec_id, mi.id);
    }

    public static void saveDescription(MenuItem mi) {
        String upd = "UPDATE MenuItems SET description = ? WHERE id = ?";
        PersistenceManager.executeUpdate(upd, mi.getDescription(), mi.id);
    }

    public static void removeItem(MenuItem mi) {
        String rem = "DELETE FROM MenuItems WHERE id = ?";
        PersistenceManager.executeUpdate(rem, mi.getId());
    }

    private int id;
    private String description;
    private Recipe recipe;

    public MenuItem(Recipe rec) {
        this(rec, rec.getName());
    }

    public MenuItem(Recipe rec, String desc) {
        id = 0;
        recipe = rec;
        description = desc;
    }

    public MenuItem(MenuItem toCopy) {
        this(toCopy.recipe, toCopy.description);
    }

    private MenuItem() {
        this(null, null);
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe itemRecipe) {
        this.recipe = itemRecipe;
    }

    public MenuItem deepCopy() {

        MenuItem copy = new MenuItem(this.recipe, this.description);

        copy.id = this.id;

        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Add the description
        sb.append(description);

        // Add recipe information if available
        if (recipe != null) {
            sb.append(" (Recipe: ");
            sb.append(recipe.getName());
            sb.append(")");
        } else {
            sb.append(" (No recipe assigned)");
        }

        // Add ID if the item has been persisted
        if (id > 0) {
            sb.append(" [ID: ");
            sb.append(id);
            sb.append("]");
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        MenuItem other = (MenuItem) obj;

        // If both items have valid IDs, compare by ID
        if (this.id > 0 && other.id > 0) {
            return this.id == other.id;
        }

        // Otherwise, compare by description and recipe
        boolean descriptionMatch = (this.description == null && other.description == null) ||
                (this.description != null && this.description.equals(other.description));

        boolean recipeMatch = (this.recipe == null && other.recipe == null) ||
                (this.recipe != null && this.recipe.equals(other.recipe));

        return descriptionMatch && recipeMatch;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        // Use ID if it's valid
        if (id > 0) {
            result = prime * result + id;
        } else {
            // Otherwise use description and recipe
            result = prime * result + (description != null ? description.hashCode() : 0);
            result = prime * result + (recipe != null ? recipe.hashCode() : 0);
        }

        return result;
    }
}
