package com.yummynoodlebar.rest.functional;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.yummynoodlebar.rest.controller.fixture.RestDataFixture;
import com.yummynoodlebar.rest.domain.Order;

public class OrderTests {

	@Before
	public void setup() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
				request));
	}

	@Test
	public void thatOrdersCanBeAddedAndQueried() {

		ResponseEntity<Order> entity = createOrder();

		String path = entity.getHeaders().getLocation().getPath();

		assertEquals(HttpStatus.CREATED, entity.getStatusCode());
		assertTrue(path.startsWith("/aggregators/orders/"));
		Order order = entity.getBody();

		System.out.println("The Order ID is " + order.getKey());
		System.out.println("The Location is "
				+ entity.getHeaders().getLocation());

		assertEquals(2, order.getItems().size());
	}

	@Test
	public void thatOrdersCannotBeAddedAndQueriedWithBadUser() {

		HttpEntity<String> requestEntity = new HttpEntity<String>(
				RestDataFixture.standardOrderJSON(), getHeaders("letsnosh"
						+ ":" + "BADPASSWORD"));

		RestTemplate template = new RestTemplate();
		try {
			ResponseEntity<Order> entity = template.postForEntity(
					"http://localhost:8080/aggregators/orders", requestEntity,
					Order.class);

			fail("Request Passed incorrectly with status "
					+ entity.getStatusCode());
		} catch (HttpClientErrorException ex) {
			assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
		}
	}

	@Test
	public void thatOrdersHaveCorrectHateoasLinks() {

		ResponseEntity<Order> entity = createOrder();

		Order order = entity.getBody();

		String orderBase = "/aggregators/orders/" + order.getKey();

		assertEquals(entity.getHeaders().getLocation().toString(), order
				.getLink("self").getHref());
		assertTrue(order.getLink("Order Status").getHref()
				.endsWith(orderBase + "/status"));
	}

	private ResponseEntity<Order> createOrder() {
		HttpEntity<String> requestEntity = new HttpEntity<String>(
				RestDataFixture.standardOrderJSON(), getHeaders("letsnosh"
						+ ":" + "noshing"));

		RestTemplate template = new RestTemplate();
		return template.postForEntity(
				"http://localhost:8080/aggregators/orders", requestEntity,
				Order.class);
	}

	static HttpHeaders getHeaders(String auth) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

		byte[] encodedAuthorisation = Base64.encode(auth.getBytes());
		headers.add("Authorization", "Basic "
				+ new String(encodedAuthorisation));

		return headers;
	}
}
