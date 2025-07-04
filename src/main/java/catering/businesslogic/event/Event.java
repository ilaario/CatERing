package catering.businesslogic.event;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

import catering.businesslogic.user.User;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;
import catering.util.DateUtils;
import catering.util.LogManager;

/**
 * Represents an event in the catering system.
 */
public class Event {
    private static final Logger LOGGER = LogManager.getLogger(Event.class);

    private int id;
    private String name;
    private Date dateStart;
    private Date dateEnd;
    private User chef;
    private ArrayList<Service> services;

    public Event() {
        services = new ArrayList<>();
    }

    public Event(String name) {
        this();
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

    public Date getDateStart() {
        return dateStart;
    }

    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
    }

    public Date getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    public User getChef() {
        return chef;
    }

    public int getChefId() {
        return chef != null ? chef.getId() : 0;
    }

    public void setChef(User chef) {
        this.chef = chef;
    }

    public void setChefId(int chefId) {
        this.chef = User.load(chefId);
    }

    public ArrayList<Service> getServices() {
        return services;
    }

    public void setServices(ArrayList<Service> services) {
        this.services = services;
    }

    // Service management
    public void addService(Service service) {
        if (services == null) {
            services = new ArrayList<>();
        }
        services.add(service);
    }

    public void removeService(Service service) {
        if (services != null) {
            services.remove(service);
        }
    }

    public boolean containsService(Service service) {
        if (services != null) {
            return services.contains(service);
        }
        return false;
    }

    // Database operations
    public void saveNewEvent() {
        String query = "INSERT INTO Events (name, date_start, date_end, chef_id) VALUES (?, ?, ?, ?)";

        Long startTimestamp = (dateStart != null) ? dateStart.getTime() : null;
        Long endTimestamp = (dateEnd != null) ? dateEnd.getTime() : null;

        PersistenceManager.executeUpdate(query, name, startTimestamp, endTimestamp, getChefId());

        // Get the ID of the newly inserted event
        id = PersistenceManager.getLastId();

        LOGGER.info("Saved event: " + name + " (ID: " + id + ")");
    }

    public void updateEvent() {
        String query = "UPDATE Events SET name = ?, date_start = ?, date_end = ?, chef_id = ? WHERE id = ?";

        Long startTimestamp = (dateStart != null) ? dateStart.getTime() : null;
        Long endTimestamp = (dateEnd != null) ? dateEnd.getTime() : null;

        PersistenceManager.executeUpdate(query, name, startTimestamp, endTimestamp, getChefId(), id);

        LOGGER.info("Updated event: " + name + " (ID: " + id + ")");
    }

    public boolean deleteEvent() {
        // Delete all services first
        for (Service service : services) {
            service.deleteService();
        }
        services.clear();

        // Delete the event
        String query = "DELETE FROM Events WHERE id = ?";
        boolean success = PersistenceManager.executeUpdate(query, id) > 0;

        if (success) {
            LOGGER.info("Deleted event: " + name + " (ID: " + id + ")");
        }

        return success;
    }

    // Static load methods
    public static ArrayList<Event> loadAllEvents() {
        ArrayList<Event> events = new ArrayList<>();
        String query = "SELECT * FROM Events ORDER BY date_start DESC";

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Event e = new Event();
                e.id = rs.getInt("id");
                e.name = rs.getString("name");
                e.dateStart = DateUtils.getDateFromResultSet(rs, "date_start");
                e.dateEnd = DateUtils.getDateFromResultSet(rs, "date_end");
                e.chef = User.load(rs.getInt("chef_id"));
                events.add(e);
            }
        });

        // Load services for each event
        for (Event e : events) {
            e.services = Service.loadServicesForEvent(e.id);
        }

        return events;
    }

    public static Event loadById(int id) {
        String query = "SELECT * FROM Events WHERE id = ?";
        return loadEventByQuery(query, id);
    }

    public static Event loadByName(String name) {
        String query = "SELECT * FROM Events WHERE name = ?";
        return loadEventByQuery(query, name);
    }

    private static Event loadEventByQuery(String query, Object param) {
        final Event[] eventHolder = new Event[1];
        final boolean[] eventFound = new boolean[1];

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                eventFound[0] = true;

                Event e = new Event();
                e.id = rs.getInt("id");
                e.name = rs.getString("name");
                e.dateStart = DateUtils.getDateFromResultSet(rs, "date_start");
                e.dateEnd = DateUtils.getDateFromResultSet(rs, "date_end");

                try {
                    e.chef = User.load(rs.getInt("chef_id"));
                } catch (Exception ex) {
                    e.chef = null;
                }

                eventHolder[0] = e;
            }
        }, param);

        if (!eventFound[0]) {
            return null;
        }

        Event result = eventHolder[0];
        if (result != null) {
            try {
                result.services = Service.loadServicesForEvent(result.id);
            } catch (Exception ex) {
                result.services = new ArrayList<>();
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return "Event [id=" + id + ", name=" + name + ", dateStart=" + dateStart +
                ", services=" + (services != null ? services.size() : 0) + "]";
    }
}