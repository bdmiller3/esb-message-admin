/*
 Copyright 2015 esbtools Contributors and/or its affiliates.

 This file is part of esbtools.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.esbtools.message.admin.common;

import org.esbtools.message.admin.common.orm.AuditEventEntity;
import org.esbtools.message.admin.model.AuditEvent;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.Query;
import java.util.List;

public class AuditEventTest extends EsbMessageAdminTestBase {

    @Test
    public void testAuditEventCreation() {

        AuditEvent expected = new AuditEvent("testuser","ACTION1","ORDER","ORDER_NUMBER","12345","A message");
        service.saveAuditEvent(expected);

        AuditEventEntity got = new AuditEventEntity();
        Query query = getEntityManager().createQuery("select f from AuditEventEntity f where f.messageKey = 12345");
        List<AuditEventEntity> queryResult = (List<AuditEventEntity>) query.getResultList();
        if (queryResult != null && queryResult.size() != 0) {
            got = queryResult.get(0);
        }

        // the entity should have an id and log time
        Assert.assertNotNull(got.getEventId());
        Assert.assertNotNull(got.getLoggedTime());

        Assert.assertEquals(expected.getAction(), got.getAction());
        Assert.assertEquals(expected.getMessageType(), got.getMessageType());
        Assert.assertEquals(expected.getKeyType(), got.getKeyType());
        Assert.assertEquals(expected.getMessageKey(), got.getMessageKey());
        Assert.assertEquals(expected.getMessage(), got.getMessage());

    }

}
