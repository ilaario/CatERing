package catering.businesslogic.event;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import catering.businesslogic.UseCaseLogicException;
import catering.businesslogic.menu.Menu;
import catering.businesslogic.user.User;
import catering.util.LogManager;

/**
 * EventManager handles all operations related to events and services in the
 * CatERing system.
 * It manages event creation, modification, and deletion, as well as service
 * management and menu assignments for services.
 */
public class EventManager {

    private static final Logger LOGGER = LogManager.getLogger(EventManager.class);

    private ArrayList<EventReceiver> eventReceivers;
    private Event selectedEvent;
    private Service currentService;

    /**
     * Constructor initializes the event receivers list
     */
    public EventManager() {
        eventReceivers = new ArrayList<>();
    }

    /**
     * Adds an event receiver to be notified of events changes
     * 
     * @param receiver The event receiver to add
     */
    public void addEventReceiver(EventReceiver receiver) {
        if (receiver != null && !eventReceivers.contains(receiver)) {
            eventReceivers.add(receiver);
        }
    }

    /**
     * Removes an event receiver
     * 
     * @param receiver The event receiver to remove
     */
    public void removeEventReceiver(EventReceiver receiver) {
        eventReceivers.remove(receiver);
    }

    /**
     * Gets all events in the system
     * 
     * @return List of all events
     */
    public ArrayList<Event> getEvents() {
        return Event.loadAllEvents();
    }

    /**
     * Sets the current service based on service ID
     * 
     * @param serviceId ID of the service to select
     */
    public void setSelectedServiceIndex(int serviceId) {
        if (selectedEvent != null && selectedEvent.getServices() != null) {
            for (Service si : selectedEvent.getServices()) {
                if (si.getId() == serviceId) {
                    currentService = si;
                    return;
                }
            }
        }
        // If service not found, currentService remains unchanged
    }

    /**
     * Sets the current service directly
     * 
     * @param service Service to set as current
     */
    public void setCurrentService(Service service) {
        this.currentService = service;
    }

    /**
     * Gets the current service
     * 
     * @return Current service or null if none selected
     */
    public Service getCurrentService() {
        return this.currentService;
    }

    /**
     * Gets the selected event
     * 
     * @return Selected event or null if none selected
     */
    public Event getSelectedEvent() {
        return selectedEvent;
    }

    /**
     * Sets the selected event
     * 
     * @param event Event to select
     */
    public void setSelectedEvent(Event event) {
        this.selectedEvent = event;
    }

    /**
     * Creates a new event with the given details
     * 
     * @param name      Event name
     * @param dateStart Start date
     * @param dateEnd   End date (can be null)
     * @param organizer User organizing the event
     * @return The newly created event
     */
    public Event createEvent(String name, Date dateStart, Date dateEnd, User chef) {
        try {
            LOGGER.info("Creating new event '" + name + "' with chef " + chef.getUserName());

            Event event = new Event();
            event.setName(name);
            event.setDateStart(dateStart);
            event.setDateEnd(dateEnd);
            event.setChef(chef);

            // Save to database
            event.saveNewEvent();

            // Set as selected event
            this.selectedEvent = event;
            this.currentService = null;

            // Notify all receivers
            notifyEventCreated(event);

            return event;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create event '" + name + "'", e);
            return null;
        }
    }

    public void selectEvent(Event event) {
        LOGGER.info("Selecting event '" + event.getName() + "' (ID: " + event.getId() + ")");
        this.selectedEvent = event;
        this.currentService = null;
    }

    public Service createService(String name, Date date, Time timeStart, Time timeEnd, String location)
            throws UseCaseLogicException {
        if (selectedEvent == null) {
            String msg = "Cannot create service: no event selected";
            LOGGER.warning(msg);
            throw new UseCaseLogicException(msg);
        }

        try {
            LOGGER.info("Creating new service '" + name + "' for event '" + selectedEvent.getName() + "'");

            Service service = new Service();
            service.setName(name);
            service.setDate(date);
            service.setTimeStart(timeStart);
            service.setTimeEnd(timeEnd);
            service.setLocation(location);
            service.setEventId(selectedEvent.getId());

            // Save to database
            service.saveNewService();

            // Add to event and set as current service
            selectedEvent.addService(service);
            this.currentService = service;

            // Notify all receivers
            notifyServiceCreated(service);

            return service;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create service '" + name + "'", e);
            return null;
        }
    }

    /**
     * Modifies an existing event
     * 
     * @param eventId ID of the event to modify
     * @param name    New name for the event
     * @param date    New date for the event
     */
    public void modifyEvent(int eventId, String name, Date date) {
        Event event = Event.loadById(eventId);
        if (event != null) {
            event.setName(name);
            event.setDateStart(date);

            // Notify all receivers
            notifyEventModified(event);

            // Update selected event if it's the same one
            if (selectedEvent != null && selectedEvent.getId() == eventId) {
                this.selectedEvent = event;
            }
        }
    }

