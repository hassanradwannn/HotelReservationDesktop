public class RoomType {
    private int id;
    private String name;
    private int capacity;
    private double pricePerNight;

    public RoomType(String name, double pricePerNight, int capacity) {
        this.name = name;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String typeName) {
        this.name = typeName;
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

    public void setPricePerNight(double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    void update(String name, int capacity, double pricePerNight) {
        this.name = name;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
    }

    void update(int capacity) {
        this.capacity = capacity;
    }

    void update(double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    @Override
    public String toString() {
        return "Name: " + name +
               ", Price/Night: " + pricePerNight +
               ", Capacity: " + capacity;
    }
}
