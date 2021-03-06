/*
 * Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.connector.googledrive;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.core.AbstractConnector;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.FileList;

/**
 * Class mediator which maps to <strong>/files</strong> endpoint's <strong>list</strong> method. Gets a list
 * of files within the user's Drive. Returns the retrieved list of files as a Google Drive SDK FileList
 * resource in XML format and attaches to the message context's envelope body, and stores an error message as
 * a property on failure. Maps to the <strong>listFiles</strong> Synapse template within the <strong>Google
 * Drive</strong> connector.
 * 
 * @see https://developers.google.com/drive/v2/reference/files/list
 */
public class GoogledriveListFiles extends AbstractConnector {
    
    /**
     * Connector method which is executed at the specified point within the corresponding Synapse template
     * within the connector.
     * 
     * @param messageContext Synapse Message Context
     * @see org.wso2.carbon.connector.core.AbstractConnector#connect(org.apache.synapse.MessageContext)
     */
    public final void connect(final MessageContext messageContext) {
    
        Map<String, String> optParam = new HashMap<String, String>();
        optParam.put(GoogleDriveUtils.StringConstants.MAX_RESULTS,
                (String) getParameter(messageContext, GoogleDriveUtils.StringConstants.MAX_RESULTS));
        optParam.put(GoogleDriveUtils.StringConstants.PAGE_TOKEN,
                (String) getParameter(messageContext, GoogleDriveUtils.StringConstants.PAGE_TOKEN));
        optParam.put(GoogleDriveUtils.StringConstants.Q,
                (String) getParameter(messageContext, GoogleDriveUtils.StringConstants.Q));
        optParam.put(GoogleDriveUtils.StringConstants.FIELDS,
                (String) messageContext.getProperty(GoogleDriveUtils.StringConstants.FIELDS));
        
        Map<String, String> resultEnvelopeMap = new HashMap<String, String>();
        try {
            
            Drive service = GoogleDriveUtils.getDriveService(messageContext);
            
            FileList files = retrieveListOfFiles(service, optParam);
            
            resultEnvelopeMap.put(GoogleDriveUtils.StringConstants.FILE_LIST, files.toPrettyString());
            messageContext.getEnvelope().detach();
            // build new SOAP envelope to return to client
            messageContext.setEnvelope(GoogleDriveUtils.buildResultEnvelope(
                    GoogleDriveUtils.StringConstants.URN_GOOGLEDRIVE_LISTFILE,
                    GoogleDriveUtils.StringConstants.LIST_FILE_RESULT, resultEnvelopeMap));
            
        } catch (IOException ioe) {
            log.error("Failed to retrieve file list:", ioe);
            GoogleDriveUtils.storeErrorResponseStatus(messageContext, ioe,
                    GoogleDriveUtils.ErrorCodeConstants.ERROR_CODE_IO_EXCEPTION);
            handleException("Failed to retrieve file list:", ioe, messageContext);
        } catch (GeneralSecurityException gse) {
            
            log.error("Google Drive authentication failure:", gse);
            GoogleDriveUtils.storeErrorResponseStatus(messageContext, gse,
                    GoogleDriveUtils.ErrorCodeConstants.ERROR_CODE_GENERAL_SECURITY_EXCEPTION);
            handleException("Google Drive authentication failure: ", gse, messageContext);
        } catch (ValidationException ve) {
            log.error("Failed to validate integer parameter.", ve);
            GoogleDriveUtils.storeErrorResponseStatus(messageContext, ve,
                    GoogleDriveUtils.ErrorCodeConstants.ERROR_CODE_CONNECTOR_VALIDATION_EXCEPTION);
            handleException("Failed to validate integer parameter.", ve, messageContext);
        }
    }
    
    /**
     * Retrieve a list of File resources according to optional parameters passed.
     * 
     * @param service Drive API service instance.
     * @param optParam Collection of optional parameters.
     * @return List of File resources.
     * @throws IOException If an error occur on Google Drive API end.
     * @throws ValidationException If a validation error occurs.
     * @throws TokenResponseException If receiving an error response from the token server.
     */
    private FileList retrieveListOfFiles(final Drive service, final Map<String, String> optParam) throws IOException,
            ValidationException, TokenResponseException {
    
        Files.List request = service.files().list();
        
        String temporaryValue = optParam.get(GoogleDriveUtils.StringConstants.MAX_RESULTS);
        
        if (temporaryValue != null && !temporaryValue.isEmpty()) {
            
            request.setMaxResults(GoogleDriveUtils.toInteger(temporaryValue));
        }
        
        temporaryValue = optParam.get(GoogleDriveUtils.StringConstants.PAGE_TOKEN);
        
        if (temporaryValue != null && !temporaryValue.isEmpty()) {
            
            request.setPageToken(temporaryValue);
        }
        
        temporaryValue = optParam.get(GoogleDriveUtils.StringConstants.Q);
        
        if (temporaryValue != null && !temporaryValue.isEmpty()) {
            
            request.setQ(temporaryValue);
        }
        temporaryValue = optParam.get(GoogleDriveUtils.StringConstants.FIELDS);
        
        if (temporaryValue != null && !temporaryValue.isEmpty()) {
            
            request.setFields(temporaryValue);
        }
        
        return request.execute();
    }
    
}
