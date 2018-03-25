
import com.messagePublisher.MessagePublisher
import com.messagePublisher.EnvironmentManager

import static spark.Spark.*;


public class message_publisher_service {
    
    
    static def call_table = [ 
        publishPaymentMatch : { messagePublisher, String merchantReferenceNumber, String transRefNo, String source = "" ->
            return messagePublisher.publishPaymentMatch(merchantReferenceNumber, transRefNo, source)
        },
        publishTcReleaseNotification : { messagePublisher, String merchantReferenceNumber, String decision ->
            return messagePublisher.publishTcReleaseNotification(merchantReferenceNumber, decision)
        },
        publishFcReleaseNotification : { messagePublisher, String merchantReferenceNumber, String decision ->
            return messagePublisher.publishFcReleaseNotification(merchantReferenceNumber, decision)
        },
        publishChargeback : { messagePublisher, String merchantReferenceNumber, String status, String statusFlag, String transactionId, String reasonCode, String chargebackAmt, String source, String currency, String originalChargeDate ->
            return messagePublisher.publishChargeback(merchantReferenceNumber, status, statusFlag, transactionId, reasonCode, chargebackAmt, source, currency, originalChargeDate)
        },
        publishChargebackForDD : { messagePublisher, String merchantReferenceNumber, String status, String statusFlag, String transactionId, String reasonCode, String chargebackAmt, String source, String currency, String originalChargeDate ->
            return messagePublisher.publishChargebackForDD(merchantReferenceNumber, status, statusFlag, transactionId, reasonCode, chargebackAmt, source, currency, originalChargeDate)
        },
    ]
    
    
    
