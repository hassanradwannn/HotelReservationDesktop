public interface Manageable {
    
    /**
     * Get the unique identifier for this entity
     * @return the entity ID
     */
    String getId();
    
    /**
     * Cancel this entity (e.g., cancel a reservation)
     * @return true if cancellation was successful, false otherwise
     */
    boolean cancel();
    
    /**
     * Update or modify this entity
     * @return true if update was successful, false otherwise
     */
    boolean update();
    
    /**
     * Get the current status of this entity
     * @return the status as a string
     */
    String getStatus();
    
    /**
     * Check if this entity is active
     * @return true if active, false otherwise
     */
    default boolean isActive() {
        return true;
    }
}
