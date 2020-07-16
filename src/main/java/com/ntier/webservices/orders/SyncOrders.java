//    Openbravo POS is a point of sales application designed for touch screens.
//    http://www.openbravo.com/product/pos
//    Copyright (c) 2007 openTrends Solucions i Sistemes, S.L
//    Modified by Openbravo SL on March 22, 2007
//    These modifications are copyright Openbravo SL
//    Author/s: A. Romero , yogan naidoo (www.ntier.co.za)
//    You may contact Openbravo SL at: http://www.openbravo.com
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.
package com.ntier.webservices.orders;

import com.ntier.webservices.DataLogicIntegration;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSystem;
import com.ntier.webservices.JRootApp;
import com.openbravo.pos.payment.PaymentInfo;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.idempiere.webservice.client.base.DataRow;
import org.idempiere.webservice.client.base.Enums.DocAction;
import org.idempiere.webservice.client.base.Enums.WebServiceResponseStatus;
import org.idempiere.webservice.client.base.Field;
import org.idempiere.webservice.client.request.CompositeOperationRequest;
import org.idempiere.webservice.client.request.CreateDataRequest;
import org.idempiere.webservice.client.request.SetDocActionRequest;
import org.idempiere.webservice.client.response.CompositeResponse;
import static java.lang.Thread.sleep;
import org.idempiere.webservice.client.request.QueryDataRequest;
import static java.lang.Thread.sleep;
import static java.lang.Thread.sleep;
import static java.lang.Thread.sleep;

public class SyncOrders extends Thread {

    private final JRootApp app;

    private final DataLogicIntegration dlintegration;
    protected DataLogicSystem dlsystem;
    protected final Properties erpProperties;

    protected final String UCTenderType_CASH = "cash";
    protected final String UCTenderType_CREDITCARD = "ccard";
    protected final String UCTenderType_EFT = "bank";
    protected final String UCTenderType_CREDIT = "debt";

    public SyncOrders(JRootApp rootApp) {

        app = rootApp;
        dlintegration = (DataLogicIntegration) app.getBean("com.ntier.webservices.DataLogicIntegration");
        dlsystem = (DataLogicSystem) app.getBean("com.openbravo.pos.forms.DataLogicSystem");
        erpProperties = dlsystem.getResourceAsProperties("erp.properties");

    }

    @Override
    public void run() {

        boolean sent = true;
        Double stopLoop;
        int c = 0;

        while (true) {
            try {

                stopLoop = sent == true ? Double.valueOf(erpProperties.getProperty("wsOrderTypeInterval")) : 0.25;

                if (c != 0) {
                    sleep(converter(stopLoop));
                }
                System.out.println(exportToERP().getMessageMsg());

                sent = true;
            } catch (InterruptedException | BasicException ex) {
                Logger.getLogger(SyncOrders.class.getName()).log(Level.SEVERE, null, ex);
            }
            c++;
        }

    }

    public MessageInf exportToERP() throws BasicException {
        List<TicketInfo> ticketlist = dlintegration.getTicketsToSync();

        if (ticketlist.isEmpty()) {
            return new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.zeroorders"));
        } else {
            for (TicketInfo ticket : ticketlist) {
                ticket.setLines(dlintegration.getTicketLines(ticket.getId()));
                ticket.setPayments(dlintegration.getTicketPayments(ticket.getId()));
            }
            if (createWsOrders(ticketlist)) {
                return new MessageInf(MessageInf.SGN_SUCCESS, AppLocal.getIntString("message.syncordersok"), AppLocal.getIntString("message.syncordersinfo") + ticketlist.size());
            } else {
                return new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.syncorderserror"), AppLocal.getIntString("message.syncordersinfo") + ticketlist.size());
            }
        }
    }

