package com.pharma.erp.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTest {

    record SampleRequest(@NotBlank String name, @Positive int quantity) {}

    @RestController
    static class SampleController {
        @PostMapping("/api/sample")
        SampleRequest create(@Valid @RequestBody SampleRequest request) {
            return request;
        }

        @GetMapping("/api/sample/{id}")
        String find(@PathVariable long id) {
            throw NotFoundException.of("Sample", id);
        }

        @GetMapping("/api/sample/conflict")
        String conflict() {
            throw new ConflictException("duplicate");
        }
    }

    private final MockMvc mvc = standalone();

    private static MockMvc standalone() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void validationErrorsAre400ProblemJson() throws Exception {
        mvc.perform(post("/api/sample").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").isArray())
                .andExpect(jsonPath("$.errors.quantity").isArray());
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/api/sample").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void notFoundIs404ProblemJson() throws Exception {
        mvc.perform(get("/api/sample/42"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Sample 42 not found"));
    }

    @Test
    void conflictIs409() throws Exception {
        mvc.perform(get("/api/sample/conflict")).andExpect(status().isConflict());
    }
}
