package com.example.tokens.authorization;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthorizationController.class)
class AuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenAuthorizationService service;

    @Test
    void returnsDecision() throws Exception {
        when(service.authorize("04A2B3C4D5E6F7", "LOC-BER-HBF")).thenReturn(new AuthorizationDecision(
                "04A2B3C4D5E6F7", AuthorizationResult.ALLOWED, "DE-DCS-C12345678-X", "ref-1"));

        mockMvc.perform(get("/api/v1/tokens/{uid}/authorization", "04A2B3C4D5E6F7").param("locationId", "LOC-BER-HBF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("ALLOWED"))
                .andExpect(jsonPath("$.authorizationReference").value("ref-1"));
    }

    @Test
    void unknownTokenIs404() throws Exception {
        when(service.authorize("NOPE", "LOC-BER-HBF")).thenReturn(
                new AuthorizationDecision("NOPE", AuthorizationResult.UNKNOWN, null, null));

        mockMvc.perform(get("/api/v1/tokens/{uid}/authorization", "NOPE").param("locationId", "LOC-BER-HBF"))
                .andExpect(status().isNotFound());
    }
}
