package onetoone.guild;
public class CreateGuildRequest {
    private String name;
    private Integer creatorUserId;

    public CreateGuildRequest(){}

    public String getName(){ return name; }
    public void setName(String name){ this.name=name; }
    public Integer getCreatorUserId(){ return creatorUserId; }
    public void setCreatorUserId(Integer creatorUserId){ this.creatorUserId=creatorUserId; }
}
