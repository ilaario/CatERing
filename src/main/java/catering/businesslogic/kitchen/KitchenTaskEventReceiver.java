package catering.businesslogic.kitchen;

public interface KitchenTaskEventReceiver {

    void updateSheetGenerated(SummarySheet summarySheet);

    void updateTaskAdded(SummarySheet currentSumSheet, KitchenTask added);

    void updateTaskListSorted(SummarySheet currentSumSheet);

    void updateAssignmentAdded(SummarySheet currentSumSheet, Assignment a);

    void updateAssignmentChanged(Assignment a);

    void updateAssignmentDeleted(Assignment ass);

    void updateTaskChanged(KitchenTask task);

}