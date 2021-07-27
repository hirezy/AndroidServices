/*
 * Copyright © 2018 Zhenjie Yan.
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
package com.bnd.andserver.framework.handler;

import androidx.annotation.NonNull;

import com.bnd.andserver.framework.ETag;
import com.bnd.andserver.framework.LastModified;
import com.bnd.andserver.framework.view.View;
import com.bnd.andserver.http.HttpRequest;
import com.bnd.andserver.http.HttpResponse;

/**
 * Created by Zhenjie Yan on 2018/8/28.
 */
public interface RequestHandler extends ETag, LastModified {

    /**
     * Use the given handler to handle this request.
     *
     * @param request current request.
     * @param response current response.
     *
     * @return the impression sent to the client.
     */
    View handle(@NonNull HttpRequest request, @NonNull HttpResponse response) throws Throwable;
}