    public static void main(String[] args) {
        
        def config_root = System.getenv("CONFIG_ROOT")
        if (config_root) {
            System.setProperty("ConfigRoot", config_root)
        } else {
            System.setProperty("ConfigRoot", "./SOAPJMS")
        }

        
        port(9999)
        threadPool(10)
    
    
        //
        // publishPaymentMatch <env_name> <merchantReferenceNumber> <transRefNo> [<source>]
        //
        get("/publishPaymentMatch/:env/:merchantReferenceNumber/:transRefNo", 
            "application/json",
            { req, res -> 
                def result = false
                try {
                    def messagePublisher = EnvironmentManager.getMessagePublisher(req.params(':env'))
                    result = call_table["publishPaymentMatch"](messagePublisher, req.params(':merchantReferenceNumber'), req.params(':transRefNo'))
                } catch (e) {
                    internalServerError("<html><body><h1>${e.toString()}</h1></body></html>")
                }
                return "{\"result\":\"${result}\"}"
            })
        get("/publishPaymentMatch/:env/:merchantReferenceNumber/:transRefNo/:source", 
            "application/json",
            { req, res -> 
                def result = false
                try {
                    def messagePublisher = EnvironmentManager.getMessagePublisher(req.params(':env'))
                    result = call_table["publishPaymentMatch"](messagePublisher, req.params(':merchantReferenceNumber'), req.params(':transRefNo'), req.params(':source'))
                } catch (e) {
                    internalServerError("<html><body><h1>${e.toString()}</h1></body></html>")
                }
                return "{\"result\":\"${result}\"}"
            })
        
        //
        // publishTcReleaseNotification <env_name> <merchantReferenceNumber> <decision> 
        //
        get("/publishTcReleaseNotification/:env/:merchantReferenceNumber/:decision", 
            "application/json",
            { req, res ->
                def result = false
                try {
                    def messagePublisher = EnvironmentManager.getMessagePublisher(req.params(':env'))
                    result = call_table["publishTcReleaseNotification"](messagePublisher, req.params(':merchantReferenceNumber'), req.params(':decision'))
                } catch (e) {
                    internalServerError("<html><body><h1>${e.toString()}</h1></body></html>")
                }
                return "{\"result\":\"${result}\"}"
            })
        
        //
        // publishFcReleaseNotification <env_name> <merchantReferenceNumber> <decision>
        //
        get("/publishFcReleaseNotification/:env/:merchantReferenceNumber/:decision", 
            "application/json",
            { req, res ->
                def result = false
                try {
                    def messagePublisher = EnvironmentManager.getMessagePublisher(req.params(':env'))
                    result = call_table["publishFcReleaseNotification"](messagePublisher, req.params(':merchantReferenceNumber'), req.params(':decision'))
                } catch (e) {
                    internalServerError("<html><body><h1>${e.toString()}</h1></body></html>")
                }
                return "{\"result\":\"${result}\"}"
            })
        
        //
        // publishChargeback <env_name> <merchantReferenceNumber> <status> <statusFlag> <transactionId> <reasonCode> <chargebackAmt> <source> <currency> <originalChargeDate>
        //
        get("/publishChargeback/:env/:merchantReferenceNumber/:status/:statusFlag/:transactionId/:reasonCode/:chargebackAmt/:source/:currency/:originalChargeDate", 
            "application/json",
            { req, res ->
                def result = false
                try {
                    def messagePublisher = EnvironmentManager.getMessagePublisher(req.params(':env'))
                    result = call_table["publishChargeback"](messagePublisher, req.params(':merchantReferenceNumber'), req.params(':status'), req.params(':statusFlag'),req.params(':transactionId'), req.params(':reasonCode'), req.params(':chargebackAmt'), req.params(':source'), req.params(':currency'), req.params(':originalChargeDate'))
                } catch (e) {
                    internalServerError("<html><body><h1>${e.toString()}</h1></body></html>")
                }
                return "{\"result\":\"${result}\"}"
            })
        
        //
        // publishChargebackForDD <env_name> <merchantReferenceNumber> <status> <statusFlag> <transactionId> <reasonCode> <chargebackAmt> <source> <currency> <originalChargeDate>
        //
        get("/publishChargebackForDD/:env/:merchantReferenceNumber/:status/:statusFlag/:transactionId/:reasonCode/:chargebackAmt/:source/:currency/:originalChargeDate", 
            "application/json",
            { req, res ->
                def result = false
                try {
                    def messagePublisher = EnvironmentManager.getMessagePublisher(req.params(':env'))
                    result = call_table["publishChargebackForDD"](messagePublisher, req.params(':merchantReferenceNumber'), req.params(':status'), req.params(':statusFlag'),req.params(':transactionId'), req.params(':reasonCode'), req.params(':chargebackAmt'), req.params(':source'), req.params(':currency'), req.params(':originalChargeDate'))
                } catch (e) {
                    internalServerError("<html><body><h1>${e.toString()}</h1></body></html>")
                }
                return "{\"result\":\"${result}\"}"
            })

        
        //
        // ROOT handler
        //
        get("/", 
            { req, res -> 
                """
                <html>
                <pre>
                
                MessagePublisher API's:
                
                ${call_table*.key}
                
                
                                
                * GET /publishPaymentMatch
                /publishPaymentMatch/:env/:merchantReferenceNumber/:transRefNo
                /publishPaymentMatch/:env/:merchantReferenceNumber/:transRefNo/:source 
                E.g. 
                <a href='./publishPaymentMatch/'>/publishPaymentMatch/</a>
                
            
                
                * GET /publishTcReleaseNotification
                /publishTcReleaseNotification/:env/:merchantReferenceNumber/:decision
                E.g.
                <a href='./publishTcReleaseNotification/'>/publishTcReleaseNotification/</a>
                
                
                
                * GET /publishFcReleaseNotification
                /publishFcReleaseNotification/:env/:merchantReferenceNumber/:decision
                E.g. 
                <a href='./publishFcReleaseNotification/'>/publishFcReleaseNotification/</a>
                
                
                
                * GET /publishChargeback
                /publishChargeback/:env/:merchantReferenceNumber/:status/:statusFlag/:transactionId/:reasonCode/:chargebackAmt/:source/:currency/:originalChargeDate
                E.g.
                <a href='./publishChargeback/'>/publishChargeback/</a>
                
                
                
                * GET /publishChargebackForDD
                /publishChargebackForDD/:env/:merchantReferenceNumber/:status/:statusFlag/:transactionId/:reasonCode/:chargebackAmt/:source/:currency/:originalChargeDate
                E.g.
                <a href='./publishChargebackForDD/'>/publishChargebackForDD/</a>
                
                
                
                </pre>
                </html>
                """
            })
    

        //
        // INTERNAL USE only
        //
        get("/logs", 
            { req, res -> 
                """
                <html>
                <pre>
                ${'tail -100 /tmp/jetty.log'.execute().text}
                </pre>
                </html>
                """
            })

    }
}
