package calltrack;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("twilio")
public class TwilioConfiguration {

    private String accountSid;
    private String flowSid;
    private String authToken;
    private String twilioNumber;
    private String protocol;
    private String url;

    public TwilioConfiguration() {

    }

    String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getFlowSid() {
        return flowSid;
    }

    public void setFlowSid(String flowSid) {
        this.flowSid = flowSid;
    }

    String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    String getTwilioNumber() {
        return twilioNumber;
    }

    public void setTwilioNumber(String twilioNumber) {
        this.twilioNumber = twilioNumber;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
