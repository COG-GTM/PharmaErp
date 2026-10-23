package com.pharma.erp.staff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharma.erp.common.test.AbstractIntegrationTest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

abstract class StaffTestSupport extends AbstractIntegrationTest {

    @Autowired
    protected ObjectMapper objectMapper;

    protected static String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    protected RequestPostProcessor admin() {
        return bearer("staff_admin");
    }

    protected RequestPostProcessor nonAdmin() {
        String token = jwtService.issueToken("staff_viewer", "read");
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    protected JsonNode read(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    protected ResultActions perform(RequestBuilder builder) throws Exception {
        return mockMvc.perform(builder);
    }

    protected Map<String, Object> staffBody(String suffix) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", "Test" + suffix);
        body.put("lastName", "Member");
        body.put("dateOfBirth", "1992-05-17");
        body.put("joinDate", "2020-02-01");
        body.put("salary", 45000.50);
        body.put("mobileNumber", "+919800000000");
        body.put("email", "staff." + suffix + "@example.com");
        body.put("homeTown", "Rajkot");
        return body;
    }

    protected int createStaff(Map<String, Object> body) throws Exception {
        JsonNode node = read(mockMvc.perform(post("/api/staff").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated()));
        return node.get("id").asInt();
    }

    protected int createLoginType(String name) throws Exception {
        JsonNode node = read(mockMvc.perform(post("/api/login-types").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", name))))
                .andExpect(status().isCreated()));
        return node.get("id").asInt();
    }
}
