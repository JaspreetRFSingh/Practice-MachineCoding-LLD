package ShoppingCart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingCart {

    private static final String SUCCESS       = "SUCCESS";
    private static final String UNAVAILABLE   = "UNAVAILABLE";
    private static final String OUT_OF_STOCK  = "OUT OF STOCK";

    private final Map<String, CatalogItem> catalog = new HashMap<>();
    // cart stores the count of each item the user intends to purchase
    private final Map<String, Integer> cart = new HashMap<>();

    public ShoppingCart(List<String> items) {
        for (String row : items) {
            String[] parts = row.split(",");
            String itemId  = parts[0].trim();
            int price      = Integer.parseInt(parts[1].trim());
            int units      = Integer.parseInt(parts[2].trim());
            catalog.put(itemId, new CatalogItem(itemId, price, units));
        }
    }

    /**
     * Adds `count` units of itemId to the cart.
     *
     * Stock check is against catalog.availableUnits minus units already reserved
     * in the cart. This prevents over-committing stock before checkout.
     *
     * Returns SUCCESS / UNAVAILABLE / OUT OF STOCK.
     */
    public String addItem(String itemId, int count) {
        CatalogItem item = catalog.get(itemId);
        if (item == null) return UNAVAILABLE;

        int alreadyInCart   = cart.getOrDefault(itemId, 0);
        int totalRequested  = alreadyInCart + count;
        if (totalRequested > item.getAvailableUnits()) return OUT_OF_STOCK;

        cart.put(itemId, totalRequested);
        return SUCCESS;
    }

    /**
     * Returns cart contents sorted lexicographically by itemId.
     * Each row: "itemId,count"
     */
    public List<String> viewCart() {
        if (cart.isEmpty()) return Collections.emptyList();

        List<String> result = new ArrayList<>();
        List<String> sortedKeys = new ArrayList<>(cart.keySet());
        Collections.sort(sortedKeys);
        for (String key : sortedKeys) {
            result.add(key + "," + cart.get(key));
        }
        return result;
    }

    /**
     * Commits the cart: deducts stock, clears the cart, returns total cost.
     * Returns -1 if the cart is empty.
     */
    public int checkout() {
        if (cart.isEmpty()) return -1;

        int total = 0;
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            CatalogItem item = catalog.get(entry.getKey());
            int count = entry.getValue();
            total += item.getPricePerUnit() * count;
            item.deductUnits(count);
        }
        cart.clear();
        return total;
    }
}
