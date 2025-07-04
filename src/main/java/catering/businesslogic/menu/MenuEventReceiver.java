package catering.businesslogic.menu;

public interface MenuEventReceiver {

    public void updateMenuCreated(Menu m);

    public void updateMenuDeleted(Menu m);

    public void updateMenuTitleChanged(Menu m);

    public void updateMenuPublishedState(Menu m);

    public void updateMenuFeaturesChanged(Menu m);

    public void updateSectionAdded(Menu m, Section sec);

    public void updateSectionDeleted(Menu m, Section s, boolean itemsDeleted);

    public void updateSectionChangedName(Menu m, Section s);

    public void updateSectionsRearranged(Menu m);

    public void updateMenuItemAdded(Menu m, MenuItem mi);

    public void updateMenuItemDeleted(Menu m, Section sec, MenuItem mi);

    public void updateMenuItemChanged(Menu m, Section s, MenuItem mi);

    public void updateMenuItemDescriptionChanged(Menu m, MenuItem mi);

    public void updateMenuItemsRearranged(Menu m, Section s);

    public void updateFreeMenuItemsRearranged(Menu m);

}
