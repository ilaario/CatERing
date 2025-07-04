package catering.businesslogic.kitchen;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import catering.businesslogic.UseCaseLogicException;
import catering.businesslogic.event.Service;
import catering.businesslogic.shift.Shift;
import catering.businesslogic.user.User;
import catering.persistence.BatchUpdateHandler;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

public class SummarySheet {

    /**
     * Loads all summary sheets from the database
     * 
     * @return List of all summary sheets
     */
    public static ArrayList<SummarySheet> loadAllSumSheets() {
        return loadSummarySheets("SELECT * FROM SummarySheets");
    }

    /**
     * Loads a specific summary sheet by ID
     * 
     * @param id The ID of the summary sheet to load
     * @return The loaded summary sheet, or null if not found
     */
    public static SummarySheet loadSummarySheetById(int id) {
        ArrayList<SummarySheet> results = loadSummarySheets("SELECT * FROM SummarySheets WHERE id = ?", id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Loads summary sheets for a specific service
     * 
     * @param serviceId The ID of the service
     * @return List of summary sheets for the service
     */
    public static ArrayList<SummarySheet> loadSummarySheetsByServiceId(int serviceId) {
        return loadSummarySheets("SELECT * FROM SummarySheets WHERE service_id = ?", serviceId);
    }

    public static void updateTaskList(SummarySheet ss) {
        String upd = "UPDATE Tasks SET position = ? WHERE id = ?";
        PersistenceManager.executeBatchUpdate(upd, ss.taskList.size(), new BatchUpdateHandler() {
            @Override
            public void handleBatchItem(PreparedStatement ps, int batchCount) throws SQLException {
                ps.setInt(1, batchCount);
                ps.setInt(2, ss.taskList.get(batchCount).getId());
            }

            @Override
            public void handleGeneratedIds(ResultSet rs, int count) throws SQLException {
                // no generated ids to handle
            }
        });
    }

    public static void saveNewSumSheet(SummarySheet s) {
        String sumSheetInsert = "INSERT INTO SummarySheets (service_id, owner_id) VALUES (?, ?);";
        int[] result = PersistenceManager.executeBatchUpdate(sumSheetInsert, 1, new BatchUpdateHandler() {
            @Override
            public void handleBatchItem(PreparedStatement ps, int batchCount) throws SQLException {
                ps.setInt(1, s.service.getId());
                ps.setInt(2, s.owner.getId());
            }

            @Override
            public void handleGeneratedIds(ResultSet rs, int count) throws SQLException {
                // should be only one
                if (count == 0) {
                    s.id = rs.getInt(1);
                }
            }
        });

        if (result[0] > 0) {
            if (!s.assignmentList.isEmpty()) {
                Assignment.saveAllNewAssignment(s.id, s.assignmentList);
            }

            if (!s.taskList.isEmpty()) {
                KitchenTask.saveAllNewTasks(s.id, s.taskList);
            }
        }
    }

    /**
     * Helper method to handle result set and create SummarySheet objects
     * 
     * @param query  The SQL query to execute
     * @param params Query parameters (optional)
     * @return List of SummarySheet objects
     */
    private static ArrayList<SummarySheet> loadSummarySheets(String query, Object... params) {
        ArrayList<SummarySheet> summarySheets = new ArrayList<>();
        ArrayList<Integer> serviceIds = new ArrayList<>();
        ArrayList<Integer> ownerIds = new ArrayList<>();

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                SummarySheet s = new SummarySheet();
                s.id = rs.getInt("id");

                summarySheets.add(s);
                serviceIds.add(rs.getInt("service_id"));
                ownerIds.add(rs.getInt("owner_id"));
            }
        }, params);

        // Load services, owners, tasks, and assignments for each sheet
        for (int i = 0; i < summarySheets.size(); i++) {
            SummarySheet s = summarySheets.get(i);
            s.service = Service.loadById(serviceIds.get(i));
            s.owner = User.load(ownerIds.get(i));
            s.taskList = KitchenTask.loadAllTasksBySumSheetId(s.id);
            s.assignmentList = Assignment.loadAllAssignmentsBySumSheetId(s.id);
        }

