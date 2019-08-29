package calltrack;

import com.google.gson.Gson;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.studio.v1.Flow;
import com.twilio.rest.studio.v1.flow.Execution;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Dial;
import com.twilio.twiml.voice.Number;
import com.twilio.twiml.voice.Say;
import com.twilio.type.PhoneNumber;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.*;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

@Service
public class CallTrackingService {

    private final HashMap<String, String> customerAdvertisementMapper = new HashMap<>();
    private final HashMap<String, Map.Entry<String, String>> advertisementMapper = new HashMap<>();

    private final TwilioConfiguration twilioConfiguration;

    @Autowired
    public CallTrackingService(final TwilioConfiguration twilioConfiguration) {
        this.twilioConfiguration = twilioConfiguration;
        this.advertisementMapper.put("abc", new AbstractMap.SimpleEntry<>("+4917626037618", "Bugatti Dealership"));
        this.advertisementMapper.put("abcd", new AbstractMap.SimpleEntry<>("<NUMBER OF DEALERSHIP 2>", "Fiat Dealership"));
        this.advertisementMapper.put("ghi", new AbstractMap.SimpleEntry<>("<NUMBER OF DEALERSHIP 3>", "Toyota Dealership"));
    }

    void init(final String caller, final String adId) {
        //TODO Add phone number validator

        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("advertisement", adId);
        parameters.put("get_dealer", String.format("%s/calltrack/get_dealer", this.twilioConfiguration.getUrl()));
        parameters.put("send_sms", String.format("%s/sms/send", this.twilioConfiguration.getUrl()));

        Execution execution = Execution.creator(this.twilioConfiguration.getFlowSid(), new PhoneNumber(caller), new PhoneNumber(this.twilioConfiguration.getTwilioNumber()))
                .setParameters(parameters).create();
        System.out.println(execution.getSid());

    }

    void getDealerNumber(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final String advertisement_id = request.getParameter("adId");
        if (this.advertisementMapper.containsKey(advertisement_id)) {
            this.mapCustomerWithDealer(request.getParameter("customer"), advertisement_id);
            response.setContentType("application/json");
            final HashMap<String, String> jsonResponse = new HashMap<>();
            jsonResponse.put("dealer_number", this.advertisementMapper.get(advertisement_id).getKey());
            jsonResponse.put("dealer_name", this.advertisementMapper.get(advertisement_id).getValue());
            response.getWriter().print(new Gson().toJson(jsonResponse));
            System.out.println("connecting to the dealer.");
        } else {
            System.out.println("The advertisement id is not found.");
            response.setStatus(HttpStatus.SC_NOT_FOUND);
        }
    }

    public void statusCallBack(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        String callStatus = request.getParameter("status");
        if (callStatus != null && (callStatus.equals("no-answer") || callStatus.equals("busy") || callStatus.equals("failed"))) {
            URL url = new URL("");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
        }
        System.out.println();
    }

    private void mapCustomerWithDealer(final String customer, final String advertisementId) {
        customerAdvertisementMapper.put(customer, advertisementId);
    }

    void proxyCall(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final String customerNumber = request.getParameter("caller");
        final HashMap<String, String> jsonResponse = new HashMap<>();
        if (this.customerAdvertisementMapper.containsKey(customerNumber)) {
            String advertisementId = this.customerAdvertisementMapper.get(customerNumber);

            if (this.advertisementMapper.containsKey(advertisementId)) {
                jsonResponse.put("error", "");
                jsonResponse.put("dealer_number", this.advertisementMapper.get(advertisementId).getKey());
                jsonResponse.put("dealer_name", this.advertisementMapper.get(advertisementId).getValue());
            } else {
                jsonResponse.put("error", "id");
            }
        } else {
            jsonResponse.put("error", "new");
        }
        response.setContentType("application/json");
        response.getWriter().print(new Gson().toJson(jsonResponse));
    }
}
