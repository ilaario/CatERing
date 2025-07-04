package catering.businesslogic.kitchen;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import catering.businesslogic.shift.Shift;
import catering.businesslogic.user.User;
import catering.persistence.BatchUpdateHandler;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

/**
 * Represents a task assignment to a shift and optionally a cook
 */
public class Assignment {

    private int id;
    private Shift shift;
    private KitchenTask task;
    private User cook;

    // Constructors
    public Assignment(KitchenTask task, Shift shift, User cook) {
        this.task = task;
        this.shift = shift;
        this.cook = cook;
    }

    public Assignment(KitchenTask task, Shift shift) {
        this.task = task;
        this.shift = shift;
        this.cook = null;
    }

    Assignment() {
    }

    // Public accessors and mutators
    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public User getCook() {
        return cook;
    }

    public void setCook(User cook) {
        this.cook = cook;
    }

    /**
     * Get the task associated with this assignment
     * 
     * @return The Task object
     */
    public KitchenTask getTask() {
        return task;
    }

    /**
     * Get the ID of this assignment
     * 
     * @return The assignment ID
     */
    public int getId() {
        return id;
    }

    // Database-related code below this point

    /**
     * Loads all assignments for a specific summary sheet
     * 
     * @param id The summary sheet ID
     * @return List of assignments for the summary sheet
     */
    public static ArrayList<Assignment> loadAllAssignmentsBySumSheetId(int id) {
        String query = "SELECT * FROM Assignment WHERE sumsheet_id = ?";
        ArrayList<Assignment> assignments = new ArrayList<>();
        ArrayList<Integer> shiftIds = new ArrayList<>();
        ArrayList<Integer> taskIds = new ArrayList<>();
        ArrayList<Integer> cookIds = new ArrayList<>();

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                // Create a new Assignment object for each row
                Assignment a = new Assignment();
                a.id = rs.getInt("id");

                assignments.add(a);

                shiftIds.add(rs.getInt("shift_id"));
                taskIds.add(rs.getInt("task_id"));
                cookIds.add(rs.getInt("cook_id"));
            }
        }, id); // Pass id as parameter

        for (int i = 0; i < shiftIds.size(); i++) {
            Assignment a = assignments.get(i);
            a.cook = User.load(cookIds.get(i));
            a.task = KitchenTask.loadTaskById(taskIds.get(i));
            a.shift = Shift.loadItemById(shiftIds.get(i));

        }

        return assignments;
    }

    /**
     * Updates an existing assignment in the database
     * 
     * @param a The assignment to update
     */
    public static void updateAssignment(Assignment a) {
        String upd = "UPDATE Assignment SET shift_id = ?, cook_id = ? WHERE id = ?";
        PersistenceManager.executeUpdate(upd,
                a.shift.getId(),
                (a.cook == null ? 0 : a.cook.getId()),
                a.id);
    }

    /**
     * Deletes an assignment from the database
     * 
     * @param a The assignment to delete
     */
    public static void deleteAssignment(Assignment a) {
        String query = "DELETE FROM Assignment WHERE id = ?";
        PersistenceManager.executeUpdate(query, a.id);

    }

    /**
     * Saves a batch of new assignments for a summary sheet
     * 
     * @param id             The summary sheet ID
     * @param assignmentList The list of assignments to save
     */
    public static void saveAllNewAssignment(int id, ArrayList<Assignment> assignmentList) {
        String secInsert = "INSERT INTO Assignment (sumsheet_id, shift_id, task_id, cook_id) VALUES (?, ?, ?, ?);";
        PersistenceManager.executeBatchUpdate(secInsert, assignmentList.size(), new BatchUpdateHandler() {
            @Override
            public void handleBatchItem(PreparedStatement ps, int batchCount) throws SQLException {
                ps.setInt(1, id);
                ps.setInt(2, assignmentList.get(batchCount).shift.getId());
                ps.setInt(3, assignmentList.get(batchCount).task.getId());
                ps.setInt(4, assignmentList.get(batchCount).cook.getId());
            }

            @Override
            public void handleGeneratedIds(ResultSet rs, int count) throws SQLException {
                assignmentList.get(count).id = rs.getInt(1);

            }
        });
    }

    /**
     * Saves a single new assignment for a summary sheet
     * 
     * @param id The summary sheet ID
     * @param a  The assignment to save
     */
    public static void saveNewAssignment(int id, Assignment a) {
        String query = "INSERT INTO Assignment (sumsheet_id, shift_id, task_id, cook_id) VALUES (?, ?, ?, ?)";
        PersistenceManager.executeUpdate(query,
                id,
                a.shift.getId(),
                a.task.getId(),
                (a.cook == null ? 0 : a.cook.getId()));
        a.id = PersistenceManager.getLastId();

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Task: ").append(getTask() != null ? getTask().getDescription() : "none");
        sb.append(", Cook: ").append(getCook() != null ? getCook().getUserName() : "unassigned");

        Shift shift = getShift();
        if (shift != null) {
            sb.append(", Shift: ").append(shift.getDate())
                    .append(" (").append(shift.getStartTime())
                    .append("-").append(shift.getEndTime()).append(")");
        }
        return sb.toString();
    }
}
