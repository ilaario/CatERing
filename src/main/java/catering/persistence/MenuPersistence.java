package catering.persistence;

import catering.businesslogic.menu.Menu;
import catering.businesslogic.menu.MenuEventReceiver;
import catering.businesslogic.menu.MenuItem;
import catering.businesslogic.menu.Section;

public class MenuPersistence implements MenuEventReceiver {

    @Override
    public void updateMenuCreated(Menu m) {
        Menu.create(m);
    }

    @Override
    public void updateSectionAdded(Menu m, Section sec) {
        Section.create(m.getId(), sec, m.getSectionPosition(sec));
    }

    @Override
    public void updateMenuItemAdded(Menu m, MenuItem mi) {
        Section sec = m.getSection(mi);
        int sec_id = (sec == null ? 0 : sec.getId());
        int pos = (sec == null ? m.getFreeItemPosition(mi) : sec.getItemPosition(mi));
        MenuItem.create(m.getId(), sec_id, mi, pos);
    }

    @Override
    public void updateMenuFeaturesChanged(Menu m) {
        Menu.saveFeatures(m);
    }

    @Override
    public void updateMenuTitleChanged(Menu m) {
        Menu.saveTitle(m);
    }

    @Override
    public void updateMenuPublishedState(Menu m) {
        Menu.savePublished(m);
    }

    @Override
    public void updateMenuDeleted(Menu m) {
        Menu.delete(m);
    }

    @Override
    public void updateSectionDeleted(Menu m, Section s, boolean itemsDeleted) {
        Section.deleteSection(m.getId(), s);
        if (!itemsDeleted)
            MenuItem.create(m.getId(), 0, s.getItems());
    }

    @Override
    public void updateSectionChangedName(Menu m, Section s) {
        Section.saveSectionName(s);
    }

    @Override
    public void updateSectionsRearranged(Menu m) {
        Menu.saveSectionOrder(m);
    }

    @Override
    public void updateFreeMenuItemsRearranged(Menu m) {
        Menu.saveFreeItemOrder(m);
    }

    @Override
    public void updateMenuItemsRearranged(Menu m, Section s) {
        Section.saveItemOrder(s);
    }

    @Override
    public void updateMenuItemChanged(Menu m, Section s, MenuItem mi) {
        int sid = (s == null ? 0 : s.getId());
        MenuItem.saveSection(sid, mi);
    }

    @Override
    public void updateMenuItemDescriptionChanged(Menu m, MenuItem mi) {
        MenuItem.saveDescription(mi);
    }

    @Override
    public void updateMenuItemDeleted(Menu m, Section sec, MenuItem mi) {
        MenuItem.removeItem(mi);
        if (sec != null) {
            Section.saveItemOrder(sec);
        } else
            Menu.saveFreeItemOrder(m);
    }
}
