package model;

/**
 * Represents a multiplayer game room.
 * Stores room metadata and player identifiers.
 */
public class MultiplayerRoom {

    private String roomId;
    private String hostPlayer;
    private String guestPlayer;
    private boolean started;
    private int level;
    private Card.Category category;

    public MultiplayerRoom(String roomId, String hostPlayer, int level, Card.Category category) {
        this.roomId = roomId;
        this.hostPlayer = hostPlayer;
        this.guestPlayer = null;
        this.started = false;
        this.level = level;
        this.category = category;
    }

    public boolean isFull() { return guestPlayer != null; }
    public boolean isStarted() { return started; }

    public String getRoomId() { return roomId; }
    public String getHostPlayer() { return hostPlayer; }
    public String getGuestPlayer() { return guestPlayer; }
    public int getLevel() { return level; }
    public Card.Category getCategory() { return category; }

    public void setGuestPlayer(String guest) { this.guestPlayer = guest; }
    public void setStarted(boolean started) { this.started = started; }

    @Override
    public String toString() {
        return "Room{id='" + roomId + "', host='" + hostPlayer
                + "', guest='" + guestPlayer + "', started=" + started + "}";
    }
}
