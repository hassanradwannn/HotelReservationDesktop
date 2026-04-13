public class RoomType {
   private int typeId;
    private String typeName;
    private int capacity;
    private double standardPrice;
    private double pricePerNight;

    public RoomType() {
    }

    public RoomType(int typeId, String typeName, double standardPrice, int capacity) {
        this.typeId = typeId;
        this.typeName = typeName;
        this.standardPrice = standardPrice;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight * capacity * 0.8;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public double getStandardPrice() {
        return standardPrice;
    }

    public void setStandardPrice(double pricePerNight) {
        this.standardPrice = pricePerNight;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getPricePerNight() {
        return this.pricePerNight;
    }

    @Override
    public String toString() {
        return "RoomType ID: " + typeId +
               ", Name: " + typeName +
               ", Price/Night: " + standardPrice +
               ", Capacity: " + capacity;
    }
}