    private boolean createWsOrders(List<TicketInfo> ticketlist) {

        boolean isOrdersSentOk = true;

        System.out.println("\n" + new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.qtyorders_sync")).getMessageMsg()
                + ticketlist.size() + "\n");

        Iterator iterator = ticketlist.iterator();
        while (iterator.hasNext()) {
            TicketInfo ticket = (TicketInfo) iterator.next();
            // Create Composite WS
            CompositeOperationRequest compositeOperation = new CompositeOperationRequest();
            SendWsRequest sendWsRequest = new SendWsRequest();
            compositeOperation.setWebServiceType(erpProperties.getProperty("wsCompositeOrderType"));

            // Set Login
            compositeOperation.setLogin(sendWsRequest.getLogin(erpProperties));

            buildOrder(compositeOperation, ticket);

            createWsOrderlines(compositeOperation, ticket);

            List<PaymentInfo> payments = ticket.getPayments();
            if (!payments.get(0).getName().equals(UCTenderType_CREDIT)) {
                new BuildPayment().createWsMixedPayment(compositeOperation, ticket, erpProperties);
            }

            completeOrder(compositeOperation);

            CompositeResponse response = sendWsRequest.sendWsRequest(compositeOperation, erpProperties);

            if (response.getStatus() == WebServiceResponseStatus.Error) {
                isOrdersSentOk = false;
            }

            updateTicketSyncStatus(response, ticket);
        }
        return isOrdersSentOk;
    }

