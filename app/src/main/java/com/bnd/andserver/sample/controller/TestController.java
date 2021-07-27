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
package com.bnd.andserver.sample.controller;

import com.alibaba.fastjson.JSON;
import com.bnd.andserver.annotation.Addition;
import com.bnd.andserver.annotation.CookieValue;
import com.bnd.andserver.annotation.CrossOrigin;
import com.bnd.andserver.annotation.FormPart;
import com.bnd.andserver.annotation.GetMapping;
import com.bnd.andserver.annotation.PathVariable;
import com.bnd.andserver.annotation.PostMapping;
import com.bnd.andserver.annotation.PutMapping;
import com.bnd.andserver.annotation.RequestBody;
import com.bnd.andserver.annotation.RequestMapping;
import com.bnd.andserver.annotation.RequestMethod;
import com.bnd.andserver.annotation.RequestParam;
import com.bnd.andserver.annotation.RestController;
import com.bnd.andserver.http.HttpRequest;
import com.bnd.andserver.http.HttpResponse;
import com.bnd.andserver.http.cookie.Cookie;
import com.bnd.andserver.http.multipart.MultipartFile;
import com.bnd.andserver.http.session.Session;
import com.bnd.andserver.sample.component.LoginInterceptor;
import com.bnd.andserver.sample.model.UserInfo;
import com.bnd.andserver.sample.util.FileUtils;
import com.bnd.andserver.sample.util.Logger;
import com.bnd.andserver.util.MediaType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Zhenjie Yan on 2018/6/9.
 */
@RestController
@RequestMapping(path = "/user")
class TestController {

    @RequestMapping(
        path = "/connection",
        method = {RequestMethod.GET},
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    Object getConnection(HttpRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("getLocalAddr", request.getLocalAddr());
        map.put("getLocalName", request.getLocalName());
        map.put("getLocalPort", request.getLocalPort());
        map.put("getRemoteAddr", request.getRemoteAddr());
        map.put("getRemoteHost", request.getRemoteHost());
        map.put("getRemotePort", request.getRemotePort());
        Logger.i(JSON.toJSONString(map));
        return map;
    }

    @CrossOrigin(
        methods = {RequestMethod.POST, RequestMethod.GET}
    )
    @RequestMapping(
        path = "/get/{userId}",
        method = {RequestMethod.PUT, RequestMethod.POST, RequestMethod.GET},
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    String info(@PathVariable(name = "userId") String userId, HttpRequest request) {
        return userId;
    }

    @PutMapping(path = "/get/{userId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    String modify(@PathVariable("userId") String userId, @RequestParam(name = "sex") String sex,
                  @RequestParam(name = "age") int age) {
        String message = "The userId is %1$s, and the sex is %2$s, and the age is %3$d.";
        return String.format(Locale.US, message, userId, sex, age);
    }

    @PostMapping(path = "/login", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    String login(HttpRequest request, HttpResponse response, @RequestParam(name = "account") String account,
                 @RequestParam(name = "password") String password) {
        Session session = request.getValidSession();
        session.setAttribute(LoginInterceptor.LOGIN_ATTRIBUTE, true);

        Cookie cookie = new Cookie("account", account + "=" + password);
        response.addCookie(cookie);
        return "Login successful.";
    }

    @Addition(stringType = "login", booleanType = true)
    @GetMapping(path = "/userInfo", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    UserInfo userInfo(@CookieValue("account") String account) {
        Logger.i("Account: " + account);
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("123");
        userInfo.setUserName("AndServer");
        return userInfo;
    }

    @PostMapping(path = "/upload", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    String upload(@RequestParam(name = "avatar") MultipartFile file) throws IOException {
        File localFile = FileUtils.createRandomFile(file);
        file.transferTo(localFile);
        return localFile.getAbsolutePath();
    }

    @GetMapping(path = "/consume", consumes = {"application/json", "!application/xml"})
    String consume() {
        return "Consume is successful";
    }

    @GetMapping(path = "/produce", produces = {"application/json; charset=utf-8"})
    String produce() {
        return "Produce is successful";
    }

    @GetMapping(path = "/include", params = {"name=123"})
    String include(@RequestParam(name = "name") String name) {
        return name;
    }

    @GetMapping(path = "/exclude", params = "name!=123")
    String exclude() {
        return "Exclude is successful.";
    }

    @GetMapping(path = {"/mustKey", "/getName"}, params = "name")
    String getMustKey(@RequestParam(name = "name") String name) {
        return name;
    }

    @PostMapping(path = {"/mustKey", "/postName"}, params = "name")
    String postMustKey(@RequestParam(name = "name") String name) {
        return name;
    }

    @GetMapping(path = "/noName", params = "!name")
    String noName() {
        return "NoName is successful.";
    }

    @PostMapping(path = "/formPart")
    UserInfo forPart(@FormPart(name = "user") UserInfo userInfo) {
        return userInfo;
    }

    @PostMapping(path = "/jsonBody")
    UserInfo jsonBody(@RequestBody UserInfo userInfo) {
        return userInfo;
    }

    @PostMapping(path = "/listBody")
    List<UserInfo> jsonBody(@RequestBody List<UserInfo> infoList) {
        return infoList;
    }
}