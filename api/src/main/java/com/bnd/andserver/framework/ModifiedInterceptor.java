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
package com.bnd.andserver.framework;

import android.util.Log;

import androidx.annotation.NonNull;

import com.bnd.andserver.AndServer;
import com.bnd.andserver.http.HttpMethod;
import com.bnd.andserver.http.HttpRequest;
import com.bnd.andserver.http.HttpResponse;
import com.bnd.andserver.http.Modified;
import com.bnd.andserver.framework.handler.RequestHandler;

/**
 * Created by Zhenjie Yan on 2018/9/14.
 */
public class ModifiedInterceptor implements HandlerInterceptor {

    @Override
    public boolean onIntercept(@NonNull HttpRequest request, @NonNull HttpResponse response,
                               @NonNull RequestHandler handler) {
        // Process cache header, if supported by the handler.
        HttpMethod method = request.getMethod();
        if (method == HttpMethod.GET || method == HttpMethod.HEAD) {
            String eTag = null;
            try {
                eTag = handler.getETag(request);
            } catch (Throwable e) {
                Log.w(AndServer.TAG, e);
            }
            long lastModified = -1;
            try {
                lastModified = handler.getLastModified(request);
            } catch (Throwable e) {
                Log.w(AndServer.TAG, e);
            }
            return new Modified(request, response).process(eTag, lastModified);
        }
        return false;
    }
}