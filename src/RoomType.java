public class RoomType {
    private String name;
    private int capacity;
    private double pricePerNight;
    private boolean isPenthouse;

    public RoomType(String name, double pricePerNight, int capacity) {
        this(name, pricePerNight, capacity, false);
    }
    
    public RoomType(String name, double pricePerNight, int capacity, boolean isPenthouse) {
        this.name = name;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
        this.isPenthouse = isPenthouse;
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
    
    public boolean isPenthouse() {
        return isPenthouse;
    }
    
    public void setPenthouse(boolean penthouse) {
        this.isPenthouse = penthouse;
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
