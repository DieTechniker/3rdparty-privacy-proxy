/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.routing;

import de.tk.opensource.privacyproxy.config.CookieNameMatchType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

/**
 * This is a example implementation of the RoutingHandler.
 * Please implement your own RoutingHandlers per provider and endpoint.
 */
public class ExampleRoutingHandler extends RoutingHandler {

    // TODO: add request mapping here
    @ResponseBody
    public ResponseEntity handleRequest(
            @RequestBody MultiValueMap<String, String> formData,
            HttpServletRequest request
    ) {
        return handlePostInternal(request, formData.toSingleValueMap(), getRoutingEndpoint());
    }

    protected String getRoutingEndpoint() {
        return ""; // TODO: return providers entdpoint URL
    }

    @Override
    protected String[] getWhitelistedRequestHeaders() {
        return new String[] { "User-Agent" }; // sample: will only pass thru the user agent header
    }

    @Override
    protected Map<String, String> getAdditionalRequestHeaders(HttpServletRequest request) {
        return Collections.singletonMap("env", ""); // sample: additional header for environment
    }

    @Override
    protected String[] getWhitelistedCookieNames() {
        return new String[] { "_example_pk_" }; // sample: cookie
    }

    @Override
    protected CookieNameMatchType getCookieNameMatchType() {
        return CookieNameMatchType.PREFIX;
    }
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/