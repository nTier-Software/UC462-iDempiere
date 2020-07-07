//    Openbravo POS is a point of sales application designed for touch screens.
//    http://www.openbravo.com/product/pos
//    Copyright (c) 2007 openTrends Solucions i Sistemes, S.L
//    Modified by Openbravo SL on March 22, 2007
//    These modifications are copyright Openbravo SL
//    Author/s: A. Romero
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
package com.ntier.webservices;

import java.util.List;
import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.DataRead;
import com.openbravo.data.loader.PreparedSentence;
import com.openbravo.data.loader.SerializerRead;
import com.openbravo.data.loader.SerializerReadClass;
import com.openbravo.data.loader.SerializerWriteString;
import com.openbravo.data.loader.Session;
import com.openbravo.data.loader.StaticSentence;
import com.openbravo.pos.forms.BeanFactoryDataSingle;
import com.openbravo.pos.inventory.TaxCategoryInfo;
import com.openbravo.pos.payment.PaymentInfoTicket;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.AppViewConnection;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author adrianromero Created on 5 de marzo de 2007, 19:56
 * @contributor Sergio Oropeza - Double Click Sistemas - Venezuela -
 * soropeza@dcs.net.ve, info@dcs.net.ve
 * @contributor yogan naidoo - nTier Software Services
 */
public class DataLogicIntegration extends BeanFactoryDataSingle {

    protected Session s;
    private Connection conInt;

