package hu.elte.ik.thesis.cinegrade.domain.user;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class User {

    private int id;
    private String username;
    private String email;
    private String avatarUrl;
    private String authToken;
    private Instant lastLogin;
    private Instant createdAt;

    public User(int id, String username, String email, String avatarUrl, String authToken, Instant lastLogin, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.authToken = authToken;
        this.lastLogin = lastLogin;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(Date.from(createdAt));
    }

    public String getFormattedLastLogin() {
        if (lastLogin == null) {
            return "Never";
        }

        Instant now = Instant.now();

        long years = ChronoUnit.YEARS.between(lastLogin, now);
        if (years > 0) return years == 1 ? "1 year ago" : years + " years ago";

        long months = ChronoUnit.MONTHS.between(lastLogin, now);
        if (months > 0) return months == 1 ? "1 month ago" : months + " months ago";

        long days = ChronoUnit.DAYS.between(lastLogin, now);
        if (days > 0) return days == 1 ? "Yesterday" : days + " days ago";

        long hours = ChronoUnit.HOURS.between(lastLogin, now);
        if (hours > 0) return hours == 1 ? "1 hour ago" : hours + " hours ago";

        long minutes = ChronoUnit.MINUTES.between(lastLogin, now);
        if (minutes > 0) return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";

        return "Just now";
    }
}
