package catering.businesslogic.event;

import java.sql.*;
import java.util.*;
import java.sql.Date;
import java.util.logging.Logger;

import catering.businesslogic.menu.Menu;
import catering.businesslogic.menu.MenuItem;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;
import catering.util.LogManager;

/**
 * Represents a service in an event in the catering system.
 */
public class Service {
    private static final Logger LOGGER = LogManager.getLogger(Service.class);

    private int id;
    private String name;
    private Date date;
    private Time timeStart;
    private Time timeEnd;
    private String location;
    private int eventId;
    private Menu menu;

    public Service() {
    }

    public Service(String name) {
        this.name = name;
    }

    // Basic getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Time getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(Time timeStart) {
        this.timeStart = timeStart;
    }

    public Time getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(Time timeEnd) {
        this.timeEnd = timeEnd;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getMenuId() {
        return (menu != null) ? menu.getId() : 0;
    }

    public Menu getMenu() {
        return menu;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    public void approveMenu() {
        if (this.menu == null)
            return;

        String query = "UPDATE Services SET approved_menu_id = ? WHERE id = ?";
        PersistenceManager.executeUpdate(query, this.menu.getId(), this.getId());
    }

    public void removeMenu() {
        this.menu = null;
    }

    public ArrayList<MenuItem> getMenuItems() {
        if (this.menu == null) {
            return new ArrayList<>();
        }
        return this.menu.getItems();
    }

    // Database operations
    public void saveNewService() {
        String query = "INSERT INTO Services (event_id, name, service_date, time_start, time_end, location) VALUES (?, ?, ?, ?, ?, ?)";

        // Convert date to timestamp for storage
        Long dateTimestamp = (this.getDate() != null) ? this.getDate().getTime() : null;

        PersistenceManager.executeUpdate(query,
                this.getEventId(),
                this.getName(),
                dateTimestamp,
                this.getTimeStart(),
                this.getTimeEnd(),
                this.getLocation());

        // Get the ID of the newly inserted service
        this.setId(PersistenceManager.getLastId());
    }

    public void updateService() {
        String query = "UPDATE Services SET name = ?, service_date = ?, time_start = ?, time_end = ?, location = ? WHERE id = ?";

        Long dateTimestamp = (this.getDate() != null) ? this.getDate().getTime() : null;

        PersistenceManager.executeUpdate(query,
                this.getName(),
                dateTimestamp,
                this.getTimeStart(),
                this.getTimeEnd(),
                this.getLocation(),
                this.getId());
    }

    public boolean deleteService() {
        String query = "DELETE FROM Services WHERE id = ?";
        return PersistenceManager.executeUpdate(query, this.getId()) > 0;
    }

    public void assignMenuToService(Menu menu) {
        this.setMenu(menu);

        String query = "UPDATE Services SET approved_menu_id = ? WHERE id = ?";
        PersistenceManager.executeUpdate(query, menu.getId(), this.getId());
    }

    public void removeMenuFromService() {
        this.removeMenu();

        String query = "UPDATE Services SET approved_menu_id = 0 WHERE id = ?";
        PersistenceManager.executeUpdate(query, this.getId());
    }

    // Static methods for data loading
    public static ArrayList<Service> loadServicesForEvent(int eventId) {
        ArrayList<Service> services = new ArrayList<>();
        String query = "SELECT * FROM Services WHERE event_id = ? ORDER BY service_date, time_start";

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Service s = new Service();
                s.id = rs.getInt("id");
                s.name = rs.getString("name");

                try {
                    s.date = Date.valueOf(rs.getString("service_date"));
                    s.timeStart = Time.valueOf(rs.getString("time_start"));
                    s.timeEnd = Time.valueOf(rs.getString("time_end"));
                } catch (IllegalArgumentException ex) {
                    // Ignore parsing errors
                }

                s.location = rs.getString("location");
                s.eventId = rs.getInt("event_id");

                int menuId = rs.getInt("approved_menu_id");
                if (menuId > 0)
                    s.menu = Menu.load(menuId);

                services.add(s);
            }
        }, eventId);

        return services;
    }

    public static Service loadById(int id) {
        String query = "SELECT * FROM Services WHERE id = ?";
        return loadServiceByQuery(query, id);
    }

    public static Service loadByName(String name) {
        String query = "SELECT * FROM Services WHERE name = ?";
        return loadServiceByQuery(query, name);
    }

    private static Service loadServiceByQuery(String query, Object param) {
        final Service[] serviceHolder = new Service[1];
        final boolean[] serviceFound = new boolean[1];
        serviceFound[0] = false;

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                serviceFound[0] = true;

                Service s = new Service();
                s.id = rs.getInt("id");
                s.name = rs.getString("name");

                try {
                    String dateStr = rs.getString("service_date");
                    String startTimeStr = rs.getString("time_start");
                    String endTimeStr = rs.getString("time_end");

                    if (dateStr != null && !dateStr.isEmpty()) {
                        s.date = Date.valueOf(dateStr);
                    }
                    if (startTimeStr != null && !startTimeStr.isEmpty()) {
                        s.timeStart = Time.valueOf(startTimeStr);
                    }
                    if (endTimeStr != null && !endTimeStr.isEmpty()) {
                        s.timeEnd = Time.valueOf(endTimeStr);
                    }
                } catch (IllegalArgumentException ex) {
                    LOGGER.warning("Error parsing date/time in service: " + s.name);
                }

                s.location = rs.getString("location");
                s.eventId = rs.getInt("event_id");

                int menuId = rs.getInt("approved_menu_id");
                if (menuId > 0) {
                    try {
                        s.menu = Menu.load(menuId);
                    } catch (Exception e) {
                        LOGGER.warning("Failed to load menu (id: " + menuId + ") for service: " + s.name);
                    }
                }

                serviceHolder[0] = s;
            }
        }, param);

        return serviceFound[0] ? serviceHolder[0] : null;
    }

    @Override
    public String toString() {
        return "Service [id=" + id + ", name=" + name + ", date=" + date + ", location=" + location +
                ", menu=" + (menu != null ? menu.getTitle() : "none") + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Service other = (Service) obj;

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

        // If dates don't match, sections are not equal
        boolean dateMatch = (this.date == null && other.date == null) ||
        (this.date != null && this.date.equals(other.date));

        if (!dateMatch)
            return false;

        // If times don't match, sections are not equal
        boolean timeStartMatch = (this.timeStart == null && other.timeStart == null) ||
        (this.timeStart != null && this.timeStart.equals(other.timeStart));

        if (!timeStartMatch)
            return false;
        
        boolean timeEndMatch = (this.timeEnd == null && other.timeEnd == null) ||
            (this.timeEnd != null && this.timeEnd.equals(other.timeEnd));
    
        if (!timeEndMatch)
            return false;        

        // If locations don't match, sections are not equal
        boolean locationMatch = (this.location == null && other.location == null) ||
        (this.location != null && this.location.equals(other.location));

        if (!locationMatch)
            return false;

        // If locations don't match, sections are not equal
        boolean menuMatch = (this.menu == null && other.menu == null) ||
        (this.menu != null && this.menu.equals(other.menu));

        if (!menuMatch)
            return false;

        // If events don't match, sections are not equal
        if (this.eventId > 0 && other.eventId > 0) {
            return this.eventId == other.eventId;
        }

        return true;
    }
}
