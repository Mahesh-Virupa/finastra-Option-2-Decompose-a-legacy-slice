package com.modernization.slice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.adapters.file-dir=target/test-out")
class TradeSliceApiTest {
    private static final String ADMIN = "dev-admin-key";

    @Autowired MockMvc mvc;
    @Autowired MeterRegistry meters;

    private ResultActions trade(String tenant, String body) throws Exception {
        return mvc.perform(post("/api/v1/trades").header("X-Tenant-Id", tenant)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void clientA_bigTrade_dualApproval_discount_deskCode_thenPosts() throws Exception {
        String res = trade("CLIENT_A", """
                {"instrument":"EQ","trader":"t1","notional":1500000}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.approvalsRemaining").value(2))
                .andExpect(jsonPath("$.fee").value(270.0))
                .andExpect(jsonPath("$.deskCode").value("DESK-NY"))
                .andExpect(jsonPath("$.servedBy").value("NEW"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(res, "$.id");

        mvc.perform(post("/api/v1/trades/" + id + "/approvals").header("X-Tenant-Id", "CLIENT_A"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
        mvc.perform(post("/api/v1/trades/" + id + "/approvals").header("X-Tenant-Id", "CLIENT_A"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("POSTED"));
        mvc.perform(post("/api/v1/trades/" + id + "/approvals").header("X-Tenant-Id", "CLIENT_A"))
                .andExpect(status().isConflict());
    }

    @Test
    void clientB_fxTrade_taxAndLegacyQueueRoute() throws Exception {
        trade("CLIENT_B", """
                {"instrument":"FX","trader":"t1","notional":2000000}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.route").value("legacy-fx-queue"))
                .andExpect(jsonPath("$.tax").value(1000.0))
                .andExpect(jsonPath("$.fee").value(400.0));
    }

    @Test
    void tenantWithoutPolicy_getsOnlyTheCorePipeline() throws Exception {
        trade("CLIENT_Z", """
                {"instrument":"EQ","trader":"t1","notional":100000}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fee").value(20.0))
                .andExpect(jsonPath("$.route").value("DEFAULT"))
                .andExpect(jsonPath("$.status").value("POSTED"));
    }

    @Test
    void shadowMode_comparesLiveTrafficAndFindsNoMismatch() throws Exception {
        double before = meters.get("shadow.compared").counter().count();
        trade("CLIENT_A", """
                {"instrument":"FX","trader":"t2","notional":750000}""").andExpect(status().isCreated());
        assertThat(meters.get("shadow.compared").counter().count()).isGreaterThan(before);
        assertThat(meters.get("shadow.mismatch").counter().count()).isZero();
    }

    @Test
    void routingCanBeFlippedToLegacyAndBack_withIdenticalBusinessResult() throws Exception {
        String body = "{\"instrument\":\"EQ\",\"trader\":\"t1\",\"notional\":100000}";
        mvc.perform(put("/admin/v1/routing/CLIENT_R").header("X-Admin-Key", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"LEGACY\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.CLIENT_R").value("LEGACY"));
        trade("CLIENT_R", body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.servedBy").value("LEGACY")).andExpect(jsonPath("$.fee").value(20.0));

        mvc.perform(put("/admin/v1/routing/CLIENT_R").header("X-Admin-Key", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"NEW\"}")).andExpect(status().isOk());
        trade("CLIENT_R", body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.servedBy").value("NEW")).andExpect(jsonPath("$.fee").value(20.0));
    }

    @Test
    void invalidTradesAreRejected() throws Exception {
        trade("CLIENT_Z", "{\"instrument\":\"XYZ\",\"trader\":\"t1\",\"notional\":1000}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reasons", hasItem("Unsupported instrument: XYZ")));
        trade("CLIENT_Z", "{\"instrument\":\"EQ\",\"trader\":\"t1\",\"notional\":-5}").andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/trades").contentType(MediaType.APPLICATION_JSON)
                .content("{\"instrument\":\"EQ\",\"trader\":\"t1\",\"notional\":1}")).andExpect(status().isBadRequest());
    }

    @Test
    void tenantsCannotSeeEachOthersTrades() throws Exception {
        String res = trade("CLIENT_Z", "{\"instrument\":\"EQ\",\"trader\":\"t1\",\"notional\":1000}")
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(res, "$.id");
        mvc.perform(get("/api/v1/trades/" + id).header("X-Tenant-Id", "CLIENT_B")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/trades/" + id).header("X-Tenant-Id", "CLIENT_Z")).andExpect(status().isOk());
    }

    @Test
    void simulateHasNoSideEffects() throws Exception {
        mvc.perform(post("/api/v1/trades/simulate").header("X-Tenant-Id", "CLIENT_S")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instrument\":\"EQ\",\"trader\":\"t1\",\"notional\":1000000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fee").value(200.0));
        mvc.perform(get("/api/v1/trades").header("X-Tenant-Id", "CLIENT_S"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void routingAdminNeedsKey() throws Exception {
        mvc.perform(get("/admin/v1/routing")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/v1/routing").header("X-Admin-Key", ADMIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.CLIENT_A").value("SHADOW"))
                .andExpect(jsonPath("$.CLIENT_B", containsString("NEW")));
    }
}
