package de.tk.opensource.privacyproxy.routing;

import de.tk.opensource.privacyproxy.config.TestConfig;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SpringBootTest(classes = TestConfig.class)
class RoutingHandlerSpringTest {

    private MockRestServiceServer mockServer;

    @Autowired
    private MyRoutingHandler routingHandler;

    @Autowired
    private RestTemplate restTemplate;

    private static void requestTo(
            final ClientHttpRequest request,
            final String expectedUrl,
            final Map<String, String> queryParams
    ) {

        final String requestUrl =
                UriComponentsBuilder.fromUri(request.getURI()).replaceQuery(null).build(true)
                        .toString();
        assertEquals(expectedUrl, requestUrl);

        final Set<Map.Entry<String, String>> queryParamsEntries = queryParams.entrySet();
        final Map<String, List<String>> queryParamsMultiValueMap =
                queryParamsEntries.stream().collect(
                        Collectors.toMap(Map.Entry::getKey, e -> Collections.singletonList(e.getValue()))
                );
        final MultiValueMap<String, String> queryParamsMultiValue =
                new MultiValueMapAdapter<>(queryParamsMultiValueMap);

        final MultiValueMap<String, String> requestQueryParams =
                UriComponentsBuilder.fromUri(request.getURI()).build(true).getQueryParams();
        assertEquals(queryParamsMultiValue, requestQueryParams);
    }

    @BeforeEach
    public void setUp() throws Exception {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testHandleGenericRequestInternalPost() throws URISyntaxException, IOException {
        final String responseBodyString = "[1,2,3,4,5,6]";
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("id", "1234");
        queryParams.put("WTX", "XYZ");

        mockServer.expect(r -> requestTo(r, "https://localhost/1337/batch", queryParams)).andExpect(
                        content().string("endpoint?key=1337,XXX&key2=YYYY")
                )
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.IMAGE_GIF).body(
                                responseBodyString.getBytes()
                        )
                );

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final ResponseEntity<Resource> responseEntity =
                routingHandler.handleGenericRequestInternal(
                        "https://localhost/1337/batch",
                        queryParams,
                        request,
                        "endpoint?key=1337,XXX&key2=YYYY",
                        HttpMethod.POST
                );

        mockServer.verify();
        assertThat(responseEntity, notNullValue());
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(
                IOUtils.toString(responseEntity.getBody().getInputStream(), StandardCharsets.UTF_8),
                equalTo(responseBodyString)
        );
    }

    @Test
    void testHandleGenericRequestInternalGet() throws URISyntaxException, IOException {
        final String responseBodyString = "[1,2,3,4,5,6]";
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("id", "1234");
        queryParams.put("WTX", "XYZ");

        mockServer.expect(r -> requestTo(r, "https://localhost/1337/wt", queryParams)).andExpect(
                        method(HttpMethod.GET)
                )
                .andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.IMAGE_GIF).body(
                                responseBodyString.getBytes()
                        )
                );

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final ResponseEntity<Resource> responseEntity =
                routingHandler.handleGenericRequestInternal(
                        "https://localhost/1337/wt",
                        queryParams,
                        request,
                        null,
                        HttpMethod.GET
                );

        mockServer.verify();
        assertThat(responseEntity, notNullValue());
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(
                IOUtils.toString(responseEntity.getBody().getInputStream(), StandardCharsets.UTF_8),
                equalTo(responseBodyString)
        );
    }

    @Test
    void testHandleGenericRequestInternalForbidden() throws URISyntaxException, IOException {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("id", "1234");
        queryParams.put("WTX", "XYZ");

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final ResponseEntity<Resource> responseEntity =
                routingHandler.handleGenericRequestInternal(
                        "https://localhost/1337/wt",
                        queryParams,
                        request,
                        "endpoint?key=1337,XXX&key2=YYYY",
                        HttpMethod.GET
                );

        assertThat(responseEntity, notNullValue());
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.FORBIDDEN));
    }
}