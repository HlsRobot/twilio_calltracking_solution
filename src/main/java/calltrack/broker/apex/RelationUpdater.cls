public class RelationUpdater {

    @InvocableMethod(label='Update Relations')
    public static void sendNewRelation(List<RelationRequest> input) {

        callBackend(JSON.serialize(input[0]));
        
    }
    
    @future(callout=true)
    public static void callBackend(String relation) {
    
        Http http = new Http();
        HttpRequest request = new HttpRequest();
        
        request.setEndpoint('BACKEND_URL/broker/write');
        request.setMethod('POST');
        request.setHeader('Content-Type', 'application/json;charset=UTF-8');
        
        request.setBody(relation);
        HttpResponse response = http.send(request);
        
        if (response.getStatusCode() == 200) {

            JSONParser parser = JSON.createParser(response.getBody());
            String ID = '';
            String twilioNumber = '';
            while (parser.nextToken() != null) {
                if ((parser.getCurrentToken() == JSONToken.FIELD_NAME) && 
                    (parser.getText() == 'ID')) {
                    // Get the value.
                    parser.nextToken();
                    ID = parser.getText();
                } else if ((parser.getCurrentToken() == JSONToken.FIELD_NAME) && 
                    (parser.getText() == 'twilioNumber')) {
                    // Get the value.
                    parser.nextToken();
                    // Compute the grand total price for all invoices.
                    twilioNumber = parser.getText();
                }
            }
          
            update new Zugeh_rige_Makler__c(Id = ID, Call_Tracking_Nummer__c = twilioNumber);
        } 
    }
    
    public class RelationRequest {
        @InvocableVariable(required=true)
        public String sellerPhone;
        
        @InvocableVariable(required=true)
        public String sellerName;
        
        @InvocableVariable(required=true)
        public String brokerPhone;
        
        @InvocableVariable(required=true)
        public String brokerName;
        
        @InvocableVariable(required=true)
        public String ID;
        
    }
}