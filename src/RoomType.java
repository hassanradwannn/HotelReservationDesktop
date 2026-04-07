public class RoomType {
   private int typeId;
    private String typeName;
    private double pricePerNight;
    private int capacity;

    public RoomType() {
    }

    public RoomType(int typeId, String typeName, double pricePerNight, int capacity) {
        this.typeId = typeId;
        this.typeName = typeName;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
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

    public double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return "RoomType ID: " + typeId +
               ", Name: " + typeName +
               ", Price/Night: " + pricePerNight +
               ", Capacity: " + capacity;
    }
}
