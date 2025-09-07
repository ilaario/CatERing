package catering.businesslogic.vacation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import catering.businesslogic.shift.Shift;
import catering.businesslogic.user.User;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

public class Vacation {
    private final int id;
    private final Date dateStart;
    private final Date dateEnd;
    private final boolean approved;

    public Vacation(int id, Date dateStart, Date dateEnd, boolean approved) {
        this.id = id;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.approved = approved;
    }

    public int getId() {
        return id;
    }

    public Date getDateStart() {
        return dateStart;
    }

    public Date getDateEnd() {
        return dateEnd;
    }

    public boolean isApproved() {
        return approved;
    }

    public boolean approveVacation(){
        String query = "UPDATE Vacation SET approved = ? WHERE id = ?";
        PersistenceManager.executeUpdate(query, true, this.id);

        return true;
    }

    public boolean rejectVacation(){
        String deleteVacationQuery = "DELETE FROM Vacation WHERE id = ?";
        PersistenceManager.executeUpdate(deleteVacationQuery, this.id);

        return true;
    }

    public static ArrayList<Vacation> getPendingVacation() {
        ArrayList<Vacation> vacationList = new ArrayList<>();

        String vacationRequests = "SELECT * FROM Vacation WHERE approved = ?";
        PersistenceManager.executeQuery(vacationRequests, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                while (rs.next()) {
                    Vacation tmp = new Vacation(
                        rs.getInt("id"), 
                        rs.getDate("dateStart"), 
                        rs.getDate("dateEnd"), 
                        rs.getBoolean("approved")
                    );

                    vacationList.add(tmp);
                }
            }
        }, false);

        return vacationList;
    }

        /**
     * Post a vacation request, if the conditions are valid
     *
     * @param id - User
     * @param dateStart - Date
     * @param dateEnd - Date
     * @return True if vacation request posted (pending for approval), False if conditions not valid
     */
    public static Vacation requestVacation(int id, long daysLeft, Date dateStart, Date dateEnd) {
        if (daysLeft <= 0 || daysLeft <= dateEnd.getTime() - dateStart.getTime()) {
            return null;
        }

        ArrayList<Shift> shifts = new ArrayList<>();

        String userQuery =
                "SELECT *" +
                "FROM Shifts s JOIN ShiftBookings b ON s.id =  b.shift_id " +
                "WHERE b.user_id = ?";

        PersistenceManager.executeQuery(userQuery, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                while (rs.next()) {
                    // 3) Crea un nuovo oggetto Shift per ogni riga
                    Shift shift = new Shift(rs.getDate("date"), rs.getTime("start_time"), rs.getTime("end_time"));
                    shifts.add(shift);
                }
            }
        }, id); // Pass uid as parameter


        if(!shifts.isEmpty()){
            for (Shift shift : shifts) {
                if(shift.getDate().getTime() >= dateStart.getTime() && shift.getDate().getTime() <= dateEnd.getTime()) {
                    return null;
                }
            }
        }

        Vacation requestedVacation = new Vacation(id, dateStart, dateEnd, false);

        String query = "INSERT INTO Vacation (user_id, dateStart, dateEnd, approved) VALUES(?,?,?,?)";

        PersistenceManager.executeUpdate(
            query, requestedVacation.id, 
            requestedVacation.dateStart.getTime(), 
            requestedVacation.dateEnd.getTime(), 
            requestedVacation.approved);

        return requestedVacation;
    }

    public static ArrayList<Vacation> getVacationTable() {
        ArrayList<Vacation> vacationList = new ArrayList<>();

        String vacationRequests = "SELECT * FROM Vacation";
        PersistenceManager.executeQuery(vacationRequests, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                while (rs.next()) {
                    Vacation tmp = new Vacation(
                        rs.getInt("id"), 
                        rs.getDate("dateStart"), 
                        rs.getDate("dateEnd"), 
                        rs.getBoolean("approved")
                    );

                    vacationList.add(tmp);
                }
            }
        });

        return vacationList;
    }

    /**
     * Determines if this user is equal to another object.
     * Two users are considered equal if they have the same ID or, if ID is 0,
     * the same username.
     *
     * @param obj The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Vacation other = (Vacation) obj;

        // If both users have valid IDs, compare by ID
        if (this.id > 0 && other.id > 0) {
            return this.id == other.id;
        }

        // Otherwise, if either ID is 0, compare by username
        return this.id != 0 
            && this.dateStart.equals(other.dateStart) 
            && this.dateEnd.equals(other.dateEnd) 
            && this.isApproved() == other.isApproved();
    }

    private static int spread(int h) {
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    /**
     * Generates a hash code for this user.
     * The hash code is based on ID if it's valid (> 0), or username otherwise.
     *
     * @return A hash code value for this user
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        if (id > 0) {
            result = prime * result + id;
        } else {
            result = prime * result + (dateStart != null ? dateStart.hashCode() : 0) + (dateEnd != null ? dateEnd.hashCode() : 0);
        }

        return spread(result);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ID = " + id + " - StartDate = " + dateStart + " - EndDate = " + dateEnd + " - Approved = " + approved);

        return sb.toString();
    }
}
