/*
 * Copyright 2018 Zhenjie Yan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bnd.andserver.sample.component;

import androidx.annotation.NonNull;

import com.bnd.andserver.annotation.Interceptor;
import com.bnd.andserver.error.HttpException;
import com.bnd.andserver.framework.HandlerInterceptor;
import com.bnd.andserver.framework.handler.MethodHandler;
import com.bnd.andserver.framework.handler.RequestHandler;
import com.bnd.andserver.framework.mapping.Addition;
import com.bnd.andserver.http.HttpRequest;
import com.bnd.andserver.http.HttpResponse;
import com.bnd.andserver.http.session.Session;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Created by Zhenjie Yan on 2018/9/11.
 */
@Interceptor
public class LoginInterceptor implements HandlerInterceptor {

    public static final String LOGIN_ATTRIBUTE = "USER.LOGIN.SIGN";

    @Override
    public boolean onIntercept(@NonNull HttpRequest request, @NonNull HttpResponse response,
        @NonNull RequestHandler handler) {
        if (handler instanceof MethodHandler) {
            MethodHandler methodHandler = (MethodHandler) handler;
            Addition addition = methodHandler.getAddition();
            if (!isLogin(request, addition)) {
                throw new HttpException(401, "You are not logged in yet.");
            }
        }
        return false;
    }

    private boolean isNeedLogin(Addition addition) {
        if (addition == null) {
            return false;
        }

        String[] stringType = addition.getStringType();
        if (ArrayUtils.isEmpty(stringType)) {
            return false;
        }

        boolean[] booleanType = addition.getBooleanType();
        if (ArrayUtils.isEmpty(booleanType)) {
            return false;
        }
        return stringType[0].equalsIgnoreCase("login") && booleanType[0];
    }

    private boolean isLogin(HttpRequest request, Addition addition) {
        if (isNeedLogin(addition)) {
            Session session = request.getSession();
            if (session != null) {
                Object o = session.getAttribute(LOGIN_ATTRIBUTE);
                return o instanceof Boolean && (boolean) o;
            }
            return false;
        }
        return true;
    }
}