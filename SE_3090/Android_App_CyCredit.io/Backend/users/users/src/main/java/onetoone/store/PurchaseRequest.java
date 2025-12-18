package onetoone.store;

public class PurchaseRequest {
  
    private Integer userId;
    
    private Integer itemId;
   
    private int qty = 1;
    
    private String purchaseNonce;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public String getPurchaseNonce() { return purchaseNonce; }
    public void setPurchaseNonce(String purchaseNonce) { this.purchaseNonce = purchaseNonce; }
}
