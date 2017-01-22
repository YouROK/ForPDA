package forpdateam.ru.forpda.api.favorites.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by radiationx on 22.09.16.
 */

public class FavData {
    private List<FavItem> items = new ArrayList<>();

    private int itemsPerPage = 20, allPagesCount = 1, currentPage = 1;

    public void addItem(FavItem item) {
        items.add(item);
    }

    public List<FavItem> getItems() {
        return items;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public int getAllPagesCount() {
        return allPagesCount;
    }

    public void setAllPagesCount(int allPagesCount) {
        this.allPagesCount = allPagesCount;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
}