    /**
     * Modifies a service
     * 
     * @param serviceId ID of the service to modify
     * @param name      New name for the service
     * @param date      New date for the service
     * @param location  New location for the service
     * @param menuId    ID of the menu to assign (0 for no menu)
     * @return The modified service, or null if not found
     */
    public Service modifyService(int serviceId, String name, Date date, String location, int menuId) {
        // First try to find service in the current event's services list
        Service service = findServiceById(serviceId);

        if (service != null) {
            // Update service properties
            service.setName(name);
            service.setDate(date);
            service.setLocation(location);

            // Handle menu assignment if needed
            if (menuId > 0 && (service.getMenuId() == 0 || service.getMenuId() != menuId)) {
                try {
                    Menu menu = Menu.load(menuId);
                    if (menu != null) {
                        service.setMenu(menu);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading menu: " + e.getMessage());
                }
            }

            // Notify all receivers
            notifyServiceModified(service);

            // Update current service reference if this is the current service
            if (currentService != null && currentService.getId() == serviceId) {
                currentService = service;
            }
        }

        return service;
    }

    /**
     * Deletes a service by its ID
     * 
     * @param serviceId ID of the service to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteService(int serviceId) {
        try {
            if (selectedEvent == null) {
                LOGGER.warning("Cannot delete service: no event selected");
                return false;
            }

            Service serviceToDelete = findServiceById(serviceId);
            if (serviceToDelete == null) {
                LOGGER.warning("Service with ID " + serviceId + " not found");
                return false;
            }

            LOGGER.info("Deleting service '" + serviceToDelete.getName() + "' (ID: " + serviceId + ")");

            // Delete from database
            boolean deleted = serviceToDelete.deleteService();

            if (!deleted) {
                LOGGER.warning("Database operation failed while deleting service " + serviceId);
                return false;
            }

            selectedEvent.removeService(serviceToDelete);

            // Clear current service if it was the one deleted
            if (currentService != null && currentService.getId() == serviceId) {
                currentService = null;
            }

            // Notify all receivers
            notifyServiceDeleted(serviceToDelete);

            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete service " + serviceId, e);
            return false;
        }
    }

    /**
     * Deletes an event and all its associated services
     * 
     * @param eventId ID of the event to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteEvent(int eventId) {
        try {
            Event eventToDelete = Event.loadById(eventId);
            if (eventToDelete == null) {
                LOGGER.warning("Event with ID " + eventId + " not found");
                return false;
            }

            LOGGER.info("Deleting event '" + eventToDelete.getName() + "' (ID: " + eventId + ")");

            // Use the event's delete method instead of direct SQL
            boolean deleted = eventToDelete.deleteEvent();

            if (!deleted) {
                LOGGER.warning("Database operation failed while deleting event " + eventId);
                return false;
            }

            // Clear references if this was the selected event
            if (selectedEvent != null && selectedEvent.getId() == eventId) {
                selectedEvent = null;
                currentService = null;
            }

            // Notify all receivers
            notifyEventDeleted(eventToDelete);

            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete event " + eventId, e);
            return false;
        }
    }

    /**
     * Assigns a menu to the current service
     * 
     * @param menu The menu to assign
     * @throws UseCaseLogicException if no event or service is selected
     */
    public void assignMenu(Menu menu) throws UseCaseLogicException {
        if (selectedEvent == null) {
            String msg = "Cannot assign menu: no event selected";
            LOGGER.warning(msg);
            throw new UseCaseLogicException(msg);
        }

        if (currentService == null) {
            String msg = "Cannot assign menu: no service selected";
            LOGGER.warning(msg);
            throw new UseCaseLogicException(msg);
        }

        LOGGER.info("Assigning menu '" + menu.getTitle() + "' to service '" + currentService.getName() + "'");

        currentService.assignMenuToService(menu);

        // Notify all receivers
        notifyMenuAssigned(currentService, menu);
    }

    /**
     * Removes the menu from the current service
     * 
     * @return true if removed successfully, false if no service selected
     */
    public boolean removeMenu() {
        if (currentService == null) {
            return false;
        }

        currentService.removeMenu();

        // Notify all receivers
        notifyMenuRemoved(currentService);

        return true;
    }

    /**
     * Helper method to find a service by ID within the selected event
     */
    private Service findServiceById(int serviceId) {
        if (selectedEvent == null || selectedEvent.getServices() == null) {
            return null;
        }

        for (Service s : selectedEvent.getServices()) {
            if (s.getId() == serviceId) {
                return s;
            }
        }

        return null;
    }

    // Notification methods to avoid code duplication

    private void notifyEventCreated(Event event) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateEventCreated(event);
        }
    }

    private void notifyEventModified(Event event) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateEventModified(event);
        }
    }

    private void notifyEventDeleted(Event event) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateEventDeleted(event);
        }
    }

    private void notifyServiceCreated(Service service) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateServiceCreated(selectedEvent, service);
        }
    }

    private void notifyServiceModified(Service service) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateServiceModified(service);
        }
    }

    private void notifyServiceDeleted(Service service) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateServiceDeleted(service);
        }
    }

    private void notifyMenuAssigned(Service service, Menu menu) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateMenuAssigned(service, menu);
        }
    }

    private void notifyMenuRemoved(Service service) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateMenuRemoved(service);
        }
    }
}
