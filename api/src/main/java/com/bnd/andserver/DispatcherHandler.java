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
package com.bnd.andserver;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bnd.andserver.error.NotFoundException;
import com.bnd.andserver.error.ServerInternalException;
import com.bnd.andserver.framework.ExceptionResolver;
import com.bnd.andserver.framework.HandlerInterceptor;
import com.bnd.andserver.framework.MessageConverter;
import com.bnd.andserver.framework.ModifiedInterceptor;
import com.bnd.andserver.framework.body.StringBody;
import com.bnd.andserver.framework.config.Multipart;
import com.bnd.andserver.framework.handler.HandlerAdapter;
import com.bnd.andserver.framework.handler.RequestHandler;
import com.bnd.andserver.framework.view.View;
import com.bnd.andserver.framework.view.ViewResolver;
import com.bnd.andserver.http.HttpContext;
import com.bnd.andserver.http.HttpRequest;
import com.bnd.andserver.http.HttpResponse;
import com.bnd.andserver.http.RequestDispatcher;
import com.bnd.andserver.http.RequestWrapper;
import com.bnd.andserver.http.StandardContext;
import com.bnd.andserver.http.StandardRequest;
import com.bnd.andserver.http.StandardResponse;
import com.bnd.andserver.http.StatusCode;
import com.bnd.andserver.http.cookie.Cookie;
import com.bnd.andserver.http.multipart.MultipartRequest;
import com.bnd.andserver.http.multipart.MultipartResolver;
import com.bnd.andserver.http.multipart.StandardMultipartResolver;
import com.bnd.andserver.http.session.Session;
import com.bnd.andserver.http.session.SessionManager;
import com.bnd.andserver.http.session.StandardSessionManager;
import com.bnd.andserver.register.Register;
import com.bnd.andserver.util.Assert;

import org.apache.httpcore.protocol.HttpRequestHandler;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Zhenjie Yan on 2018/8/8.
 */
public class DispatcherHandler implements HttpRequestHandler, Register {

    private final Context mContext;

    private SessionManager mSessionManager;
    private MessageConverter mConverter;
    private ViewResolver mViewResolver;
    private ExceptionResolver mResolver;
    private Multipart mMultipart;

    private List<HandlerAdapter> mAdapterList = new LinkedList<>();
    private List<HandlerInterceptor> mInterceptorList = new LinkedList<>();

    public DispatcherHandler(Context context) {
        this.mContext = context;
        this.mSessionManager = new StandardSessionManager(context);
        this.mViewResolver = new ViewResolver();
        this.mResolver = new ExceptionResolver.ResolverWrapper(ExceptionResolver.DEFAULT);

        this.mInterceptorList.add(new ModifiedInterceptor());
    }

    @Override
    public void addAdapter(@NonNull HandlerAdapter adapter) {
        Assert.notNull(adapter, "The adapter cannot be null.");

        if (!mAdapterList.contains(adapter)) {
            mAdapterList.add(adapter);
        }
    }

    @Override
    public void addInterceptor(@NonNull HandlerInterceptor interceptor) {
        Assert.notNull(interceptor, "The interceptor cannot be null.");

        if (!mInterceptorList.contains(interceptor)) {
            mInterceptorList.add(interceptor);
        }
    }

    @Override
    public void setConverter(MessageConverter converter) {
        this.mConverter = converter;
        this.mViewResolver = new ViewResolver(converter);
    }

    @Override
    public void setResolver(@NonNull ExceptionResolver resolver) {
        Assert.notNull(resolver, "The exceptionResolver cannot be null.");

        this.mResolver = new ExceptionResolver.ResolverWrapper(resolver);
    }

    @Override
    public void setMultipart(Multipart multipart) {
        this.mMultipart = multipart;
    }

    @Override
    public void handle(org.apache.httpcore.HttpRequest req, org.apache.httpcore.HttpResponse res,
                       org.apache.httpcore.protocol.HttpContext con) {
        HttpRequest request = new StandardRequest(req, new StandardContext(con), this, mSessionManager);
        HttpResponse response = new StandardResponse(res);
        handle(request, response);
    }

