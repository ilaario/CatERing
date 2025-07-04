package catering.businesslogic.menu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import catering.businesslogic.recipe.KitchenProcess;
import catering.businesslogic.recipe.Recipe;
import catering.businesslogic.user.User;
import catering.persistence.BatchUpdateHandler;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

public class Menu {

    // Feature constants
    public static final String FEATURE_NEEDS_COOK = "needsCook";
    public static final String FEATURE_FINGER_FOOD = "fingerFood";
    public static final String FEATURE_BUFFET = "buffet";
    public static final String FEATURE_WARM_DISHES = "warmDishes";
    public static final String FEATURE_NEEDS_KITCHEN = "needsKitchen";

    public static final String[] DEFAULT_FEATURES = {
            FEATURE_NEEDS_COOK,
            FEATURE_FINGER_FOOD,
            FEATURE_BUFFET,
            FEATURE_WARM_DISHES,
            FEATURE_NEEDS_KITCHEN
    };

    public static void create(Menu m) {

        String query = "INSERT INTO Menus (title, owner_id, published) VALUES (?, ?, ?);";

        int[] result = PersistenceManager.executeBatchUpdate(query, 1, new BatchUpdateHandler() {
            @Override
            public void handleBatchItem(PreparedStatement ps, int batchCount) throws SQLException {
                ps.setString(1, m.title);
                ps.setInt(2, m.owner.getId());
                ps.setBoolean(3, m.published);
            }

            @Override
            public void handleGeneratedIds(ResultSet rs, int count) throws SQLException {
                if (count == 0) {
                    m.id = rs.getInt(1);
                }
            }
        });

        if (result[0] > 0) {
            // Save features
            saveFeaturesToDB(m);

            // Save sections if any
            if (!m.sections.isEmpty()) {
                Section.create(m.id, m.sections);
            }

            // Save free items if any
            if (!m.freeItems.isEmpty()) {
                MenuItem.create(m.id, 0, m.freeItems);
            }
        }
    }

