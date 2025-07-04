package catering.businesslogic.menu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import catering.persistence.BatchUpdateHandler;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

public class Section {

    public static void create(int menuid, Section sec, int posInMenu) {
        String secInsert = "INSERT INTO MenuSections (menu_id, name, position) VALUES (?, ?, ?)";
        PersistenceManager.executeUpdate(secInsert, menuid, sec.name, posInMenu);
        sec.id = PersistenceManager.getLastId();

        if (sec.sectionItems.size() > 0) {
            MenuItem.create(menuid, sec.id, sec.sectionItems);
        }
    }

    public static void create(int menuid, List<Section> sections) {
        String query = "INSERT INTO MenuSections (menu_id, name, position) VALUES (?, ?, ?);";
        PersistenceManager.executeBatchUpdate(query, sections.size(), new BatchUpdateHandler() {
            @Override
            public void handleBatchItem(PreparedStatement ps, int batchCount) throws SQLException {
                ps.setInt(1, menuid);
                ps.setString(2, sections.get(batchCount).name);

                ps.setInt(3, batchCount);
            }

            @Override
            public void handleGeneratedIds(ResultSet rs, int count) throws SQLException {
                sections.get(count).id = rs.getInt(1);
            }
        });

        for (Section s : sections) {
            if (s.sectionItems.size() > 0) {
                MenuItem.create(menuid, s.id, s.sectionItems);
            }
        }
    }

    public static ArrayList<Section> loadSections(int menu_id) {
        ArrayList<Section> result = new ArrayList<>();
        String query = "SELECT * FROM MenuSections WHERE menu_id = ? ORDER BY position";

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Section s = new Section(rs.getString("name"));
                s.id = rs.getInt("id");
                result.add(s);
            }
        }, menu_id);

        for (Section s : result) {
            // load items for each section
            s.sectionItems = MenuItem.loadMenuItems(menu_id, s.id);
        }

        return result;
    }

    public static void deleteSection(int menu_id, Section s) {

        String query = "DELETE FROM MenuItems WHERE section_id = ? AND menu_id = ?";
        PersistenceManager.executeUpdate(query, s.id, menu_id);

        query = "DELETE FROM MenuSections WHERE id = ?";
        PersistenceManager.executeUpdate(query, s.id);
    }

    public static void saveSectionName(Section s) {
        String query = "UPDATE MenuSections SET name = ? WHERE id = ?";
        PersistenceManager.executeUpdate(query, s.name, s.id);
    }

    public static void saveItemOrder(Section s) {
        String query = "UPDATE MenuItems SET position = ? WHERE id = ?";
        PersistenceManager.executeBatchUpdate(query, s.sectionItems.size(), new BatchUpdateHandler() {
            @Override
            public void handleBatchItem(PreparedStatement ps, int batchCount) throws SQLException {
                ps.setInt(1, batchCount);
                ps.setInt(2, s.sectionItems.get(batchCount).getId());
            }

            @Override
            public void handleGeneratedIds(ResultSet rs, int count) throws SQLException {
                // no generated ids to handle
            }
        });
    }

    private int id;
    private String name;
    private ArrayList<MenuItem> sectionItems;

    public Section(String name) {
        id = 0;
        this.name = name;
        sectionItems = new ArrayList<>();
    }

    public Section(Section toCopy) {
        this(toCopy.name);
        for (MenuItem original : toCopy.sectionItems) {
            this.sectionItems.add(new MenuItem(original));
        }
    }

    public void addItem(MenuItem mi) {
        this.sectionItems.add(mi);
    }

    public void updateItems(ArrayList<MenuItem> newItems) {
        ArrayList<MenuItem> updatedList = new ArrayList<>();
        for (int i = 0; i < newItems.size(); i++) {
            MenuItem mi = newItems.get(i);
            MenuItem prev = this.findItemById(mi.getId());
            if (prev == null) {
                updatedList.add(mi);
            } else {
                prev.setDescription(mi.getDescription());
                prev.setRecipe(mi.getRecipe());
                updatedList.add(prev);
            }
        }
        this.sectionItems.clear();
        this.sectionItems.addAll(updatedList);
    }

    public int getItemPosition(MenuItem mi) {
        return this.sectionItems.indexOf(mi);
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Add section header with ID and name
        sb.append(String.format("Section [id=%d, name='%s']", id, name));

        // Add items count
        sb.append(String.format(" - %d items", sectionItems.size()));

        // Add section items if any exist
        if (!sectionItems.isEmpty()) {
            sb.append(":\n");
            for (int i = 0; i < sectionItems.size(); i++) {
                MenuItem item = sectionItems.get(i);
                sb.append(String.format("  %d. %s", (i + 1), item.toString()));

                // Add newline for all but the last item
                if (i < sectionItems.size() - 1) {
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<MenuItem> getItems() {
        return this.sectionItems;
    }

    public int getItemsCount() {
        return sectionItems.size();
    }

    public void moveItem(MenuItem mi, int position) {
        sectionItems.remove(mi);
        sectionItems.add(position, mi);
    }

    public void removeItem(MenuItem mi) {
        sectionItems.remove(mi);
    }

    private MenuItem findItemById(int id) {
        for (MenuItem mi : sectionItems) {
            if (mi.getId() == id)
                return mi;
        }
        return null;
    }

    public Section deepCopy() {
        Section copy = new Section(this.name);

        copy.id = this.id;

        for (MenuItem mi : this.getItems()) {
            copy.addItem(mi.deepCopy());
        }
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Section other = (Section) obj;

        // If both sections have valid IDs, compare by ID
        if (this.id > 0 && other.id > 0) {
            return this.id == other.id;
        }

        // Otherwise, compare by name and items
        boolean nameMatch = (this.name == null && other.name == null) ||
                (this.name != null && this.name.equals(other.name));

        // If names don't match, sections are not equal
        if (!nameMatch)
            return false;

        // If item counts differ, sections are not equal
        if (this.sectionItems.size() != other.sectionItems.size())
            return false;

        // Compare items in order (assuming order matters)
        for (int i = 0; i < this.sectionItems.size(); i++) {
            MenuItem thisItem = this.sectionItems.get(i);
            MenuItem otherItem = other.sectionItems.get(i);
            if (!thisItem.equals(otherItem))
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
            // Otherwise use name and a representation of the items
            result = prime * result + (name != null ? name.hashCode() : 0);

            // Add a hash representation of items (avoiding full iteration for performance)
            result = prime * result + sectionItems.size();

            // Include hash codes of first and last items if they exist
            if (!sectionItems.isEmpty()) {
                result = prime * result + sectionItems.get(0).hashCode();
                if (sectionItems.size() > 1) {
                    result = prime * result + sectionItems.get(sectionItems.size() - 1).hashCode();
                }
            }
        }

        return result;
    }
}