    private void handle(HttpRequest request, HttpResponse response) {
        MultipartResolver multipartResolver = new StandardMultipartResolver();
        try {
            if (multipartResolver.isMultipart(request)) {
                configMultipart(multipartResolver);
                request = multipartResolver.resolveMultipart(request);
            }

            // Determine adapter for the current request.
            HandlerAdapter ha = getHandlerAdapter(request);
            if (ha == null) {
                throw new NotFoundException(request.getPath());
            }

            // Determine handler for the current request.
            RequestHandler handler = ha.getHandler(request);
            if (handler == null) {
                throw new NotFoundException(request.getPath());
            }

            // Pre processor, e.g. interceptor.
            if (preHandle(request, response, handler)) {
                return;
            }

            // Actually invoke the handler.
            request.setAttribute(HttpContext.ANDROID_CONTEXT, mContext);
            request.setAttribute(HttpContext.HTTP_MESSAGE_CONVERTER, mConverter);
            View view = handler.handle(request, response);
            mViewResolver.resolve(view, request, response);
            processSession(request, response);
        } catch (Throwable err) {
            try {
                mResolver.onResolve(request, response, err);
            } catch (Exception e) {
                e = new ServerInternalException(e);
                response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                response.setBody(new StringBody(e.getMessage()));
            }
            processSession(request, response);
        } finally {
            if (request instanceof MultipartRequest) {
                multipartResolver.cleanupMultipart((MultipartRequest) request);
            }
        }
    }

    private void configMultipart(MultipartResolver multipartResolver) {
        if (mMultipart != null) {
            long allFileMaxSize = mMultipart.getAllFileMaxSize();
            if (allFileMaxSize == -1 || allFileMaxSize > 0) {
                multipartResolver.setAllFileMaxSize(allFileMaxSize);
            }

            long fileMaxSize = mMultipart.getFileMaxSize();
            if (fileMaxSize == -1 || fileMaxSize > 0) {
                multipartResolver.setFileMaxSize(fileMaxSize);
            }

            int maxInMemorySize = mMultipart.getMaxInMemorySize();
            if (maxInMemorySize > 0) {
                multipartResolver.setMaxInMemorySize(maxInMemorySize);
            }

            File uploadTempDir = mMultipart.getUploadTempDir();
            if (uploadTempDir != null) {
                multipartResolver.setUploadTempDir(uploadTempDir);
            }
        }
    }

    /**
     * Return the {@link RequestHandler} for this request.
     *
     * @param request current HTTP request.
     *
     * @return the {@link RequestHandler}, or {@code null} if no handler could be found.
     */
    private HandlerAdapter getHandlerAdapter(HttpRequest request) {
        for (HandlerAdapter ha: mAdapterList) {
            if (ha.intercept(request)) {
                return ha;
            }
        }
        return null;
    }

    /**
     * Intercept the execution of a handler.
     *
     * @param request current request.
     * @param response current response.
     * @param handler the corresponding handler of the current request.
     *
     * @return true if the interceptor has processed the request and responded.
     */
    private boolean preHandle(HttpRequest request, HttpResponse response, RequestHandler handler) throws Exception {
        for (HandlerInterceptor interceptor: mInterceptorList) {
            if (interceptor.onIntercept(request, response, handler)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public RequestDispatcher getRequestDispatcher(final HttpRequest request, final String path) {
        HttpRequest copyRequest = request;
        while (copyRequest instanceof RequestWrapper) {
            RequestWrapper wrapper = (RequestWrapper) request;
            copyRequest = wrapper.getRequest();
        }

        StandardRequest newRequest = (StandardRequest) copyRequest;
        newRequest.setPath(path);

        HandlerAdapter ha = getHandlerAdapter(copyRequest);
        if (ha == null) {
            throw new NotFoundException(request.getPath());
        }

        return new RequestDispatcher() {
            @Override
            public void forward(@NonNull HttpRequest request, @NonNull HttpResponse response) {
                handle(request, response);
            }
        };
    }

    private void processSession(HttpRequest request, HttpResponse response) {
        Object objSession = request.getAttribute(HttpContext.REQUEST_CREATED_SESSION);
        if (objSession instanceof Session) {
            Session session = (Session) objSession;
            try {
                mSessionManager.add(session);
            } catch (IOException e) {
                Log.e(AndServer.TAG, "Session persistence failed.", e);
            }

            Cookie cookie = new Cookie(HttpRequest.SESSION_NAME, session.getId());
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }
    }
}