    /**
     * Load a menu from the database by ID
     */
    public static Menu load(Integer id) {

        String query = "SELECT * FROM Menus WHERE id = ?";

        Menu m = new Menu();

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {

                m.id = id;
                m.title = rs.getString("title");
                m.published = rs.getBoolean("published");
                m.owner = User.load(rs.getInt("owner_id"));

                // Load sections
                m.sections = Section.loadSections(id);

                // Load free items
                m.freeItems = MenuItem.loadMenuItems(id, 0);

                // Load features
                loadFeaturesFromDB(m);

                // Check if menu is in use
                checkIfMenuIsInUse(m);

            }
        }, id);

        return m;
    }

    /**
     * Delete a menu from the database
     */
    public static void delete(Menu m) {
        PersistenceManager.executeUpdate("DELETE FROM MenuItems WHERE menu_id = ?", m.id);
        PersistenceManager.executeUpdate("DELETE FROM MenuSections WHERE menu_id = ?", m.id);
        PersistenceManager.executeUpdate("DELETE FROM MenuFeatures WHERE menu_id = ?", m.getId());
        PersistenceManager.executeUpdate("DELETE FROM Menus WHERE id = ?", m.getId());
    }

    public static void saveTitle(Menu m) {
        PersistenceManager.executeUpdate("UPDATE Menus SET title = ? WHERE id = ?",
                m.getTitle(), m.getId());
    }

    /**
     * Save the published status of a menu
     */
    public static void savePublished(Menu m) {
        PersistenceManager.executeUpdate("UPDATE Menus SET published = ? WHERE id = ?",
                m.published, m.getId());
    }

    /**
     * Save the features of a menu
     */
    public static void saveFeatures(Menu m) {
        // First delete existing features
        PersistenceManager.executeUpdate("DELETE FROM MenuFeatures WHERE menu_id = ?", m.getId());

        // Then add the updated features
        saveFeaturesToDB(m);
    }

    /**
     * Save the section order of a menu
     */
    public static void saveSectionOrder(Menu m) {
        String query = "UPDATE MenuSections SET position = ? WHERE id = ?";
        PersistenceManager.executeBatchUpdate(query, m.sections.size(), new BatchUpdateHandler() {
            @Override
            public void handleBatchItem(PreparedStatement ps, int batchCount) throws SQLException {
                ps.setInt(1, batchCount);
                ps.setInt(2, m.sections.get(batchCount).getId());
            }

            @Override
            public void handleGeneratedIds(ResultSet rs, int count) throws SQLException {
                // No generated IDs to handle
            }
        });
    }

    /**
     * Save the order of free items in a menu
     */
    public static void saveFreeItemOrder(Menu m) {
        String query = "UPDATE MenuItems SET position = ? WHERE id = ?";
        PersistenceManager.executeBatchUpdate(query, m.freeItems.size(), new BatchUpdateHandler() {
            @Override
            public void handleBatchItem(PreparedStatement ps, int batchCount) throws SQLException {
                ps.setInt(1, batchCount);
                ps.setInt(2, m.freeItems.get(batchCount).getId());
            }

            @Override
            public void handleGeneratedIds(ResultSet rs, int count) throws SQLException {
                // No generated IDs to handle
            }
        });
    }

    /**
     * Save features to the database
     */
    private static void saveFeaturesToDB(Menu m) {
        String query = "INSERT INTO MenuFeatures (menu_id, name, value) VALUES (?, ?, ?)";
        String[] features = m.features.keySet().toArray(new String[0]);
        PersistenceManager.executeBatchUpdate(query, features.length, new BatchUpdateHandler() {
            @Override
            public void handleBatchItem(PreparedStatement ps, int batchCount) throws SQLException {
                ps.setInt(1, m.id);
                ps.setString(2, features[batchCount]);
                ps.setBoolean(3, m.features.get(features[batchCount]));
            }

            @Override
            public void handleGeneratedIds(ResultSet rs, int count) throws SQLException {
                // No generated IDs to handle
            }
        });
    }

    /**
     * Load features from the database
     */
    private static void loadFeaturesFromDB(Menu m) {
        String query = "SELECT * FROM MenuFeatures WHERE menu_id = ?";
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                m.features.put(rs.getString("name"), rs.getBoolean("value"));
            }
        }, m.id);
    }

    private static void checkIfMenuIsInUse(Menu m) {
        String query = "SELECT * FROM Services WHERE approved_menu_id = ?";
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                m.inUse = true;
            }
        }, m.id);
    }

    private int id;

    private String title;

    private boolean published;

    private boolean inUse;

    private HashMap<String, Boolean> features;

    private ArrayList<MenuItem> freeItems;

    private ArrayList<Section> sections;

    private User owner;

    public Menu(User owner, String title, String[] menuFeatures) {
        this.id = 0;
        this.title = title;
        this.owner = owner;
        this.published = false;
        this.inUse = false;
        this.features = new HashMap<String, Boolean>();
        this.sections = new ArrayList<Section>();
        this.freeItems = new ArrayList<MenuItem>();

        for (String feature : menuFeatures) {
            this.features.put(feature, false);
        }
    }

    public Menu(User owner, String title) {
        this(owner, title, DEFAULT_FEATURES);
    }

    private Menu() {
        this(null, null);
    }

    public Section addSection(String name) {
        Section sec = new Section(name);
        this.sections.add(sec);
        return sec;
    }

    public Section getSection(int position) {
        if (position < 0 || position >= sections.size()) {
            throw new IndexOutOfBoundsException("Invalid section position");
        }
        return this.sections.get(position);
    }

    public Section getSectionById(int id) {
        for (Section sec : sections) {
            if (sec.getId() == id) {
                return sec;
            }
        }
        return null;
    }

    public Section getSection(String name) {
        for (Section sec : sections) {
            if (sec.getName().equals(name)) {
                return sec;
            }
        }
        return null;
    }

    public Section getSection(MenuItem mi) {
        for (Section sec : sections) {
            if (sec.getItemPosition(mi) >= 0) {
                return sec;
            }
        }
        if (freeItems.contains(mi)) {
            return null;
        }
        throw new IllegalArgumentException("MenuItem not found in this menu");
    }

    public boolean hasSection(Section sec) {
        return this.sections.contains(sec);
    }

    public int getSectionPosition(Section sec) {
        return this.sections.indexOf(sec);
    }

    public ArrayList<Section> getSections() {
        return this.sections;
    }

    public void moveSection(Section sec, int position) {
        sections.remove(sec);
        sections.add(position, sec);
    }

    public void removeSection(Section s, boolean deleteItems) {
        if (!deleteItems) {
            this.freeItems.addAll(s.getItems());
        }
        this.sections.remove(s);
    }

    public int getSectionCount() {
        return sections.size();
    }

    public MenuItem addItem(Recipe recipe, Section sec, String desc) {
        MenuItem mi = new MenuItem(recipe, desc);
        if (sec != null) {
            sec.addItem(mi);
        } else {
            this.freeItems.add(mi);
        }
        return mi;
    }

    public ArrayList<MenuItem> getFreeItems() {
        return this.freeItems;
    }

    public int getFreeItemPosition(MenuItem mi) {
        return freeItems.indexOf(mi);
    }

    public int getFreeItemCount() {
        return freeItems.size();
    }

    public void moveFreeItem(MenuItem mi, int position) {
        this.freeItems.remove(mi);
        this.freeItems.add(position, mi);
    }

    public void changeItemSection(MenuItem mi, Section oldSec, Section newSec) {
        if (oldSec == null) {
            freeItems.remove(mi);
        } else {
            oldSec.removeItem(mi);
        }

        if (newSec == null) {
            freeItems.add(mi);
        } else {
            newSec.addItem(mi);
        }
    }

    public void removeItem(MenuItem mi) {
        Section sec = getSection(mi);
        if (sec == null) {
            freeItems.remove(mi);
        } else {
            sec.removeItem(mi);
        }
    }

    public void updateFreeItems(ArrayList<MenuItem> newItems) {
        ArrayList<MenuItem> updatedList = new ArrayList<>();
        for (MenuItem mi : newItems) {
            MenuItem prev = findItemById(mi.getId());
            if (prev == null) {
                updatedList.add(mi);
            } else {
                prev.setDescription(mi.getDescription());
                prev.setRecipe(mi.getRecipe());
                updatedList.add(prev);
            }
        }
        this.freeItems.clear();
        this.freeItems.addAll(updatedList);
    }

    public ArrayList<MenuItem> getItems() {
        ArrayList<MenuItem> allItems = new ArrayList<>();
        allItems.addAll(this.freeItems);

        for (Section section : this.sections) {
            allItems.addAll(section.getItems());
        }

        return allItems;
    }

    public ArrayList<KitchenProcess> getKitchenProcesses() {
        ArrayList<KitchenProcess> allKitchenProcesses = new ArrayList<>();

        for (MenuItem item : this.getItems()) {
            Recipe recipe = item.getRecipe();
            allKitchenProcesses.add(recipe);
            allKitchenProcesses.addAll(recipe.getPreparations());
        }

        return allKitchenProcesses;
    }

    public void initializeDefaultFeatures() {
        for (String feature : DEFAULT_FEATURES) {
            if (!features.containsKey(feature)) {
                features.put(feature, false);
            }
        }
    }

    // ===== FEATURE MANAGEMENT =====

    public Map<String, Boolean> getFeatures() {
        return this.features;
    }

    public boolean getFeature(String feature) {
        return this.features.getOrDefault(feature, false);
    }

    public void setFeature(String feature, boolean val) {
        if (this.features.containsKey(feature)) {
            this.features.put(feature, val);
        }
    }

    public void setFeatures(boolean needsCook, boolean fingerFood, boolean buffet,
            boolean warmDishes, boolean needsKitchen) {
        setNeedsCook(needsCook);
        setFingerFood(fingerFood);
        setBuffet(buffet);
        setWarmDishes(warmDishes);
        setNeedsKitchen(needsKitchen);
    }

    public void setAndSaveFeatures(boolean needsCook, boolean fingerFood, boolean buffet,
            boolean warmDishes, boolean needsKitchen) {
        setFeatures(needsCook, fingerFood, buffet, warmDishes, needsKitchen);
        saveFeatures(this);
    }

    // Specific feature getters and setters
    public boolean needsCook() {
        return getFeature(FEATURE_NEEDS_COOK);
    }

    public void setNeedsCook(boolean value) {
        setFeature(FEATURE_NEEDS_COOK, value);
    }

    public boolean isFingerFood() {
        return getFeature(FEATURE_FINGER_FOOD);
    }

    public void setFingerFood(boolean value) {
        setFeature(FEATURE_FINGER_FOOD, value);
    }

    public boolean isBuffet() {
        return getFeature(FEATURE_BUFFET);
    }

    public void setBuffet(boolean value) {
        setFeature(FEATURE_BUFFET, value);
    }

    public boolean hasWarmDishes() {
        return getFeature(FEATURE_WARM_DISHES);
    }

    public void setWarmDishes(boolean value) {
        setFeature(FEATURE_WARM_DISHES, value);
    }

    public boolean needsKitchen() {
        return getFeature(FEATURE_NEEDS_KITCHEN);
    }

    public void setNeedsKitchen(boolean value) {
        setFeature(FEATURE_NEEDS_KITCHEN, value);
    }

    public boolean requiresKitchenPreparation() {
        return needsKitchen() || needsCook() || hasWarmDishes();
    }

    public int getId() {
        return id;
    }

    // ===== BASIC GETTERS AND SETTERS =====

    public User getOwner() {
        return this.owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public boolean isInUse() {
        return this.inUse;
    }

    public boolean isOwner(User u) {
        return u.getId() == this.owner.getId();
    }

    // ===== UTILITY METHODS =====

    public Menu deepCopy() {

        Menu copy = new Menu(this.owner, this.title, DEFAULT_FEATURES);

        copy.published = this.published;
        copy.inUse = this.inUse;

        for (Map.Entry<String, Boolean> entry : this.features.entrySet())
            copy.features.put(entry.getKey(), entry.getValue());

        for (Section sec : this.sections)
            copy.sections.add(sec.deepCopy());

        for (MenuItem mi : this.freeItems)
            copy.freeItems.add(mi.deepCopy());

        return copy;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(this.title)
                .append(" (autore: ")
                .append(this.owner != null ? this.owner.getUserName() : "unknown")
                .append("), ")
                .append(published ? "pubblicato" : "non pubblicato")
                .append(", ")
                .append(inUse ? "in uso" : "non in uso");

        // Add features information
        for (String f : features.keySet()) {
            result.append("\n").append(f).append(": ").append(features.get(f));
        }

        // Add sections
        if (!sections.isEmpty()) {
            result.append("\n\nSections:");
            for (Section sec : sections) {
                result.append("\n").append(sec.toString());
            }
        }

        // Add free items if any
        if (!freeItems.isEmpty()) {
            result.append("\n\nVOCI LIBERE:");
            for (MenuItem mi : freeItems) {
                result.append("\n\t").append(mi.toString());
            }
        }

        return result.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Menu other = (Menu) obj;
        if (id != other.id)
            return false;
        if (published != other.published)
            return false;
        if (inUse != other.inUse)
            return false;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        if (owner == null) {
            if (other.owner != null)
                return false;
        } else if (!owner.equals(other.owner))
            return false;
        if (features == null) {
            if (other.features != null)
                return false;
        } else if (!features.equals(other.features))
            return false;
        if (freeItems == null) {
            if (other.freeItems != null)
                return false;
        } else if (!freeItems.equals(other.freeItems))
            return false;
        if (sections == null) {
            if (other.sections != null)
                return false;
        } else if (!sections.equals(other.sections))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + id;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + Boolean.hashCode(published);
        result = 31 * result + Boolean.hashCode(inUse);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (features != null ? features.hashCode() : 0);
        result = 31 * result + (freeItems != null ? freeItems.hashCode() : 0);
        result = 31 * result + (sections != null ? sections.hashCode() : 0);
        return result;
    }

    private MenuItem findItemById(int id) {
        for (MenuItem mi : freeItems) {
            if (mi.getId() == id) {
                return mi;
            }
        }
        return null;
    }

}
