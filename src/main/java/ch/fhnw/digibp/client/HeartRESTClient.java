/*
 * Copyright (c) 2020. University of Applied Sciences and Arts Northwestern Switzerland FHNW.
 * All rights reserved.
 */

package ch.fhnw.digibp.client;

import ch.fhnw.digibp.util.PryvUtil;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HeartRESTClient {
    @Value("${pryv.token-endpoint}")
    String pryvTokenEndpointVault;

    @Value("${camunda-rest.tenantid}")
    String camundaTenantId;

    @Value("${camunda-rest.url}")
    String camundaRestUrl;

    public String getPryvTokenEndpointUser(String streamIdUserId){
        return Unirest.get(PryvUtil.getEndpoint(pryvTokenEndpointVault) + "events?streams="+streamIdUserId+"&limit=1")
                .header("Authorization", PryvUtil.getToken(pryvTokenEndpointVault))
                .asJson()
                .getBody()
                .getObject().getJSONArray("events").getJSONObject(0).getString("content");
    }

    public void registerUserInVault(String pryvTokenEndpoint){
        String jsonBody = "[\n" +
                "    {\n" +
                "        \"method\": \"streams.create\",\n" +
                "        \"params\": {\n" +
                "            \"id\": \"heart-access-token\",\n" +
                "            \"name\": \"Heart Access Token\"\n" +
                "        }\n" +
                "    },\n" +
                "    {\n" +
                "        \"method\": \"streams.create\",\n" +
                "        \"params\": {\n" +
                "            \"id\": \""+PryvUtil.getUserId(pryvTokenEndpoint)+"\",\n" +
                "            \"name\": \""+PryvUtil.getUserName(pryvTokenEndpoint)+"\",\n" +
                "            \"parentId\": \"heart-access-token\"\n" +
                "        }\n" +
                "    },\n" +
                "    {\n" +
                "        \"method\": \"events.create\",\n" +
                "        \"params\": {\n" +
                "            \"streamIds\": [\n" +
                "                \""+PryvUtil.getUserId(pryvTokenEndpoint)+"\"\n" +
                "            ],\n" +
                "            \"type\": \"credentials/pryv-api-endpoint\",\n" +
                "            \"content\": \""+pryvTokenEndpoint+"\"\n" +
                "        }\n" +
                "    }\n" +
                "]";

        Unirest.post(PryvUtil.getEndpoint(pryvTokenEndpointVault))
                .header("Authorization", PryvUtil.getToken(pryvTokenEndpointVault))
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .asString();
    }

    public void registerWebhook(String pryvTokenEndpoint, String webhookUrl){
        String jsonBody = "{\n" +
                "    \"url\": \""+webhookUrl+"\"\n" +
                "}";

        Unirest.post(PryvUtil.getEndpoint(pryvTokenEndpoint) + "webhooks")
                .header("Authorization", PryvUtil.getToken(pryvTokenEndpoint))
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .asString();
    }

    public void createAnalysis(String pryvTokenEndpoint, String category){
        String jsonBody = "{\n" +
                "    \"streamIds\": [\n" +
                "        \"analysis\"\n" +
                "    ],\n" +
                "    \"type\": \"note/txt\",\n" +
                "    \"content\": \"Analysis of the blood pressure category based on the last entry: "+category+"\"\n" +
                "}";

        Unirest.post(PryvUtil.getEndpoint(pryvTokenEndpoint) + "events")
                .header("Authorization", PryvUtil.getToken(pryvTokenEndpoint))
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .asString();
    }

    public JSONObject getBloodPressureEvent(String pryvTokenEndpointUser, String modifiedSince){
        return Unirest.get(PryvUtil.getEndpoint(pryvTokenEndpointUser) + "events?streams=blood-pressure&limit=1&modifiedSince="+modifiedSince+"")
                .header("Authorization", PryvUtil.getToken(pryvTokenEndpointUser))
                .asJson()
                .getBody()
                .getObject();
    }

    public void initHeartService(String eventId, Integer systolic, Integer diastolic){
        Unirest.post(camundaRestUrl+"/message")
                .header("Content-Type", "application/json")
                .body("{\n" +
                        "    \"messageName\": \"Message_VerifyBloodPressure\",\n" +
                        "    \"businessKey\": \""+eventId+"\",\n" +
                        "    \"tenantId\": \""+camundaTenantId+"\",\n" +
                        "    \"resultEnabled\": false,\n" +
                        "    \"processVariables\": {\n" +
                        "        \"systolic\": {\n" +
                        "            \"value\": "+systolic+",\n" +
                        "            \"type\": \"integer\"\n" +
                        "        },\n" +
                        "        \"diastolic\": {\n" +
                        "            \"value\": "+diastolic+",\n" +
                        "            \"type\": \"integer\"\n" +
                        "        }\n" +
                        "    }\n" +
                        "}")
                .asString();
    }

    public void createDiagnosis(String pryvTokenEndpoint, String diagnosis){
        String jsonBody = "{\n" +
                "    \"streamIds\": [\n" +
                "        \"diagnosis\"\n" +
                "    ],\n" +
                "    \"type\": \"note/txt\",\n" +
                "    \"content\": \""+diagnosis+"\"\n" +
                "}";

        Unirest.post(PryvUtil.getEndpoint(pryvTokenEndpoint) + "events")
                .header("Authorization", PryvUtil.getToken(pryvTokenEndpoint))
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .asString();
    }

    public String getPatientEmail(String pryvTokenEndpointUser){
        String email = "";
        JSONObject jsonObject = Unirest.get(PryvUtil.getEndpoint(pryvTokenEndpointUser) + "events?streams=contact-email&limit=1")
                .header("Authorization", PryvUtil.getToken(pryvTokenEndpointUser))
                .asJson()
                .getBody()
                .getObject();

        if(!jsonObject.getJSONArray("events").isEmpty()) {
            email = jsonObject.getJSONArray("events").getJSONObject(0).getString("content");
        }
        return email;
    }
}