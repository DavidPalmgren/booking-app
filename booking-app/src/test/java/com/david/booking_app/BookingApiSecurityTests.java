package com.david.booking_app;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.david.booking_app.room.Room;
import com.david.booking_app.room.RoomRepository;

@SpringBootTest(properties = {
    "spring.security.oauth2.client.registration.github.client-id=test-id",
    "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
@AutoConfigureMockMvc
@Transactional
class BookingApiSecurityTests {

  @Autowired
  MockMvc mvc;

  @Autowired
  RoomRepository rooms;

  private static RequestPostProcessor githubUser(int id, String login) {
    return oauth2Login().attributes(attrs -> attrs.putAll(Map.of(
        "id", id,
        "login", login,
        "name", login + " Name")));
  }

  private String bookingJson(long roomId, int hour) {
    String day = LocalDate.now().plusDays(30).toString();
    return """
        {"roomId": %d, "start": "%sT%02d:00:00", "end": "%sT%02d:00:00"}
        """.formatted(roomId, day, hour, day, hour + 1);
  }

  @Test
  void apiRequiresLogin() throws Exception {
    mvc.perform(get("/rooms")).andExpect(status().isUnauthorized());
  }

  @Test
  void meReturnsUserCreatedFromGithubProfile() throws Exception {
    mvc.perform(get("/users/me").with(githubUser(900001, "octotest")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("octotest Name"))
        .andExpect(jsonPath("$.email").value("900001+octotest@users.noreply.github.com"))
        .andExpect(jsonPath("$.oauthSubject").doesNotExist());
  }

  @Test
  void bookingRequiresCsrfToken() throws Exception {
    Room room = rooms.save(new Room("Csrf Test Room"));

    mvc.perform(post("/bookings").with(githubUser(900002, "nocsrf"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(bookingJson(room.getId(), 9)))
        .andExpect(status().isForbidden());
  }

  @Test
  void bookingIsMadeAsLoggedInUserAndOnlyOwnerCanCancel() throws Exception {
    Room room = rooms.save(new Room("Owner Test Room"));

    String me = mvc.perform(get("/users/me").with(githubUser(900003, "owner")))
        .andReturn().getResponse().getContentAsString();
    String myId = me.replaceAll(".*\"id\":(\\d+).*", "$1");

    String created = mvc.perform(post("/bookings").with(githubUser(900003, "owner")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(bookingJson(room.getId(), 10)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(Long.parseLong(myId)))
        .andReturn().getResponse().getContentAsString();
    String bookingId = created.replaceAll(".*\"id\":(\\d+).*", "$1");

    mvc.perform(delete("/bookings/" + bookingId).with(githubUser(900004, "intruder")).with(csrf()))
        .andExpect(status().isForbidden());

    mvc.perform(delete("/bookings/" + bookingId).with(githubUser(900003, "owner")).with(csrf()))
        .andExpect(status().isNoContent());
  }
}
