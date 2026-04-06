package ShoppingCart;

/**
 * Represents a single entry in the item catalog.
 * availableUnits is mutable — it is decremented at checkout to reflect
 * consumed stock. It is NOT decremented when items are added to a cart,
 * so the cart holds a "reservation" until checkout commits it.
 */
public class CatalogItem {
    private final String itemId;
    private final int pricePerUnit;
    private int availableUnits;

    public CatalogItem(String itemId, int pricePerUnit, int availableUnits) {
        this.itemId = itemId;
        this.pricePerUnit = pricePerUnit;
        this.availableUnits = availableUnits;
    }

    public String getItemId() { return itemId; }
    public int getPricePerUnit() { return pricePerUnit; }
    public int getAvailableUnits() { return availableUnits; }

    public void deductUnits(int count) { availableUnits -= count; }
}
