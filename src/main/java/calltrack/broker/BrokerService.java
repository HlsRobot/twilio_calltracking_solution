package calltrack.broker;

import calltrack.TwilioConfiguration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.twilio.http.HttpMethod;
import com.twilio.twiml.TwiMLException;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Dial;
import com.twilio.twiml.voice.Say;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class BrokerService {

    private final TwilioConfiguration twilioConfiguration;
    //List of available Twilio numbers in the account see: https://www.twilio.com/docs/phone-numbers/api/incomingphonenumber-resource#read-multiple-incomingphonenumber-resources
    private final List<String> availablePhoneNumbers = new ArrayList<>();
    static final String USERNAME     = "SALESFORCE_USERNAME";
    static final String PASSWORD     = "SALESFORCE_PASSWORD";
    static final String LOGINURL     = "https://login.salesforce.com";
    static final String GRANTSERVICE = "/services/oauth2/token?grant_type=password";
    static final String CLIENTID     = "CONSUMER_KEY_FROM_CONNECTED_APP";
    static final String CLIENTSECRET = "CONSUMER_SECRET_FROM_CONNECTED_APP";
    static final String DOMAINNAME   = "SALESFORCE_DOMAIN_NAME";


    @Autowired
    public BrokerService(TwilioConfiguration twilioConfiguration) {
        this.twilioConfiguration = twilioConfiguration;
    }

    //Check existing Seller - Broker relations and compare them to the newly created one.
    //Required in cas that an existing relation is updated with a new SFDC ID etc.
    private RelationDTO checkRelations(final String caller, final String twilioNumber) throws IOException {
        List<RelationDTO> relationList = this.readJsonFile();

        Optional<RelationDTO> relationSellerOpt = relationList.stream()
                .filter(relation -> caller.equals(relation.getSellerPhone()) && twilioNumber.equals(relation.getTwilioNumber()))
                .findFirst();
        if (relationSellerOpt.isPresent() && relationSellerOpt.get().isActive()) {
            return relationSellerOpt.get();
        } else {
            Optional<RelationDTO> relationBrokerOpt = relationList.stream()
                    .filter(relation -> caller.equals(relation.getBrokerPhone()) && twilioNumber.equals(relation.getTwilioNumber()))
                    .findFirst();
            if (relationBrokerOpt.isPresent() && relationBrokerOpt.get().isActive()) {
                return relationBrokerOpt.get();
            } else {
                return new RelationDTO();
            }
        }

    }

    //Helper methods for the current demo that read a Json list that simulates a DB
    private List<RelationDTO> readJsonFile() throws IOException {
        Gson gson = new Gson();

        try (FileReader fileReader = new FileReader("relations.json")){
            return gson.fromJson(fileReader, new TypeToken<List<RelationDTO>>() {}.getType());
        }
    }

    //Helper methods for the current demo that edits a Json list that simulates a DB
    private void writeJsonFile(List<RelationDTO> relationList) throws IOException {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter fileWriter = new FileWriter("relations.json")) {
            gson.toJson(relationList, fileWriter);
        }
    }

    //Main method that receives a request from SFDC (Apex class) with a newly created relation,
    // analyse it, convert it to DTO and stores it in the "DB"
    void updateDBFromSFDC(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        Map<String, String[]> parameters = request.getParameterMap();
        String bodyStringJson = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

        Gson g = new Gson();
        RelationDTO newRelation = g.fromJson(bodyStringJson, RelationDTO.class);
        newRelation.setActive(true);

        //Get existing relations from "DB"
        List<RelationDTO> relationList = this.readJsonFile();
        //check if the relation already exists
        OptionalInt indexOpt = IntStream.range(0, relationList.size())
                .filter(i -> newRelation.getSellerPhone().equals(relationList.get(i).getSellerPhone()) && newRelation.getBrokerPhone().equals(relationList.get(i).getBrokerPhone()))
                .findFirst();
        if (indexOpt.isPresent()) {
            newRelation.setTwilioNumber(relationList.get(indexOpt.getAsInt()).getTwilioNumber());
            relationList.set(indexOpt.getAsInt(), newRelation);
        } else {
            List<String> usedNumbers = new ArrayList<>();
            for (RelationDTO relation: relationList) {
                if (newRelation.getSellerPhone().equals(relation.getSellerPhone())) {
                    usedNumbers.add(relation.getTwilioNumber());
                } else if (newRelation.getBrokerPhone().equals(relation.getBrokerPhone())) {
                    usedNumbers.add(relation.getTwilioNumber());
                }
            }
            for (String record: this.availablePhoneNumbers) {
                if (!usedNumbers.contains(record)) {
                    newRelation.setTwilioNumber(record);
                    break;
                }
            }
            relationList.add(newRelation);
        }

        this.writeJsonFile(relationList);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String responseJson = g.toJson(newRelation);
        try {
            response.getWriter().print(responseJson);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Main method that Twilio invokes when a PSTN Call is initiated. It contains the information of the caller and the Twilio Number called
    //The combination of the two numbers can help to search the "DB" for existing active relations.
    // If found in connects the call to the second participant in the relation else it provides an automated message to the caller.
    void initProxyCall(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        Map<String, String[]> parameterMap = request.getParameterMap();
        String caller = request.getParameter("Caller");
        String twilioNumber = request.getParameter("Called");
        RelationDTO relation = this.checkRelations(caller, twilioNumber);

        String sayString = "You are now being connected with ";
        if (relation.isActive()) {
            Dial dial;
            Say say;
            if (caller.equals(relation.getSellerPhone())) {
                dial = new Dial.Builder(relation.getBrokerPhone())
                        .action("/broker/log?id=" + relation.getID()).method(HttpMethod.POST)
                        .callerId(request.getParameter("Called"))
                        .timeout(15)
                        .build();
                say = new Say.Builder(sayString + relation.getBrokerName()).voice(Say.Voice.POLLY_AMY).build();
            } else {
                dial = new Dial.Builder(relation.getSellerPhone())
                        .action("/broker/log?id=" + relation.getID()).method(HttpMethod.POST)
                        .callerId(request.getParameter("Called"))
                        .timeout(15)
                        .build();
                say = new Say.Builder(sayString + relation.getSellerName()).build();
            }
            VoiceResponse voiceResponse = new VoiceResponse.Builder().say(say).dial(dial).build();

            response.setContentType("text/xml");

            try {
                response.getWriter().print(voiceResponse.toXml());
            } catch (TwiMLException e) {
                e.printStackTrace();
            }
        } else {
            Say say = new Say.Builder("Unfortunately there is no current relation").build();
            VoiceResponse voiceResponse = new VoiceResponse.Builder().say(say).build();

            response.setContentType("text/xml");

            try {
                response.getWriter().print(voiceResponse.toXml());
            } catch (TwiMLException e) {
                e.printStackTrace();
            }
        }

    }

    //After a PSTN call is finished Twilio is invoking this method that provides information required like the call status, duration etc.
    //The information is then packed in Json and sent to SFDC to be stored in a record.
    void logCallInSFDC(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        Map<String, String[]> parameters = request.getParameterMap();

        String callStatus = request.getParameter("DialCallStatus");

        final String object = "Zugeh_rige_Makler__c";
        final String accessToken = this.getSFDCToken();

        CloseableHttpClient httpclient = HttpClientBuilder.create()
                .build();

        // Assemble the request URL
        String updateURL = DOMAINNAME + "/services/data/v53.0/sobjects/" + object + "/" + request.getParameter("id");


        HttpPatch httpPatch = new HttpPatch(updateURL);
        httpPatch.setHeader(HttpHeaders.CONTENT_TYPE,"application/json");
        httpPatch.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        StringEntity params = new StringEntity("{\"Call_Status__c\":\"" + callStatus + "\"}");
        httpPatch.setEntity(params);
        HttpResponse responseFromSFDC = null;

        try {
            // Execute the login POST request
            responseFromSFDC = httpclient.execute(httpPatch);
        } catch (IOException cpException) {
            cpException.printStackTrace();
        }

        if (responseFromSFDC.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            System.out.println("Error authenticating to Force.com: " + responseFromSFDC.getStatusLine().getStatusCode());
            // Error is in EntityUtils.toString(response.getEntity())
            System.out.println(EntityUtils.toString(responseFromSFDC.getEntity()));
        }

    }

    //Standard Java method to generate an auth token from Salesforce.
    private String getSFDCToken() throws IOException {
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();

        // Assemble the login request URL
        String loginURL = LOGINURL +
                GRANTSERVICE +
                "&client_id=" + CLIENTID +
                "&client_secret=" + CLIENTSECRET +
                "&username=" + USERNAME +
                "&password=" + PASSWORD;

        // Login requests must be POSTs
        HttpPost httpPost = new HttpPost(loginURL);
        HttpResponse response = null;

        try {
            // Execute the login POST request
            response = httpclient.execute(httpPost);
        } catch (IOException cpException) {
            cpException.printStackTrace();
        }

        // verify response is HTTP OK
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            System.out.println("Error authenticating to Force.com: "+statusCode);
            // Error is in EntityUtils.toString(response.getEntity())
            System.out.println(EntityUtils.toString(response.getEntity()));
            return "";
        }

        String getResult = null;
        try {
            getResult = EntityUtils.toString(response.getEntity());
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(getResult, JsonObject.class);
        httpPost.releaseConnection();

        String accessToken = jsonObject.get("access_token").toString();
        return accessToken.replaceAll("\"", "");
    }

}
