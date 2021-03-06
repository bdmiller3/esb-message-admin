package org.esbtools.message.admin.common.feature;

import static org.esbtools.message.admin.common.config.EMAConfiguration.getResubmitBlackList;
import static org.esbtools.message.admin.common.config.EMAConfiguration.getResubmitControlHeader;
import static org.esbtools.message.admin.common.config.EMAConfiguration.getResubmitHeaderNamespace;
import static org.esbtools.message.admin.common.config.EMAConfiguration.getResubmitRestEndpoints;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.esbtools.gateway.resubmit.ResubmitRequest;
import org.esbtools.message.admin.common.orm.AuditEventEntity;
import org.esbtools.message.admin.common.orm.EsbMessageEntity;
import org.esbtools.message.admin.common.orm.EsbMessageHeaderEntity;
import org.esbtools.message.admin.common.utility.RestRequestUtility;
import org.esbtools.message.admin.model.AuditEvent;
import org.esbtools.message.admin.model.EsbMessage;
import org.esbtools.message.admin.model.Header;
import org.esbtools.message.admin.model.MetadataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class EmaResubmit {
    private static final Logger LOG = LoggerFactory.getLogger(EmaResubmit.class);
    private static final String RESUBMIT_EVENT = "Resubmit Happened";
    private static final String DEFAULT_USER = "someUser";
    
    @Inject
    private EntityManager entityMgr;
    
    public MetadataResponse resubmitMessage(EsbMessage esbMessage) {

        ResubmitRequest request = new ResubmitRequest();

        EsbMessageEntity persistedMessage = entityMgr.find(EsbMessageEntity.class, esbMessage.getId());
        // scold the user for trying to update instead of insert
        if( persistedMessage == null ){
            throw new IllegalArgumentException("Message {\n}"+esbMessage.toString()+"\n} does not exist in backend store, cannot update.");
        }

        if(StringUtils.isBlank(getResubmitControlHeader())){
            throw new RuntimeException("resubmitControlHeader not set in config.json - resubmit feature is unusable");
        }
        
        // It may be the case that the header we're configured to use isn't
        // present on the message So we should indicate this with an appropriate
        // return object so the front end can act.        
        try{
            request.setDestination( persistedMessage.getHeader( getResubmitControlHeader() ).getValue() );
        }catch(Exception e){
            LOG.warn("Message that was resubmitted lacked configured control header! Message ID: " + new Long(esbMessage.getId()).toString() );
            MetadataResponse result = new MetadataResponse();
            result.setErrorMessage("Unable to resubmit message due to configured resubmit control header not being present on message.");
            return result;
        }
        request.setHeaders( reduceToEsbHeaders(persistedMessage) );
        request.setSystem( persistedMessage.getSourceSystem() );

        // explicitly check if the loaded message is in our list of allowed message types
        if( isEditableMessage(persistedMessage) ){
            LOG.info("Message " + persistedMessage.getId() + " resubmitted with new payload");
            request.setPayload( esbMessage.getPayload() );
        } else {
            LOG.info("Message " + persistedMessage.getId() + " is not editable");
            request.setPayload( persistedMessage.getPayload() );
        }

        saveAuditEvent( new AuditEvent(DEFAULT_USER, RESUBMIT_EVENT, "", "", "", request.getPayload().toString() ) );
        LOG.info("Initiating resubmit request: {}", request.toJson());
        MetadataResponse result = sendMessageToResubmitGateway( request.toJson() );
        persistedMessage.setResubmittedOn(new Date());
        entityMgr.flush();
        return result;
    }
    
    /**
     * Saves audit event
     * @param auditEvent
     */
    public void saveAuditEvent(AuditEvent auditEvent) {
        AuditEventEntity event = new AuditEventEntity(auditEvent);
        entityMgr.persist(event);
    }
    
    private MetadataResponse sendMessageToResubmitGateway( String message ) {
        if(getResubmitRestEndpoints().size() == 0){
            throw new RuntimeException("resubmitRestEndpoints not set in config.json - resubmit feature is unusable");
        }

        if( RestRequestUtility.sendMessageToRestEndPoint( message, getResubmitRestEndpoints() ) ){
            return new MetadataResponse();
        } else{
            MetadataResponse result = new MetadataResponse();
            result.setErrorMessage("Unable to resubmit message.");
            return result;
        }

    }
    
    public static Boolean isEditableMessage( EsbMessageEntity message ){
        boolean editable = true;
        if(message.getMessageType() == null){
            LOG.warn(message.getMessageId() + " has no message type, defaulting to not allowing editing");
            editable = false;
        }

        if (message.getHeader("editableMessage") == null) {
            LOG.warn(message.getMessageId() + " has no editable header set, defaulting to not allowing editing");
            editable = false;
        } else {
            editable = Boolean.parseBoolean(message.getHeader("editableMessage").getValue());
        }

        return editable;
    }
    
    public static Boolean isEditableMessage( EsbMessage message ){
        boolean editable = true;
        if(message.getMessageType() == null){
            LOG.warn(message.getMessageId() + " has no message type, defaulting to not allowing editing");
            editable = false;
        }

        if (message.getHeaders() == null) {
            LOG.warn(message.getMessageId() + " has no message headers, defaulting to not allowing editing");
            editable = false;
        }

        for (Header header : message.getHeaders()) {
            if (header.getName().equals("editableMessage")) {
                editable = Boolean.parseBoolean(header.getValue());
            }
        }

        return editable;
    }
    
    private Map<String,String> reduceToEsbHeaders( EsbMessageEntity message ){
        Map<String,String> headers = new HashMap<String,String>();
        
        if(StringUtils.isBlank(getResubmitHeaderNamespace())){
            LOG.warn("Resubmit header namespace is blank, this means no headers will be added to the resubmit message");
        }

        for( EsbMessageHeaderEntity header : message.getErrorHeaders() ){
            if( header.getName().contains( getResubmitHeaderNamespace() ) ){
                headers.put( header.getName(), header.getValue() );
            }
        }

        return headers;
    }
}
