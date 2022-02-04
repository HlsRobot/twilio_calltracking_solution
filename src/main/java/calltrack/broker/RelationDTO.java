package calltrack.broker;


import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class RelationDTO {

    private String ID;
    private String sellerPhone;
    private String sellerName;
    private String brokerPhone;
    private String brokerName;
    private String twilioNumber;
    private boolean active;


    public RelationDTO() {
        this.active = false;
    }

    public RelationDTO(String ID, String sellerPhone, String sellerName, String brokerPhone, String brokerName, String twilioNumber, boolean active) {
        this.ID = ID;
        this.sellerPhone = sellerPhone;
        this.sellerName = sellerName;
        this.brokerPhone = brokerPhone;
        this.brokerName = brokerName;
        this.twilioNumber = twilioNumber;
        this.active = active;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getSellerPhone() {
        return sellerPhone;
    }

    public void setSellerPhone(String sellerPhone) {
        this.sellerPhone = sellerPhone;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getBrokerPhone() {
        return brokerPhone;
    }

    public void setBrokerPhone(String brokerPhone) {
        this.brokerPhone = brokerPhone;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getTwilioNumber() {
        return twilioNumber;
    }

    public void setTwilioNumber(String twilioNumber) {
        this.twilioNumber = twilioNumber;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}


