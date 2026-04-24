package tn.esprit.rh_rse.dto.Formation;

public class PaymentRequestDTO {
    private String userId;
    private int points;
    private int amount;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
}