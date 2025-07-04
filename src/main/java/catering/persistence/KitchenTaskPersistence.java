package catering.persistence;

import catering.businesslogic.kitchen.Assignment;
import catering.businesslogic.kitchen.KitchenTaskEventReceiver;
import catering.businesslogic.kitchen.SummarySheet;
import catering.businesslogic.kitchen.KitchenTask;

public class KitchenTaskPersistence implements KitchenTaskEventReceiver {

    @Override
    public void updateSheetGenerated(SummarySheet summarySheet) {
        SummarySheet.saveNewSumSheet(summarySheet);
    }

    @Override
    public void updateTaskAdded(SummarySheet currentSumSheet, KitchenTask added) {
        KitchenTask.saveNewTask(currentSumSheet.getId(), added, currentSumSheet.getTaskPosition(added));
    }

    @Override
    public void updateTaskListSorted(SummarySheet currentSumSheet) {
        SummarySheet.updateTaskList(currentSumSheet);
    }

    @Override
    public void updateAssignmentAdded(SummarySheet currentSumSheet, Assignment a) {
        Assignment.saveNewAssignment(currentSumSheet.getId(), a);
    }

    @Override
    public void updateAssignmentChanged(Assignment a) {
        Assignment.updateAssignment(a);
    }

    @Override
    public void updateAssignmentDeleted(Assignment ass) {
        Assignment.deleteAssignment(ass);
    }

    @Override
    public void updateTaskChanged(KitchenTask task) {
        KitchenTask.updateTaskChanged(task);
    }

}