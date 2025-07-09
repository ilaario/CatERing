package catering.businesslogic.vacation;

import java.util.Date;

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
        // TODO: implement this method
        return true;
    }

    public boolean rejectVacation(){
        // TODO: implement this method
        return true;
    }
}
