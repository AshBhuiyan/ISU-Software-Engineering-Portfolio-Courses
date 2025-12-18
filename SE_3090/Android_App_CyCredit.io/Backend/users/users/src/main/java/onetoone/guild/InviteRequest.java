package onetoone.guild;

public class InviteRequest {
    private Integer receiverUserId;
    private String receiverName;
    private Integer senderUserId;


    public InviteRequest(){}

    public Integer getReceiverUserId(){ return receiverUserId; }
    public void setReceiverUserId(Integer receiverUserId){ this.receiverUserId=receiverUserId; }
    public String getReceiverName(){ return receiverName; }
    public void setReceiverName(String receiverName){ this.receiverName=receiverName; }
    public  Integer getSenderUserId(){ return senderUserId; }
    public void setSenderUserId(Integer senderUserId){ this.senderUserId=senderUserId; }

}
