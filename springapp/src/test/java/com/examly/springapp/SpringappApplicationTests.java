package com.examly.springapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringappApplicationTests {

    private String admintoken;
    private String customertoken;

    private Long customerId;
    private Long courseId;
    private Long orderId;
    private Long reviewId;
    private Long cartId;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = createHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    // ---------- Register & Login ----------

    @Test
    @Order(1)
    void backend_testRegisterAdmin() {
        String requestBody = "{\"userId\": 1,\"email\": \"demoadmin@gmail.com\", \"password\": \"admin@1234\", \"username\": \"admin123\", \"userRole\": \"ADMIN\", \"role\": \"ADMIN\", \"mobileNumber\": \"9876543210\"}";
        ResponseEntity<String> response = restTemplate.postForEntity("/api/register",
                new HttpEntity<>(requestBody, createHeaders()), String.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @Order(2)
    void backend_testRegisterCustomerUser() {
        String requestBody = "{\"userId\": 2,\"email\": \"democustomer@gmail.com\", \"password\": \"user@1234\", \"username\": \"customer123\", \"userRole\": \"CUSTOMER\", \"role\": \"CUSTOMER\", \"mobileNumber\": \"1122334455\"}";
        ResponseEntity<String> response = restTemplate.postForEntity("/api/register",
                new HttpEntity<>(requestBody, createHeaders()), String.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @Order(3)
    void backend_testLoginAdmin() throws Exception {
        String requestBody = "{\"email\": \"demoadmin@gmail.com\", \"username\": \"admin123\", \"password\": \"admin@1234\"}";

        ResponseEntity<String> response = restTemplate.postForEntity("/api/login",
                new HttpEntity<>(requestBody, createHeaders()), String.class);

        Assertions.assertNotNull(response.getBody(), "Response body is null!");

        JsonNode responseBody = objectMapper.readTree(response.getBody());
        admintoken = responseBody.get("token").asText();

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(admintoken);
    }

    @Test
    @Order(4)
    void backend_testLoginCustomer() throws Exception {
        String requestBody = "{\"email\": \"democustomer@gmail.com\", \"username\": \"customer123\", \"password\": \"user@1234\"}";

        ResponseEntity<String> response = restTemplate.postForEntity("/api/login",
                new HttpEntity<>(requestBody, createHeaders()), String.class);

        Assertions.assertNotNull(response.getBody(), "Response body is null!");

        JsonNode responseBody = objectMapper.readTree(response.getBody());
        customertoken = responseBody.get("token").asText();

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(customertoken);
    }

    // ---------- Customer profile ----------

    @Test
    @Order(5)
    void backend_testRegisterCustomerProfile() throws Exception {
        Assertions.assertNotNull(customertoken, "Customer token should not be null");

        String requestBody = "{\"customerName\": \"John Doe\", \"information\": \"Enthusiastic lifelong learner\", \"user\": {\"userId\": 2}}";

        HttpEntity<String> request = new HttpEntity<>(requestBody, createHeaders(customertoken));
        ResponseEntity<String> response = restTemplate.exchange("/api/customer", HttpMethod.POST, request, String.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());

        JsonNode responseBody = objectMapper.readTree(response.getBody());
        customerId = responseBody.get("customerId").asLong();

        Assertions.assertEquals("John Doe", responseBody.get("customerName").asText());
        Assertions.assertNotNull(customerId);
    }

   
    @Test
    @Order(6)
    void backend_testGetCustomerByUserId() throws Exception {
        String url = "/api/customer/user/2";

        HttpEntity<Void> request = new HttpEntity<>(createHeaders(customertoken));
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode responseBody = objectMapper.readTree(response.getBody());
        Assertions.assertEquals("John Doe", responseBody.get("customerName").asText());
    }

    // ---------- Course ----------

    @Test
    @Order(7)
    void backend_testAddCourseWithRoleValidation() throws Exception {
        String requestBody = "{\"courseType\": \"Web Development\", \"courseImageUrl\": \"https://8080-abccefeedacacecccdadbbfacbafdfacaeaeacfdedbcbce.premiumproject.examly.io/course.png\", \"courseDetails\": \"Full stack web development course\", \"coursePrice\": 1999.0}";

        HttpEntity<String> adminRequest = new HttpEntity<>(requestBody, createHeaders(admintoken));
        ResponseEntity<String> adminResponse = restTemplate.exchange("/api/course", HttpMethod.POST, adminRequest, String.class);
        Assertions.assertEquals(HttpStatus.CREATED, adminResponse.getStatusCode());

        JsonNode responseBody = objectMapper.readTree(adminResponse.getBody());
        courseId = responseBody.get("courseId").asLong();
        Assertions.assertEquals("Web Development", responseBody.get("courseType").asText());
        Assertions.assertEquals(1999.0, responseBody.get("coursePrice").asDouble());

        HttpEntity<String> customerRequest = new HttpEntity<>(requestBody, createHeaders(customertoken));
        ResponseEntity<String> customerResponse = restTemplate.exchange("/api/course", HttpMethod.POST, customerRequest, String.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN, customerResponse.getStatusCode());
    }

    @Test
    @Order(8)
    void backend_testGetAllCoursesWithRoleValidation() throws Exception {
        HttpEntity<Void> adminRequest = new HttpEntity<>(createHeaders(admintoken));
        ResponseEntity<String> adminResponse = restTemplate.exchange("/api/course", HttpMethod.GET, adminRequest, String.class);
        Assertions.assertEquals(HttpStatus.OK, adminResponse.getStatusCode());
        Assertions.assertTrue(objectMapper.readTree(adminResponse.getBody()).isArray());

        HttpEntity<Void> customerRequest = new HttpEntity<>(createHeaders(customertoken));
        ResponseEntity<String> customerResponse = restTemplate.exchange("/api/course", HttpMethod.GET, customerRequest, String.class);
        Assertions.assertEquals(HttpStatus.OK, customerResponse.getStatusCode());
    }

    @Test
    @Order(9)
    void backend_testGetCourseByIdWithRoleValidation() throws Exception {
        Assertions.assertNotNull(courseId, "courseId should not be null");
        String url = "/api/course/courses/" + courseId;

        HttpEntity<Void> adminRequest = new HttpEntity<>(createHeaders(admintoken));
        ResponseEntity<String> adminResponse = restTemplate.exchange(url, HttpMethod.GET, adminRequest, String.class);
        Assertions.assertEquals(HttpStatus.OK, adminResponse.getStatusCode());

        JsonNode responseBody = objectMapper.readTree(adminResponse.getBody());
        Assertions.assertEquals(courseId, responseBody.get("courseId").asLong());

        HttpEntity<Void> customerRequest = new HttpEntity<>(createHeaders(customertoken));
        ResponseEntity<String> customerResponse = restTemplate.exchange(url, HttpMethod.GET, customerRequest, String.class);
        Assertions.assertEquals(HttpStatus.OK, customerResponse.getStatusCode());
    }

    
    // ---------- Orders ----------

    @Test
    @Order(10)
    void backend_testCreateOrderWithRoleValidation() throws Exception {
        Assertions.assertNotNull(courseId, "courseId should not be null");
        Assertions.assertNotNull(customerId, "customerId should not be null");

        String requestBody = "{\"orderPrice\": 2499.0, \"courses\": [{\"courseId\": " + courseId + "}], \"customer\": {\"customerId\": " + customerId + "}}";

        HttpEntity<String> customerRequest = new HttpEntity<>(requestBody, createHeaders(customertoken));
        ResponseEntity<String> customerResponse = restTemplate.exchange("/api/order", HttpMethod.POST, customerRequest, String.class);
        Assertions.assertEquals(HttpStatus.CREATED, customerResponse.getStatusCode());

        JsonNode responseBody = objectMapper.readTree(customerResponse.getBody());
        orderId = responseBody.get("orderId").asLong();
        Assertions.assertEquals(2499.0, responseBody.get("orderPrice").asDouble());

        HttpEntity<String> adminRequest = new HttpEntity<>(requestBody, createHeaders(admintoken));
        ResponseEntity<String> adminResponse = restTemplate.exchange("/api/order", HttpMethod.POST, adminRequest, String.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN, adminResponse.getStatusCode());
    }

    @Test
    @Order(11)
    void backend_testGetAllOrdersWithRoleValidation() throws Exception {
        HttpEntity<Void> adminRequest = new HttpEntity<>(createHeaders(admintoken));
        ResponseEntity<String> adminResponse = restTemplate.exchange("/api/order", HttpMethod.GET, adminRequest, String.class);
        Assertions.assertEquals(HttpStatus.OK, adminResponse.getStatusCode());
        Assertions.assertTrue(objectMapper.readTree(adminResponse.getBody()).isArray());

        HttpEntity<Void> customerRequest = new HttpEntity<>(createHeaders(customertoken));
        ResponseEntity<String> customerResponse = restTemplate.exchange("/api/order", HttpMethod.GET, customerRequest, String.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN, customerResponse.getStatusCode());
    }

    @Test
    @Order(12)
    void backend_testGetOrderByIdWithRoleValidation() throws Exception {
        Assertions.assertNotNull(orderId, "orderId should not be null");
        String url = "/api/order/" + orderId;

        HttpEntity<Void> adminRequest = new HttpEntity<>(createHeaders(admintoken));
        ResponseEntity<String> adminResponse = restTemplate.exchange(url, HttpMethod.GET, adminRequest, String.class);
        Assertions.assertEquals(HttpStatus.OK, adminResponse.getStatusCode());

        HttpEntity<Void> customerRequest = new HttpEntity<>(createHeaders(customertoken));
        ResponseEntity<String> customerResponse = restTemplate.exchange(url, HttpMethod.GET, customerRequest, String.class);
        Assertions.assertEquals(HttpStatus.OK, customerResponse.getStatusCode());

        JsonNode responseBody = objectMapper.readTree(customerResponse.getBody());
        Assertions.assertEquals(orderId, responseBody.get("orderId").asLong());
    }

    @Test
    @Order(13)
    void backend_testGetOrdersByCustomerId() throws Exception {
        Assertions.assertNotNull(customerId, "customerId should not be null");
        String url = "/api/order/customer/" + customerId;

        HttpEntity<Void> request = new HttpEntity<>(createHeaders(customertoken));
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertTrue(objectMapper.readTree(response.getBody()).isArray());
    }

    // ---------- Reviews ----------

    @Test
    @Order(14)
    void backend_testCreateReviewWithRoleValidation() throws Exception {
        Assertions.assertNotNull(customerId, "customerId should not be null");

        String requestBody = "{\"subject\": \"Great course\", \"body\": \"Learned a lot from this course\", \"rating\": 5, \"dateCreated\": \"2026-09-07\", \"customer\": {\"customerId\": " + customerId + "}}";

        HttpEntity<String> customerRequest = new HttpEntity<>(requestBody, createHeaders(customertoken));
        ResponseEntity<String> customerResponse = restTemplate.exchange("/api/review", HttpMethod.POST, customerRequest, String.class);
        Assertions.assertEquals(HttpStatus.CREATED, customerResponse.getStatusCode());

        JsonNode responseBody = objectMapper.readTree(customerResponse.getBody());
        reviewId = responseBody.get("reviewId").asLong();
        Assertions.assertEquals("Great course", responseBody.get("subject").asText());
        Assertions.assertEquals(5, responseBody.get("rating").asInt());

        HttpEntity<String> adminRequest = new HttpEntity<>(requestBody, createHeaders(admintoken));
        ResponseEntity<String> adminResponse = restTemplate.exchange("/api/review", HttpMethod.POST, adminRequest, String.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN, adminResponse.getStatusCode());
    }

    @Test
    @Order(15)
    void backend_testGetAllReviewsWithRoleValidation() throws Exception {
        HttpEntity<Void> adminRequest = new HttpEntity<>(createHeaders(admintoken));
        ResponseEntity<String> adminResponse = restTemplate.exchange("/api/review", HttpMethod.GET, adminRequest, String.class);
        Assertions.assertEquals(HttpStatus.OK, adminResponse.getStatusCode());
        Assertions.assertTrue(objectMapper.readTree(adminResponse.getBody()).isArray());

        HttpEntity<Void> customerRequest = new HttpEntity<>(createHeaders(customertoken));
        ResponseEntity<String> customerResponse = restTemplate.exchange("/api/review", HttpMethod.GET, customerRequest, String.class);
        Assertions.assertEquals(HttpStatus.OK, customerResponse.getStatusCode());
    }

    @Test
    @Order(16)
    void backend_testGetReviewByIdWithRoleValidation() throws Exception {
        Assertions.assertNotNull(reviewId, "reviewId should not be null");
        String url = "/api/review/" + reviewId;

        HttpEntity<Void> adminRequest = new HttpEntity<>(createHeaders(admintoken));
        ResponseEntity<String> adminResponse = restTemplate.exchange(url, HttpMethod.GET, adminRequest, String.class);
        Assertions.assertEquals(HttpStatus.OK, adminResponse.getStatusCode());

        HttpEntity<Void> customerRequest = new HttpEntity<>(createHeaders(customertoken));
        ResponseEntity<String> customerResponse = restTemplate.exchange(url, HttpMethod.GET, customerRequest, String.class);
        Assertions.assertEquals(HttpStatus.OK, customerResponse.getStatusCode());
    }

   
}

    

  
   