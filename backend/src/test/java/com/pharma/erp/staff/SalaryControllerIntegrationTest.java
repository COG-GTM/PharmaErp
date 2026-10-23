package com.pharma.erp.staff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SalaryControllerIntegrationTest extends StaffTestSupport {

    private int createSalary(int staffId, Map<String, Object> body) throws Exception {
        return read(perform(post("/api/staff/{id}/salaries", staffId).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated()))
                .get("id").asInt();
    }

    @Test
    void crudLifecycleAndDefaultOrdering() throws Exception {
        int staffId = createStaff(staffBody(unique()));
        createSalary(staffId, Map.of("amount", 1000.00, "payPeriod", "2026-07", "paidOn", "2026-07-31"));
        int latest = createSalary(staffId, Map.of("amount", 1200.25, "payPeriod", "2026-09"));

        perform(get("/api/staff/{id}/salaries", staffId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(latest))
                .andExpect(jsonPath("$.content[0].staffId").value(staffId))
                .andExpect(jsonPath("$.content[0].amount").value(1200.25));

        perform(put("/api/staff/{id}/salaries/{sid}", staffId, latest).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 1300, "payPeriod", "2026-09", "paidOn", "2026-09-30"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1300))
                .andExpect(jsonPath("$.paidOn").value("2026-09-30"));

        perform(get("/api/staff/{id}/salaries/{sid}", staffId, latest).with(admin()))
                .andExpect(status().isOk());
        perform(delete("/api/staff/{id}/salaries/{sid}", staffId, latest).with(admin()))
                .andExpect(status().isNoContent());
        perform(get("/api/staff/{id}/salaries/{sid}", staffId, latest).with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void validation() throws Exception {
        int staffId = createStaff(staffBody(unique()));
        perform(post("/api/staff/{id}/salaries", staffId).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 0, "payPeriod", "2026-13", "paidOn", "2999-01-01"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").isArray())
                .andExpect(jsonPath("$.errors.payPeriod").isArray())
                .andExpect(jsonPath("$.errors.paidOn").isArray());
        perform(post("/api/staff/{id}/salaries", staffId).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").isArray());
    }

    @Test
    void salaryIsScopedToItsStaffMember() throws Exception {
        int owner = createStaff(staffBody(unique()));
        int other = createStaff(staffBody(unique()));
        int salaryId = createSalary(owner, Map.of("amount", 500));

        perform(get("/api/staff/{id}/salaries/{sid}", other, salaryId).with(admin())).andExpect(status().isNotFound());
        perform(delete("/api/staff/{id}/salaries/{sid}", other, salaryId).with(admin()))
                .andExpect(status().isNotFound());
        perform(get("/api/staff/{id}/salaries", 999999).with(admin())).andExpect(status().isNotFound());
        perform(post("/api/staff/{id}/salaries", 999999).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("amount", 1))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingStaffCascadesSalaries() throws Exception {
        int staffId = createStaff(staffBody(unique()));
        createSalary(staffId, Map.of("amount", 750));
        perform(delete("/api/staff/{id}", staffId).with(admin())).andExpect(status().isNoContent());
        perform(get("/api/staff/{id}/salaries", staffId).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void payrollRequiresAdminScope() throws Exception {
        int staffId = createStaff(staffBody(unique()));
        perform(get("/api/staff/{id}/salaries", staffId).with(nonAdmin())).andExpect(status().isForbidden());
        int salaryId = createSalary(staffId, Map.of("amount", 1));
        perform(get("/api/staff/{id}/salaries/{sid}", staffId, salaryId).with(nonAdmin()))
                .andExpect(status().isForbidden());
        perform(post("/api/staff/{id}/salaries", staffId).with(nonAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("amount", 1))))
                .andExpect(status().isForbidden());
    }
}
