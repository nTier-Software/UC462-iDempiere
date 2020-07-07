/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ntier.webservices.orders;

import com.openbravo.pos.payment.PaymentInfo;
import com.openbravo.pos.ticket.TicketInfo;
import java.util.List;
import java.util.Properties;
import org.idempiere.webservice.client.base.DataRow;
import org.idempiere.webservice.client.request.CompositeOperationRequest;
import org.idempiere.webservice.client.request.CreateDataRequest;

/**
 *
 * @author yogan naidoo
 */
public class BuildPayment {

    void createWsMixedPayment(CompositeOperationRequest compositeOperation, TicketInfo ticket, Properties erpProperties) {
        // Create WS for Order Line
        CreateDataRequest createMixedPayment = new CreateDataRequest();
        createMixedPayment.setWebServiceType(erpProperties.getProperty("wsCreatePosPayment"));
        DataRow data = new DataRow();
        data.addField("AD_Client_ID", "@C_Order.AD_Client_ID");
        data.addField("AD_Org_ID", "@C_Order.AD_Org_ID");
        data.addField("C_Order_ID", "@C_Order.C_Order_ID");
        setPaymentTenderType(data, ticket);
        data.addField("PayAmt", Double.toString(SyncOrders.round(ticket.getTotal(), 2)));
        createMixedPayment.setDataRow(data);
        compositeOperation.addOperation(createMixedPayment);
    }

    private void setPaymentTenderType(DataRow data, TicketInfo ticket) {
        final String UCTenderType_CASH = "cash";
        final String UCTenderType_CREDITCARD = "ccard";
        final String UCTenderType_EFT = "bank";
        final String UCTenderType_CREDIT = "debt";
        
        List<PaymentInfo> payments = ticket.getPayments();
        switch (payments.get(0).getName()) {
            case UCTenderType_CASH:
                data.addField("C_POSTenderType_ID", IDPosTenderType.CASH.getValue());
                data.addField("TenderType", IDTenderType.CASH.getValue());
                break;
            case UCTenderType_CREDITCARD:
                data.addField("C_POSTenderType_ID", IDPosTenderType.CREDITCARD.getValue());
                data.addField("TenderType", IDTenderType.CREDITCARD.getValue());
                break;
            case UCTenderType_EFT:
                data.addField("C_POSTenderType_ID", IDPosTenderType.EFT.getValue());
                data.addField("TenderType", IDTenderType.EFT.getValue());
                break;
            default:
                System.out.println("\n" + "************ UC tender type not recognised************* " + payments.get(0).getName());
                data.addField("C_POSTenderType_ID", IDPosTenderType.CASH.getValue());
                data.addField("TenderType", IDTenderType.CASH.getValue());
        }
    }

    enum IDPosTenderType {
        CASH("1000001"),
        CREDITCARD("1000000"),
        EFT("1000002");

        private final String value;

        public final String getValue() {
            return value;
        }

        private IDPosTenderType(String value) {
            this.value = value;
        }
    }

    enum IDTenderType {
        CASH("X"),
        CREDITCARD("C"),
        EFT("A");

        private final String value;

        public final String getValue() {
            return value;
        }

        private IDTenderType(String value) {
            this.value = value;
        }
    }
}
