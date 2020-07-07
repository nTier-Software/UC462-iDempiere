//    uniCenta oPOS  - Touch Friendly Point Of Sale
//    Copyright (c) 2009-2015 uniCenta & previous Openbravo POS works
//    http://www.unicenta.com
//
//    This file is part of uniCenta oPOS
//
//    uniCenta oPOS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//   uniCenta oPOS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with uniCenta oPOS.  If not, see <http://www.gnu.org/licenses/>.

package com.ntier.webservices;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.DataRead;
import com.openbravo.data.loader.SerializerRead;
import java.util.Date;

public class CustomerInfoExt extends com.openbravo.pos.customers.CustomerInfoExt {
    private Date Updated;
    //this method was in class DataLogicSales
    public static SerializerRead getSerializerRead() {
    
    
        return new SerializerRead() {
            @Override
            public Object readValues(DataRead dr) throws BasicException {
                CustomerInfoExt c = new CustomerInfoExt(dr.getString(1));
                c.setTaxid(dr.getString(2));
                c.setSearchkey(dr.getString(3));
                c.setName(dr.getString(4));
                c.setCard(dr.getString(5));
                c.setTaxCustomerID(dr.getString(6));
                c.setNotes(dr.getString(7));
                c.setMaxdebt(dr.getDouble(8));
                c.setVisible(dr.getBoolean(9));
                c.setCurdate(dr.getTimestamp(10));
                c.setAccdebt(dr.getDouble(11));
                c.setFirstname(dr.getString(12));
                c.setLastname(dr.getString(13));
                c.setCemail(dr.getString(14));
                c.setPhone1(dr.getString(15));
                c.setPhone2(dr.getString(16));
                c.setFax(dr.getString(17));
                c.setAddress(dr.getString(18));
                c.setAddress2(dr.getString(19));
                c.setPcode(dr.getString(20));
                c.setCity(dr.getString(21));
                c.setRegion(dr.getString(22));
                c.setCountry(dr.getString(23));
          return c;
        }};
    }
    
    
    
    /** Creates a new instance of UserInfoBasic
     * @param id */
    public CustomerInfoExt(String id) {
        super(id);
    }   

    public Date getUpdated() {
        return Updated;
    }

    public void setUpdated(Date Updated) {
        this.Updated = Updated;
    }
    
    
}
