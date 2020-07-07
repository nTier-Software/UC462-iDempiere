/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ntier.webservices.orders;

import java.util.Properties;
import org.idempiere.webservice.client.base.Enums;
import org.idempiere.webservice.client.base.LoginRequest;
import org.idempiere.webservice.client.net.WebServiceConnection;
import org.idempiere.webservice.client.request.CompositeOperationRequest;
import org.idempiere.webservice.client.request.QueryDataRequest;
import org.idempiere.webservice.client.response.CompositeResponse;
import org.idempiere.webservice.client.response.WindowTabDataResponse;

/**
 *
 * @author yogan naidoo
 */
public class SendWsRequest {

    public CompositeResponse sendWsRequest(CompositeOperationRequest compositeOperation, Properties erpProperties) {
        // CREATE CLIENT
        WebServiceConnection client = getClient(erpProperties);
        CompositeResponse response = null;
        try {
            // SEND REQUEST
            response = client.sendRequest(compositeOperation);
            // GET RESPONSE
            if (response.getStatus() == Enums.WebServiceResponseStatus.Error) {
                System.out.println(response.getErrorMessage());
            } else {
                for (int i = 0; i < response.getResponsesCount(); i++) {
                    if (response.getResponse(i).getStatus() == Enums.WebServiceResponseStatus.Error) {
                        System.out.println(response.getResponse(i).getErrorMessage());
                    } else {
                        System.out.println(response.getResponse(i).getWebServiceResponseModel());
                    }
                }
            }
            client.writeRequest(System.out);
            System.out.println();
            client.writeResponse(System.out);
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public int sendWsQueryRequest(QueryDataRequest wsRequest, Properties erpProperties) {
        // CREATE CLIENT
        WebServiceConnection client = getClient(erpProperties);
        WindowTabDataResponse response = null;
        try {
            // SEND REQUEST
            response = client.sendRequest(wsRequest);
            // GET RESPONSE
            if (response.getStatus() == Enums.WebServiceResponseStatus.Error) {
                System.out.println(response.getErrorMessage());
            }
            client.writeRequest(System.out);
            System.out.println();
            client.writeResponse(System.out);
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (!((response.getNumRows() == 0) || (response.getStatus() == Enums.WebServiceResponseStatus.Error))) ? (response.getDataSet().getRow(0).getFields().get(0).getIntValue()) : 0;
    }
    
    public WebServiceConnection getClient(Properties erpProperties) {
        WebServiceConnection client = new WebServiceConnection();
        client.setAttempts(1);
        client.setTimeout(30000);
        client.setAttemptsTimeout(0);
        client.setUrl(erpProperties.getProperty("UrlBase"));
        client.setAppName("Unicenta Integration");
        return client;
    }
    
    public LoginRequest getLogin(Properties erpProperties) {
        LoginRequest login = new LoginRequest();
        login.setUser(erpProperties.getProperty("UserName"));
        login.setPass(erpProperties.getProperty("UserPass"));
        login.setLang(Enums.Language.valueOf(erpProperties.getProperty("Language")));
        login.setClientID(Integer. parseInt(erpProperties.getProperty("AD_Client_ID")));
        login.setRoleID(Integer. parseInt(erpProperties.getProperty("AD_Role_ID")));
        login.setOrgID(Integer. parseInt(erpProperties.getProperty("AD_Org_ID")));
        login.setWarehouseID(Integer. parseInt(erpProperties.getProperty("M_Warehouse_ID")));
        return login;
    }
}
