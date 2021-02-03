/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ntier.webservices.orders;

import com.openbravo.pos.ticket.TicketInfo;
import java.util.Properties;
import org.idempiere.webservice.client.base.DataRow;
import org.idempiere.webservice.client.base.Field;
import org.idempiere.webservice.client.request.CompositeOperationRequest;
import org.idempiere.webservice.client.request.CreateUpdateDataRequest;
import org.idempiere.webservice.client.request.QueryDataRequest;

/**
 *
 * @author yogan naidoo
 */
public class BuildNewOrQueryBP {

    private void createLocation(CompositeOperationRequest compositeOperation, TicketInfo ticket, Properties erpProperties) {
        CreateUpdateDataRequest createLocation = new CreateUpdateDataRequest();
        createLocation.setWebServiceType(erpProperties.getProperty("wsCreateLocation"));
        DataRow data = new DataRow();
        Field field = new Field("C_Country_ID");
        field.setLval(SyncOrders.isBlankOrNull(ticket.getCustomer().getCountry()) ? erpProperties.getProperty("country") : ticket.getCustomer().getCountry());
        data.addField(field);
        data.addField("Address1", SyncOrders.isBlankOrNull(ticket.getCustomer().getAddress()) ? erpProperties.getProperty("address1") : ticket.getCustomer().getAddress());
        data.addField("Address2", ticket.getCustomer().getAddress2());
        data.addField("Postal", ticket.getCustomer().getPcode());
        data.addField("City", SyncOrders.isBlankOrNull(ticket.getCustomer().getCity()) ? erpProperties.getProperty("city") : ticket.getCustomer().getCity());
        createLocation.setDataRow(data);
        compositeOperation.addOperation(createLocation);
        createBPLocation(compositeOperation, ticket, erpProperties);
    }

    void createBP(CompositeOperationRequest compositeOperation, TicketInfo ticket, Properties erpProperties) {
        CreateUpdateDataRequest createBP = new CreateUpdateDataRequest();
        createBP.setWebServiceType(erpProperties.getProperty("wsCreateBP"));
        DataRow data = new DataRow();
        data.addField("Name", ticket.getCustomer().getName());
        data.addField("IsCustomer", "Y");
        data.addField("IsVendor", "N");
        createBP.setDataRow(data);
        compositeOperation.addOperation(createBP);
        createLocation(compositeOperation, ticket, erpProperties);
    }

    private void createBPLocation(CompositeOperationRequest compositeOperation, TicketInfo ticket, Properties erpProperties) {
        CreateUpdateDataRequest createBPLocation = new CreateUpdateDataRequest();
        createBPLocation.setWebServiceType(erpProperties.getProperty("wsCreateBPLocation"));
        DataRow data = new DataRow();
        data.addField("C_BPartner_ID", "@C_BPartner.C_BPartner_ID");
        data.addField("C_Location_ID", "@C_Location.C_Location_ID");
        data.addField("IsShipTo", "True");
        data.addField("IsBillTo", "True");
        createBPLocation.setDataRow(data);
        compositeOperation.addOperation(createBPLocation);
        createUser(compositeOperation, ticket, erpProperties);
    }

    private void createUser(CompositeOperationRequest compositeOperation, TicketInfo ticket, Properties erpProperties) {
        CreateUpdateDataRequest createUser = new CreateUpdateDataRequest();
        createUser.setWebServiceType(erpProperties.getProperty("wsCreateUser"));
        DataRow data = new DataRow();
        if (SyncOrders.isBlankOrNull(ticket.getCustomer().getFirstname()) || SyncOrders.isBlankOrNull(ticket.getCustomer().getCemail())) {
            data.addField("Name", erpProperties.getProperty("contactName"));
            data.addField("EMail", erpProperties.getProperty("email"));
        } else {
            data.addField("Name", ticket.getCustomer().getFirstname());
            data.addField("EMail", ticket.getCustomer().getCemail());
        }
        data.addField("C_BPartner_ID", "@C_BPartner.C_BPartner_ID");
        data.addField("C_BPartner_Location_ID", "@C_BPartner_Location.C_BPartner_Location_ID");
        data.addField("Phone", ticket.getCustomer().getPhone1());
        createUser.setDataRow(data);
        compositeOperation.addOperation(createUser);
    }

    int queryBpRecordId(TicketInfo ticket, Properties erpProperties) {
        QueryDataRequest query = new QueryDataRequest();
        SendWsRequest sendWsRequest  = new SendWsRequest();
        query.setWebServiceType(erpProperties.getProperty("wsQueryBP"));
        query.setLogin(sendWsRequest.getLogin(erpProperties));
        DataRow dataRow = new DataRow();
        dataRow.addField("AD_Client_ID", erpProperties.getProperty("AD_Client_ID"));
        dataRow.addField("Name", "%" + ticket.getCustomer().getName() + "%");
        query.setDataRow(dataRow);
        return sendWsRequest.sendWsQueryRequest(query, erpProperties);
    }
    
}
