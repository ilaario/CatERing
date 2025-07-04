package catering.businesslogic.kitchen;

import java.util.ArrayList;

import catering.businesslogic.CatERing;
import catering.businesslogic.UseCaseLogicException;
import catering.businesslogic.event.Event;
import catering.businesslogic.event.Service;
import catering.businesslogic.shift.Shift;
import catering.businesslogic.user.User;

public class KitchenTaskManager {

    private SummarySheet currentSumSheet;
    private ArrayList<KitchenTaskEventReceiver> eventReceivers;

    public KitchenTaskManager() {
        eventReceivers = new ArrayList<>();
    }

    public void addEventReceiver(KitchenTaskEventReceiver rec) {
        this.eventReceivers.add(rec);
    }

    public SummarySheet generateSummarySheet(Event event, Service service) throws UseCaseLogicException {

        User user = CatERing.getInstance().getUserManager().getCurrentUser();

        if (!user.isChef())
            throw new UseCaseLogicException("User is not a chef");

        if (event == null)
            throw new UseCaseLogicException("Event not specified");

        if (service == null)
            throw new UseCaseLogicException("Service not specified");

        if (!event.containsService(service))
            throw new UseCaseLogicException("Event does not include service");

        if (!user.equals(event.getChef()))
            throw new UseCaseLogicException("User not assigned chef");

        if (service.getMenu() == null)
            throw new UseCaseLogicException("Service lacks menu");

        SummarySheet newSummarySheet = new SummarySheet(service, user);

        this.setCurrentSumSheet(newSummarySheet);
        this.notifySheetGenerated(newSummarySheet);

        return newSummarySheet;
    }

    public ArrayList<SummarySheet> loadAllSumSheets() {
        return SummarySheet.loadAllSumSheets();
    }

    public SummarySheet openSumSheet(SummarySheet ss) throws UseCaseLogicException, SummarySheetException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (!user.isChef())
            throw new UseCaseLogicException();
        if (!ss.isOwner(user))
            throw new SummarySheetException("User: " + user.getUserName() + " is not owner of the SummarySheet");
        setCurrentSumSheet(ss);
        return ss;
    }

    public void addKitchenTask(KitchenTask t) {
        KitchenTask added = currentSumSheet.addTask(t);
        notifyTaskAdded(added);
    }

    public void moveTask(KitchenTask t, int pos) throws UseCaseLogicException {
        if (currentSumSheet == null || currentSumSheet.getTaskPosition(t) < 0)
            throw new UseCaseLogicException();
        if (pos < 0 || pos >= currentSumSheet.getTaskListSize())
            throw new IllegalArgumentException();
        this.currentSumSheet.moveTask(t, pos);

        this.notifyTaskListSorted();
    }

    public void addTaskInformation(KitchenTask task, int quantity, int portions, long minutes)
            throws SummarySheetException, UseCaseLogicException {
        if (currentSumSheet == null)
            throw new UseCaseLogicException();
        if (currentSumSheet.getTaskPosition(task) < 0)
            throw new SummarySheetException("Task not found in this SummarySheet");
        if (quantity < 0)
            throw new IllegalArgumentException("Quantity must be >= 0");
        if (portions < 0)
            throw new IllegalArgumentException("Portions must be >= 0");
        if (minutes < 0)
            throw new IllegalArgumentException("Minutes must be >= 0");

        KitchenTask t = currentSumSheet.addTaskInformation(task, quantity, portions, minutes);

        notifyTaskChanged(t);
    }

    public Assignment assignTask(KitchenTask t, Shift s) throws UseCaseLogicException {
        return assignTask(t, s, null);
    }

    public Assignment assignTask(KitchenTask t, Shift s, User cook) throws UseCaseLogicException {
        if (currentSumSheet == null) {
            throw new UseCaseLogicException("Cannot assign task because there is no active summary sheet.");
        }
        if (cook != null && !CatERing.getInstance().getShiftManager().isAvailable(cook, s)) {
            throw new UseCaseLogicException("Cook " + cook.getUserName() + " is not available for the selected shift.");
        }
        Assignment a = currentSumSheet.addAssignment(t, s, cook);
        this.notifyAssignmentAdded(a);

        return a;
    }

    public void modifyAssignment(Assignment ass) throws UseCaseLogicException, SummarySheetException {
        Shift shift = ass.getShift();
        modifyAssignment(ass, shift, null);
    }

    public void modifyAssignment(Assignment ass, User cook) throws UseCaseLogicException, SummarySheetException {
        Shift shift = ass.getShift();
        modifyAssignment(ass, shift, cook);
    }

    public void modifyAssignment(Assignment ass, Shift shift) throws UseCaseLogicException, SummarySheetException {
        modifyAssignment(ass, shift, null);
    }

    public void modifyAssignment(Assignment ass, Shift shift, User cook)
            throws UseCaseLogicException, SummarySheetException {
        Assignment a;

        if (currentSumSheet == null)
            throw new UseCaseLogicException();
        if (cook == null || CatERing.getInstance().getShiftManager().isAvailable(cook, shift))
            a = currentSumSheet.modifyAssignment(ass, shift, cook);
        else
            throw new UseCaseLogicException();

        notifyAssignmentChanged(a);
    }

    /**
     * Gets the current summary sheet
     *
     * @return The current summary sheet
     */
    public SummarySheet getCurrentSummarySheet() {
        return currentSumSheet;
    }

    public void setTaskReady(KitchenTask t) throws UseCaseLogicException {
        KitchenTask task = currentSumSheet.setTaskReady(t);
        notifyTaskChanged(task);
    }

    public void deleteAssignment(Assignment a) throws UseCaseLogicException {
        Assignment ass = currentSumSheet.deleteAssignment(a);
        notifyAssignmentDeleted(ass);
    }

    private void setCurrentSumSheet(SummarySheet summarySheet) {
        currentSumSheet = summarySheet;
    }

    private void notifyTaskChanged(KitchenTask task) {
        for (KitchenTaskEventReceiver er : eventReceivers) {
            er.updateTaskChanged(task);
        }
    }

    private void notifyAssignmentDeleted(Assignment ass) {
        for (KitchenTaskEventReceiver er : eventReceivers) {
            er.updateAssignmentDeleted(ass);
        }
    }

    private void notifyAssignmentChanged(Assignment a) {
        for (KitchenTaskEventReceiver er : eventReceivers) {
            er.updateAssignmentChanged(a);
        }
    }

    /**
     * Notifies all event receivers about a new assignment
     * 
     * @param assignment The assignment that was added
     */
    private void notifyAssignmentAdded(Assignment assignment) {
        for (KitchenTaskEventReceiver er : eventReceivers) {
            er.updateAssignmentAdded(currentSumSheet, assignment);
        }
    }

    private void notifyTaskListSorted() {
        for (KitchenTaskEventReceiver er : eventReceivers) {
            er.updateTaskListSorted(currentSumSheet);
        }
    }

    private void notifyTaskAdded(KitchenTask added) {
        for (KitchenTaskEventReceiver er : eventReceivers) {
            er.updateTaskAdded(currentSumSheet, added);
        }
    }

    private void notifySheetGenerated(SummarySheet summarySheet) {
        for (KitchenTaskEventReceiver er : eventReceivers) {
            er.updateSheetGenerated(summarySheet);
        }
    }

}
