package com.pharma.erp.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param loginPageUrl where {@code /}, {@code /redirect} and failed legacy logins send the browser
 *     (env {@code PHARMA_AUTH_LOGIN_PAGE_URL}); relative to the backend origin unless absolute
 * @param cookieSecure whether the {@code token} cookie carries the {@code Secure} flag
 *     (env {@code PHARMA_AUTH_COOKIE_SECURE}); on by default (browsers accept it on
 *     {@code http://localhost}), disable only for plain-HTTP deployments on other hosts
 */
@ConfigurationProperties(prefix = "pharma.auth")
public record AuthProperties(
        @DefaultValue("/signin") String loginPageUrl, @DefaultValue("true") boolean cookieSecure) {}
