package calltrack;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class SmsService {

    @Autowired
    public SmsService() {
    }

    void sendSmsToDealer(final HttpServletRequest request, final HttpServletResponse response) {
        Message message = Message.creator(
                new PhoneNumber(request.getParameter("dealer")),
                new PhoneNumber(request.getParameter("twilio")),
                String.format("A customer tried to reach you for the advertisement with Id: %s. " +
                                "The customer's name is: %s and his number: %s. Regards CoolCars.com",
                        request.getParameter("advertisement"),
                        request.getParameter("cust_name"),
                        request.getParameter("cust_number"))).create();

        System.out.println("Message successfully sent");
        System.out.println(message.getSid());
    }
}
