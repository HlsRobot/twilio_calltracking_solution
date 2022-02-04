package calltrack.broker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("broker")
public class BrokerController {

    private final BrokerService brokerService;

    @Autowired
    public BrokerController(BrokerService brokerService) {
        this.brokerService = brokerService;
    }

    @PostMapping("call")
    public void connectSellerBroker(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        this.brokerService.initProxyCall(request, response);
    }

    @PostMapping("write")
    public void addRelationToDB(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        this.brokerService.updateDBFromSFDC(request, response);
    }

    @PostMapping("log")
    public void logCall(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        this.brokerService.logCallInSFDC(request, response);
    }

}