    @Override
    public void init(AppView app) {
        try {
            this.s = AppViewConnection.createSession(app.getProperties());
            try {

                conInt = s.getConnection();
            } catch (SQLException e) {
                System.out.print("No session or connection");
            }
        } catch (BasicException ex) {
            Logger.getLogger(DataLogicIntegration.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public List getTickets() throws BasicException {
        return new PreparedSentence(s, "SELECT T.ID, T.TICKETTYPE, T.TICKETID, R.DATENEW, R.MONEY, R.ATTRIBUTES, P.ID, P.NAME, C.ID, "
                + "C.SEARCHKEY, C.NAME, C.TAXID "
                + "FROM receipts R JOIN tickets T ON R.ID = T.ID "
                + "JOIN (SELECT COUNT(LINE) LINESLEFT,TICKET FROM ticketlines WHERE I_ORDER_ID IS NULL GROUP BY TICKET) TL ON T.ID = TL.TICKET "
                + "LEFT OUTER JOIN people P ON T.PERSON = P.ID "
                + "LEFT OUTER JOIN customers C ON T.CUSTOMER = C.ID "
                + "WHERE (T.TICKETTYPE = 0 OR T.TICKETTYPE = 1) AND T.STATUS = 2 AND TL.LINESLEFT > 0 "
                + "ORDER BY T.TICKETID ASC", null, new SerializerReadClass(TicketInfo.class)).list();
    }

    /**
     * Check if there are tickets to be synced by checking database status
     * in tickets. 0 not synced, 1 synced
     *
     * @return List of tickets to be synced
     * @throws BasicException
     */
   
    public List getTicketsToSync() throws BasicException {
        //new StaticSentence(s, "update tickets set status=0 where id in (select ticket from ticketlines where i_order_id is null) ").exec();
        return new PreparedSentence(s, "SELECT T.ID, T.TICKETTYPE, T.TICKETID, R.DATENEW, R.MONEY, R.ATTRIBUTES, P.ID, P.NAME, C.ID,T.STATUS,C.SEARCHKEY, C.NAME, C.TAXID "
                + "FROM tickets T "
                + "INNER JOIN receipts R ON T.ID = R.ID "
                + "LEFT OUTER JOIN people P ON T.PERSON = P.ID "
                + "LEFT OUTER JOIN customers C ON T.CUSTOMER = C.ID "
                //+ "WHERE (T.TICKETTYPE = 0 OR T.TICKETTYPE = 1) AND T.STATUS = 0  AND (hostsync = '" + hostname + "' OR hostsync IS NULL) "
                + "WHERE ((T.TICKETTYPE = 0 OR T.TICKETTYPE = 1) AND T.STATUS = 0) "
                + "GROUP BY T.ID, T.TICKETTYPE, T.TICKETID, R.DATENEW, R.MONEY, R.ATTRIBUTES, P.ID, P.NAME, C.ID, C.SEARCHKEY, C.NAME, C.TAXID "
                + "ORDER BY T.TICKETID ASC", null, new SerializerReadClass(TicketInfo.class)).list();
    }

    public List getTicketLines(final String ticket) throws BasicException {
        return new PreparedSentence(s //, "SELECT L.TICKET, L.LINE, L.PRODUCT, L.ATTRIBUTESETINSTANCE_ID, L.UNITS, L.PRICE, T.ID, T.NAME, T.CATEGORY, T.VALIDFROM, T.CUSTCATEGORY, T.PARENTID, T.RATE, T.RATECASCADE, T.RATEORDER, L.ATTRIBUTES " +
                , "SELECT L.TICKET, L.LINE, L.PRODUCT, L.ATTRIBUTESETINSTANCE_ID, L.UNITS, L.PRICE, T.ID, T.NAME, T.CATEGORY, T.CUSTCATEGORY, T.PARENTID, T.RATE, T.RATECASCADE, T.RATEORDER, L.ATTRIBUTES "
                + "FROM ticketlines L, taxes T WHERE L.TAXID = T.ID AND L.TICKET = ? ORDER BY L.LINE" //  red1       , "SELECT L.TICKET, L.LINE, L.PRODUCT, L.UNITS, L.PRICE, T.ID, T.NAME, T.CATEGORY, T.CUSTCATEGORY, T.PARENTID, T.RATE, T.RATECASCADE, T.RATEORDER, L.ATTRIBUTES " +
                //  red1         "FROM TICKETLINES L, TAXES T WHERE L.TAXID = T.ID AND L.TICKET = ?"
                , SerializerWriteString.INSTANCE, new SerializerReadClass(TicketLineInfo.class)).list(ticket);
    }

    public List getTicketPayments(final String ticket) throws BasicException {
        return new PreparedSentence(s, "SELECT TOTAL, PAYMENT FROM payments WHERE RECEIPT = ?", SerializerWriteString.INSTANCE, new SerializerRead() {
            public Object readValues(DataRead dr) throws BasicException {
                return new PaymentInfoTicket(
                        dr.getDouble(1),
                        dr.getString(2));
            }
        }).list(ticket);
    }

    public CustomerInfoExt getCustomerInfoByID(final String ID) throws BasicException {
        return (CustomerInfoExt) new PreparedSentence(s, "SELECT C.ID, C.TAXID, C.SEARCHKEY, C.NAME, C.CARD, C.TAXCATEGORY, C.NOTES, C.MAXDEBT, C.VISIBLE, C.CURDATE, C.CURDEBT"
                + ", C.FIRSTNAME, C.LASTNAME, C.EMAIL, C.PHONE, C.PHONE2, C.FAX"
                + ", C.ADDRESS, C.ADDRESS2, C.POSTAL, C.CITY, C.REGION, C.COUNTRY"
                + " FROM customers C"
                + " WHERE C.ID = ?", SerializerWriteString.INSTANCE, CustomerInfoExt.getSerializerRead()
        ).find(ID);
    }
    
    public CustomerInfoExt getCustomerInfoByName(final String Name) throws BasicException {
        return (CustomerInfoExt) new PreparedSentence(s, "SELECT C.ID, C.TAXID, C.SEARCHKEY, C.NAME, C.CARD, C.TAXCATEGORY, C.NOTES, C.MAXDEBT, C.VISIBLE, C.CURDATE, C.CURDEBT"
                + ", C.FIRSTNAME, C.LASTNAME, C.EMAIL, C.PHONE, C.PHONE2, C.FAX"
                + ", C.ADDRESS, C.ADDRESS2, C.POSTAL, C.CITY, C.REGION, C.COUNTRY"
                + " FROM customers C"
                + " WHERE C.NAME = ?", SerializerWriteString.INSTANCE, CustomerInfoExt.getSerializerRead()
        ).find(Name);
    }

    public TaxCategoryInfo getTaxCategoryInfoByName(final String name) throws BasicException {
        return (TaxCategoryInfo) new PreparedSentence(s, "SELECT ID, NAME FROM taxcategories WHERE NAME = ?", SerializerWriteString.INSTANCE, new SerializerRead() {
            public Object readValues(DataRead dr) throws BasicException {
                return new TaxCategoryInfo(
                        dr.getString(1),
                        dr.getString(2));
            }
        }).find(name);
    }

    public void checkTickets() throws BasicException {

        new StaticSentence(s, "UPDATE tickets SET STATUS = 0 WHERE id in (select ticket from ticketlines where i_order_id is null) and status !=2").exec();

    }

    public void execTicketUpdate(String ticketid, String STATUS) throws BasicException {
        new StaticSentence(s, "UPDATE tickets SET STATUS = " + STATUS + " WHERE STATUS = 0 AND ID='" + ticketid + "'").exec();
    }

    public final ProductInfoExt getProductInfo(String id) throws BasicException {
        System.out.println("product info ");

        return (ProductInfoExt) new PreparedSentence(s, "SELECT "
                + "ID, "
                + "REFERENCE, "
                + "CODE, "
                + "CODETYPE, "
                + "NAME, "
                + "PRICEBUY, "
                + "PRICESELL, "
                + "CATEGORY, "
                + "TAXCAT, "
                + "ATTRIBUTESET_ID, "
                + "STOCKCOST, "
                + "STOCKVOLUME, "
                + "IMAGE, "
                + "ISCOM, "
                + "ISSCALE, "
                + "ISCONSTANT, "
                + "PRINTKB, "
                + "SENDSTATUS, "
                + "ISSERVICE, "
                + "ATTRIBUTES, "
                + "DISPLAY, "
                + "ISVPRICE, "
                + "ISVERPATRIB, "
                + "TEXTTIP, "
                + "WARRANTY, "
                + "COALESCE(stockcurrent.UNITS,0.0), "
                + "printto, "
                + "supplier, "
                + "uom, "
                + "memodate "
                + "FROM products LEFT JOIN stockcurrent ON (stockcurrent.PRODUCT = products.ID) "
                + "WHERE ID = ? "
                + "GROUP BY ID, REFERENCE, NAME, stockcurrent.UNITS", SerializerWriteString.INSTANCE, ProductInfoExt.getSerializerRead()).find(id);
    }

    @Override
    public void init(Session s) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
