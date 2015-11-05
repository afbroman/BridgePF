package org.sagebionetworks.bridge.models.accounts;

import org.sagebionetworks.bridge.dynamodb.DynamoFPHSExternalIdentifier;
import org.sagebionetworks.bridge.json.BridgeTypeName;
import org.sagebionetworks.bridge.models.BridgeEntity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@BridgeTypeName("ExternalIdentifier")
@JsonDeserialize(as=DynamoFPHSExternalIdentifier.class)
public interface FPHSExternalIdentifier extends BridgeEntity {

    public static FPHSExternalIdentifier create() {
        return new DynamoFPHSExternalIdentifier();
    }
    
    public String getExternalId();
    public void setExternalId(String externalIdentifier);
    
    public boolean getRegistered();
    public void setRegistered(boolean registered);
    
}
