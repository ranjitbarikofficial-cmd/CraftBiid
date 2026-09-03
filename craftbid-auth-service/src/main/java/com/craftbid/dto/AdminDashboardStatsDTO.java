package com.craftbid.dto;

public class AdminDashboardStatsDTO {

    private long totalUsers;
    private long totalArtisans;
    private long totalCrafts;
    private long totalAuctions;
    private long totalActiveAuctions;
    private long totalBids;

    public AdminDashboardStatsDTO() {
    }

    public AdminDashboardStatsDTO(
            long totalUsers,
            long totalArtisans,
            long totalCrafts,
            long totalAuctions,
            long totalActiveAuctions,
            long totalBids) {
        this.totalUsers = totalUsers;
        this.totalArtisans = totalArtisans;
        this.totalCrafts = totalCrafts;
        this.totalAuctions = totalAuctions;
        this.totalActiveAuctions = totalActiveAuctions;
        this.totalBids = totalBids;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalArtisans() {
        return totalArtisans;
    }

    public void setTotalArtisans(long totalArtisans) {
        this.totalArtisans = totalArtisans;
    }

    public long getTotalCrafts() {
        return totalCrafts;
    }

    public void setTotalCrafts(long totalCrafts) {
        this.totalCrafts = totalCrafts;
    }

    public long getTotalAuctions() {
        return totalAuctions;
    }

    public void setTotalAuctions(long totalAuctions) {
        this.totalAuctions = totalAuctions;
    }

    public long getTotalActiveAuctions() {
        return totalActiveAuctions;
    }

    public void setTotalActiveAuctions(long totalActiveAuctions) {
        this.totalActiveAuctions = totalActiveAuctions;
    }

    public long getTotalBids() {
        return totalBids;
    }

    public void setTotalBids(long totalBids) {
        this.totalBids = totalBids;
    }
}
