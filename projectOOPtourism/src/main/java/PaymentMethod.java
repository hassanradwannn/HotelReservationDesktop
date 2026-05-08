public enum PaymentMethod {
    CASH("Cash"),
    CREDIT_CARD("Credit Card"),
    ONLINE("Online");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
