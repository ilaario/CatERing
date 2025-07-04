package catering.businesslogic.kitchen;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import catering.businesslogic.recipe.KitchenProcess;
import catering.businesslogic.recipe.Preparation;
import catering.businesslogic.recipe.Recipe;
import catering.persistence.BatchUpdateHandler;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

public class KitchenTask {

    private int id;
    private String description;
    private KitchenProcess kitchenProcess;
    private int quantity;
    private int portions;
    private boolean ready;
    private boolean type;

    private KitchenTask() {
    }

    public KitchenTask(KitchenProcess rec) {
        this(rec, rec.getName());
    }

    public KitchenTask(KitchenProcess rec, String desc) {
        id = 0;
        kitchenProcess = rec;
        description = desc;
        type = rec.isRecipe();
        ready = false;
        quantity = 0;
        portions = 0;
    }

    public KitchenTask(KitchenTask mi) {
        this.id = 0;
        this.description = mi.description;
        this.kitchenProcess = mi.kitchenProcess;
        this.type = mi.type;
    }

    // STATIC METHODS FOR PERSISTENCE

    public static void saveAllNewTasks(int id, ArrayList<KitchenTask> taskList) {
        String secInsert = "INSERT INTO Tasks (sumsheet_id, kitchenproc_id, description, type, position, ready, quantity, portions) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";

        PersistenceManager.executeBatchUpdate(secInsert, taskList.size(), new BatchUpdateHandler() {
            @Override
            public void handleBatchItem(PreparedStatement ps, int batchCount) throws SQLException {
                ps.setInt(1, id);
                ps.setInt(2, taskList.get(batchCount).kitchenProcess.getId());
                ps.setString(3, taskList.get(batchCount).description);
                ps.setBoolean(4, taskList.get(batchCount).type);
                ps.setInt(5, batchCount);
                ps.setBoolean(6, taskList.get(batchCount).ready);
                ps.setInt(7, taskList.get(batchCount).quantity);
                ps.setInt(8, taskList.get(batchCount).portions);
            }

            @Override
            public void handleGeneratedIds(ResultSet rs, int count) throws SQLException {
                taskList.get(count).id = rs.getInt(1);
            }
        });

    }

    public static void saveNewTask(int id, KitchenTask task, int taskPosition) {
        String query = "INSERT INTO Tasks (sumsheet_id, kitchenproc_id, description, type, position, ready, quantity, portions) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PersistenceManager.executeUpdate(query,
                id,
                task.kitchenProcess.getId(),
                task.getDescription(),
                task.kitchenProcess.isRecipe(),
                taskPosition,
                task.ready,
                task.quantity,
                task.portions);

        task.id = PersistenceManager.getLastId();

    }

    public static ArrayList<KitchenTask> loadAllTasksBySumSheetId(int id) {
        String query = "SELECT * FROM Tasks WHERE sumsheet_id = ? ORDER BY position";
        ArrayList<KitchenTask> taskArrayList = new ArrayList<>();
        ArrayList<Integer> recipeIds = new ArrayList<>();
        ArrayList<Boolean> types = new ArrayList<>();

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {

                KitchenTask t = new KitchenTask();
                t.id = rs.getInt("id");

                t.description = rs.getString("description");
                t.portions = rs.getInt("portions");
                t.ready = rs.getBoolean("ready");
                t.quantity = rs.getInt("quantity");
                recipeIds.add(rs.getInt("kitchenproc_id")); // Changed from kitchen_proc_id
                types.add(rs.getBoolean("type"));
                taskArrayList.add(t);
            }
        }, id); // Pass id as parameter

        for (int i = 0; i < recipeIds.size(); i++) {
            KitchenTask t = taskArrayList.get(i);
            if (types.get(i)) {
                t.kitchenProcess = Recipe.loadRecipe(recipeIds.get(i));
            } else {
                t.kitchenProcess = Preparation.loadPreparationById(recipeIds.get(i));
            }

        }

        return taskArrayList;
    }

    public static KitchenTask loadTaskById(int id) {
        String query = "SELECT * FROM Tasks WHERE id = ?";
        KitchenTask[] taskHolder = new KitchenTask[1]; // Use array to allow modification in lambda
        ArrayList<Integer> ids = new ArrayList<>(1);
        ArrayList<Boolean> types = new ArrayList<>(1);

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                if (taskHolder[0] != null)
                    return; // Only handle the first result

                KitchenTask t = new KitchenTask();
                t.id = rs.getInt("id");

                t.description = rs.getString("description");
                t.portions = rs.getInt("portions");
                t.ready = rs.getBoolean("ready");
                t.quantity = rs.getInt("quantity");

                t.type = rs.getBoolean("type");
                ids.add(rs.getInt("kitchenproc_id")); // Changed from kitchen_proc_id
                types.add(t.type);
                taskHolder[0] = t;
            }
        }, id); // Pass id as parameter

        if (taskHolder[0] == null) {
            return null; // No task found with the given ID
        }

        KitchenTask t = taskHolder[0];
        if (types.get(0)) {
            t.kitchenProcess = Recipe.loadRecipe(ids.get(0));
        } else {
            t.kitchenProcess = Preparation.loadPreparationById(ids.get(0));
        }

        return t;
    }

    public static void updateTaskChanged(KitchenTask task) {
        String query = "UPDATE Tasks SET description = ?, quantity = ?, portions = ?, ready = ? WHERE id = ?";

        PersistenceManager.executeUpdate(query,
                task.getDescription(),
                task.quantity,
                task.portions,
                task.ready,
                task.id);
    }

    public void setReady() {
        ready = true;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setPortions(int portions) {
        this.portions = portions;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(isReady() ? "[✓] " : "[ ] ")
                .append(getDescription());

        if (getQuantity() > 0 || getPortions() > 0) {
            sb.append(" (");
            if (getQuantity() > 0)
                sb.append("Qty: ").append(getQuantity());
            if (getQuantity() > 0 && getPortions() > 0)
                sb.append(", ");
            if (getPortions() > 0)
                sb.append("Portions: ").append(getPortions());
            sb.append(")");
        }
        return sb.toString();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public KitchenProcess getKitchenProcess() {
        return kitchenProcess;
    }

    public void setKitchenProcess(KitchenProcess kitchenProcess) {
        this.kitchenProcess = kitchenProcess;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getPortions() {
        return portions;
    }

    public boolean isReady() {
        return ready;
    }
}