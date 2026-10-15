package com.david.booking_app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.security.oauth2.client.registration.github.client-id=test-id",
		"spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class BookingAppApplicationTests {

	@Test
	void contextLoads() {
	}

}
