public class Amenity {
    private int id;
    private String name;
    private double price;

    public Amenity(String name, double price) {
        this.name = name;
        this.price = price;
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

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    void update(String name, double price) {
        this.name = name;
        this.price = price;
    }

    void update(String name) {
        this.name = name;
    }

    void update(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Name: " + name + ", Price: " + price;
    }
}
