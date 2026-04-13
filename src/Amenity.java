public class Amenity {
  private int amenityId;
    private String name;
    private String description;
    private double price;

    public Amenity() {
    }

    public Amenity(int amenityId, String name, String description, double price) {
        this.amenityId = amenityId;
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public int getAmenityId() {
        return amenityId;
    }

    public void setAmenityId(int amenityId) {
        this.amenityId = amenityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Amenity ID: " + amenityId + ", Name: " + name + ", Description: " + description;
    }
}

