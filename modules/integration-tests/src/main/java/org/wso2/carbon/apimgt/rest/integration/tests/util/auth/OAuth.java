/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.apimgt.rest.integration.tests.util.auth;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.RetryableException;
import org.wso2.carbon.apimgt.rest.integration.tests.util.TestUtil;
import org.wso2.carbon.apimgt.rest.integration.tests.util.TokenInfo;

/**
 * Interceptor class for handle OAuth Bearer Token with given username,password,scopes.
 */

public class OAuth implements RequestInterceptor {
    private String username;
    private String password;
    private String scopes;

    private volatile TokenInfo tokenInfo;

    public OAuth(String username, String password, String scopes) {
        this.username = username;
        this.password = password;
        this.scopes = scopes;
    }

    @Override
    public void apply(RequestTemplate template) {
        // If the request already have an authorization (eg. Basic auth), do nothing
        if (template.headers().containsKey("Authorization")) {
            return;
        }
        // If first time, get the token
        if (tokenInfo == null) {
            generateFirstToken();
        } else if (System.currentTimeMillis() - tokenInfo.getExpiryTime() >= 1000) {
            generateTokenFromRefreshGrant();
        }
        if (getAccessToken() != null) {
            template.header("Authorization", "Bearer " + getAccessToken());
        }
    }

    public synchronized void generateFirstToken() {
        try {
            tokenInfo = TestUtil.generateToken(username, password, scopes);
        } catch (Exception e) {
            throw new RetryableException(e.getMessage(), e, null);
        }
    }

    public synchronized void generateTokenFromRefreshGrant() {
        try {
            tokenInfo = TestUtil.generateToken(scopes, tokenInfo.getRefreshToken());
        } catch (Exception e) {
            throw new RetryableException(e.getMessage(), e, null);
        }
    }


    public synchronized String getAccessToken() {
        return tokenInfo.getToken();
    }

}