    private void updateTicketSyncStatus(CompositeResponse response, TicketInfo ticket) {
        try {
            if (response.getStatus() == WebServiceResponseStatus.Successful) {
                System.out.println("\n" + "*************Order Imported: "
                        + ticket.getTicketId() + " *************" + "\n");
                dlintegration.execTicketUpdate(ticket.getId(), "1");

            } else {
                System.out.println("\n" + "*************Order Not Imported: "
                        + ticket.getTicketId() + " *************" + "\n");
                dlintegration.execTicketUpdate(ticket.getId(), "0");
            }
        } catch (BasicException ex) {
            Logger.getLogger(SyncOrders.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createWsOrderlines(CompositeOperationRequest compositeOperation, TicketInfo ticket) {
        for (TicketLineInfo line : ticket.getLines()) {
            // Create WS for Order Line
            CreateDataRequest createOrderLine = new CreateDataRequest();
            createOrderLine.setWebServiceType(erpProperties.getProperty("wsCreateOrderLine"));
            DataRow dataOrderLine = new DataRow();
            dataOrderLine.addField("AD_Client_ID", "@C_Order.AD_Client_ID");
            dataOrderLine.addField("AD_Org_ID", "@C_Order.AD_Org_ID");
            dataOrderLine.addField("C_Order_ID", "@C_Order.C_Order_ID");

            ProductInfoExt productInfo = null;
            try {
                productInfo = dlintegration.getProductInfo(line.getProductID());
            } catch (BasicException ex) {
                Logger.getLogger(SyncOrders.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            Field field;
            int ProductID = queryProductID(productInfo, erpProperties);
            if (ProductID != 0) {
                dataOrderLine.addField("M_Product_ID", ProductID);
            } else {
                //Lookup product via name
                field = new Field("M_Product_ID");
                field.setLval("Standard");
                dataOrderLine.addField(field);
            }

            dataOrderLine.addField("QtyOrdered", Double.toString(Math.abs(line.getMultiply())));
            dataOrderLine.addField("QtyEntered", Double.toString(Math.abs(line.getMultiply())));
            dataOrderLine.addField("Line", String.valueOf((Math.abs(line.getTicketLine()) + 1) * 10));
            dataOrderLine.addField("PriceEntered", Double.toString(round(line.getPrice(), 2)));
            dataOrderLine.addField("PriceActual", Double.toString(round(line.getPrice(), 2)));
            //Lookup tax via name
            field = new Field("C_Tax_ID");
            field.setLval(line.getTaxInfo().getName());
            dataOrderLine.addField(field);

            createOrderLine.setDataRow(dataOrderLine);
            compositeOperation.addOperation(createOrderLine);
        }
    }

    private void buildOrder(CompositeOperationRequest compositeOperation, TicketInfo ticket) {
        // Create WS for Order
        CreateDataRequest createOrder = new CreateDataRequest();
        createOrder.setWebServiceType(erpProperties.getProperty("wsOrderType"));
        DataRow data = new DataRow();
        Calendar datenew = Calendar.getInstance();
        datenew.setTime(ticket.getDate());
        try {
            if (ticket.getCustomerId() == null) {
                ticket.setCustomer(dlintegration.getCustomerInfoByName("Standard"));
            } else {
                ticket.setCustomer(dlintegration.getCustomerInfoByID(ticket.getCustomerId()));
            }
        } catch (BasicException ex) {
            Logger.getLogger(SyncOrders.class.getName()).log(Level.SEVERE, null, ex);
        }

        BuildNewOrQueryBP buildNewOrQueryBP = new BuildNewOrQueryBP();

        int RecordId = new BuildNewOrQueryBP().queryBpID(ticket, erpProperties);
        if (RecordId != 0) {
            data.addField("C_BPartner_ID", RecordId);
        } else {
            buildNewOrQueryBP.createBP(compositeOperation, ticket, erpProperties);
            data.addField("C_BPartner_ID", "@C_BPartner.C_BPartner_ID");
            data.addField("C_BPartner_Location_ID", "@C_BPartner_Location.C_BPartner_Location_ID");

        }

        if (ticket.getTicketType() == 0) {

            data.addField("C_DocTypeTarget_ID", erpProperties.getProperty("C_DocType_ID")); //regular order
        } else {

            data.addField("C_DocTypeTarget_ID", erpProperties.getProperty("C_DocTypeRefund_ID")); //return
        }
        data.addField("AD_Client_ID", erpProperties.getProperty("AD_Client_ID"));
        data.addField("AD_Org_ID", erpProperties.getProperty("AD_Org_ID"));

        data.addField("M_Warehouse_ID", erpProperties.getProperty("M_Warehouse_ID"));

        data.addField("DocumentNo", Integer.toString(ticket.getTicketId()));
        data.addField("DateOrdered", new java.sql.Timestamp(datenew.getTime().getTime()).toString());
        data.addField("SalesRep_ID", Integer.valueOf(ticket.getUser().getId()));

        List<PaymentInfo> payments = ticket.getPayments();
        data.addField("PaymentRule", (payments.get(0).getName().equals(UCTenderType_CREDIT)) ? "P" : "M");

        createOrder.setDataRow(data);
        compositeOperation.addOperation(createOrder);
    }

    private void completeOrder(CompositeOperationRequest compositeOperation) {
        SetDocActionRequest createDocAction = new SetDocActionRequest();
        createDocAction.setWebServiceType(erpProperties.getProperty("wsCompleteOrder"));
        createDocAction.setRecordID(0);
        createDocAction.setRecordIDVariable("@C_Order.C_Order_ID");
        createDocAction.setDocAction(DocAction.Complete);

        compositeOperation.addOperation(createDocAction);
    }

    int queryProductID(ProductInfoExt productInfo, Properties erpProperties) {
        QueryDataRequest query = new QueryDataRequest();
        SendWsRequest sendWsRequest = new SendWsRequest();
        query.setWebServiceType(erpProperties.getProperty("wsQueryProduct"));
        query.setLogin(sendWsRequest.getLogin(erpProperties));
        DataRow dataRow = new DataRow();
        dataRow.addField("AD_Client_ID", erpProperties.getProperty("AD_Client_ID"));
        dataRow.addField("Name", "%" + productInfo.getName() + "%");
        query.setDataRow(dataRow);
        return sendWsRequest.sendWsQueryRequest(query, erpProperties);
    }

    protected static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static boolean isBlankOrNull(String str) {
        return (str == null || "".equals(str.trim()));
    }

    public long converter(Double min) {
        long millis = (long) (min * 60 * 1000);
        return millis;
    }

}
