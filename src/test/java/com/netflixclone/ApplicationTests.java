package com.netflixclone;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void frontendAndEmailLinksArePublic() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("index.html"));
		mockMvc.perform(get("/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.allOf(
						org.hamcrest.Matchers.containsString("PrimeFlix"),
						org.hamcrest.Matchers.containsString("backend-status"),
						org.hamcrest.Matchers.containsString("password-dialog"),
						org.hamcrest.Matchers.containsString("hero-play"),
						org.hamcrest.Matchers.containsString("get-started-form"),
						org.hamcrest.Matchers.containsString("Frequently asked questions"),
						org.hamcrest.Matchers.containsString("unverified-warning"),
						org.hamcrest.Matchers.containsString("admin-users-dialog"))));
		mockMvc.perform(get("/style.css")).andExpect(status().isOk());
		mockMvc.perform(get("/script.js")).andExpect(status().isOk());
		mockMvc.perform(get("/favicon.svg")).andExpect(status().isOk());
		mockMvc.perform(get("/favicon.ico"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/favicon.svg"));
		mockMvc.perform(get("/verify-email").param("token", "test-token"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/index.html"));
		mockMvc.perform(get("/reset-password").param("token", "test-token"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/index.html"));
	}

	@Test
	void sessionValidationRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/auth/validate"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(username = "member@example.com")
	void sessionValidationUsesAuthenticatedPrincipal() throws Exception {
		mockMvc.perform(get("/api/auth/validate"))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));
	}
}