        return summarySheets;
    }

    private int id;

    private Service service;

    private User owner;

    private ArrayList<KitchenTask> taskList;

    private ArrayList<Assignment> assignmentList;

    public SummarySheet(Service service, User user) {

        this.service = service;
        this.owner = user;
        taskList = new ArrayList<KitchenTask>();
        assignmentList = new ArrayList<>();

        service.getMenu().getKitchenProcesses()
                .forEach(kitchenProcess -> taskList.add(new KitchenTask(kitchenProcess, kitchenProcess.getName())));
    }

    private SummarySheet() {
    }

    public int getTaskPosition(KitchenTask t) {
        return taskList.indexOf(t);
    }

    public KitchenTask addTask(KitchenTask t) {
        this.taskList.add(t);
        return t;
    }

    public int getId() {
        return id;
    }

    public int getTaskListSize() {
        return taskList.size();
    }

    public void moveTask(KitchenTask t, int pos) {
        taskList.remove(t);
        taskList.add(pos, t);
    }

    public ArrayList<KitchenTask> getTaskList() {
        return taskList;
    }

    public Assignment addAssignment(KitchenTask t, Shift s, User cook) {
        Assignment ass = new Assignment(t, s, cook);
        assignmentList.add(ass);
        return ass;
    }

    public boolean isOwner(User user) {
        return user.equals(this.owner);
    }

    public Assignment modifyAssignment(Assignment ass, Shift shift, User cook) throws SummarySheetException {
        if (!assignmentList.contains(ass))
            throw new SummarySheetException("Invalid Assignment");
        ass.setShift(shift);
        ass.setCook(cook);
        return ass;
    }

    public ArrayList<Assignment> getAssignments() {
        return assignmentList;
    }

    public Assignment deleteAssignment(Assignment a) throws UseCaseLogicException {
        if (!assignmentList.contains(a))
            throw new UseCaseLogicException();
        return assignmentList.remove(assignmentList.indexOf(a));
    }

    public KitchenTask setTaskReady(KitchenTask t) throws UseCaseLogicException {
        if (!taskList.contains(t))
            throw new UseCaseLogicException();
        t.setReady();
        return t;
    }

    public KitchenTask addTaskInformation(KitchenTask task, int quantity, int portions, long minutes) {
        task.setQuantity(quantity);
        task.setPortions(portions);

        return task;
    }

    /**
     * Get the owner of this summary sheet
     * 
     * @return The User who owns this summary sheet
     */
    public User getOwner() {
        return owner;
    }

    /**
     * Returns a string representation of this summary sheet for testing purposes.
     * 
     * @return A string containing the key information of this summary sheet
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\n\nSummarySheet [ID: ")
                .append(id)
                .append(", Owner: ").append(owner != null ? owner.getUserName() : "none");

        // Service info
        if (service != null) {
            sb.append(", Service: ").append(service.getName());
        } else {
            sb.append(", No service");
        }

        // Collection counts
        sb.append(", Tasks: ").append(taskList != null ? taskList.size() : 0)
                .append(", Assignments: ").append(assignmentList != null ? assignmentList.size() : 0);

        // Close header section or prepare for details
        if ((taskList == null || taskList.isEmpty()) &&
                (assignmentList == null || assignmentList.isEmpty())) {
            sb.append("]");
        } else {
            sb.append("]");

            // Task details - using Task.toString()
            if (taskList != null && !taskList.isEmpty()) {
                sb.append("\n\nTasks:");
                int count = 1;
                for (KitchenTask task : taskList) {
                    sb.append("\n  ").append(count++).append(". ")
                            .append(task.toString());
                }
            }

            // Assignment details - using Assignment.toString()
            if (assignmentList != null && !assignmentList.isEmpty()) {
                sb.append("\n\nAssignments:");
                int count = 1;
                for (Assignment ass : assignmentList) {
                    sb.append("\n  ").append(count++).append(". ")
                            .append(ass.toString());
                }
            }
        }

        return sb.toString();
    }
}
