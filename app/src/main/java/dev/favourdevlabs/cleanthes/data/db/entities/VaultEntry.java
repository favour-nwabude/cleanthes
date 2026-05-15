package dev.favourdevlabs.cleanthes.data.entities;

public class VaultEntry {

    private long id;
    private String title;
    private String username;
    private String encryptedPassword;
    private String website;
    private String category;
    private String notes;
    private long createdAt;
    private long updatedAt;
    private boolean isFavorite;

    public VaultEntry() {
    }

    public VaultEntry(String title, String username, String encryptedPassword, String website, String category,
            String notes, boolean isFavorite) {
        this.title = title;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
        this.website = website;
        this.category = category;
        this.notes = notes;
        this.isFavorite = isFavorite;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUsername() {
        return username;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public String getWebsite() {
        return website;
    }

    public String getCategory() {
        return category;
    }

    public String getNotes() {
        return notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    @Override
    public String toString() {
        return "VaultEntry{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", username='" + username + '\'' +
                ", category='" + category + '\'' +
                ", isFavorite=" + isFavorite +
                ", createdAt=" + createdAt +
                '}';
    }
}
