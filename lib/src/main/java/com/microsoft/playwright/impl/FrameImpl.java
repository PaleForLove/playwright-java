/**
 * Copyright (c) Microsoft Corporation.
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

package com.microsoft.playwright.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.*;

import java.util.*;

import static com.microsoft.playwright.Frame.LoadState.*;
import static com.microsoft.playwright.impl.Serialization.deserialize;
import static com.microsoft.playwright.impl.Serialization.serializeArgument;
import static com.microsoft.playwright.impl.Utils.isFunctionBody;

public class FrameImpl extends ChannelOwner implements Frame {
  private String name;
  private String url;
  FrameImpl parentFrame;
  Set<FrameImpl> childFrames = new LinkedHashSet<>();
  private final Set<LoadState> loadStates = new HashSet<>();
  PageImpl page;
  boolean isDetached;

  FrameImpl(ChannelOwner parent, String type, String guid, JsonObject initializer) {
    super(parent, type, guid, initializer);

    name = initializer.get("name").getAsString();
    url = initializer.get("url").getAsString();
    if (initializer.has("parentFrame")) {
      parentFrame = connection.getExistingObject(initializer.getAsJsonObject("parentFrame").get("guid").getAsString());
      parentFrame.childFrames.add(this);
    }
    for (JsonElement item : initializer.get("loadStates").getAsJsonArray()) {
      loadStates.add(loadStateFromProtocol(item.getAsString()));
    }
  }

  private static LoadState loadStateFromProtocol(String value) {
    switch (value) {
      case "load": return LOAD;
      case "domcontentloaded": return DOMCONTENTLOADED;
      case "networkidle": return NETWORKIDLE;
      default: throw new RuntimeException("Unexpected value: " + value);
    }
  }

  private Object evaluate(String expression, Object arg, boolean forceExpression) {
    JsonObject params = new JsonObject();
    params.addProperty("expression", expression);
    params.addProperty("world", "main");
    if (!isFunctionBody(expression)) {
      forceExpression = true;
    }
    params.addProperty("isFunction", !forceExpression);
    params.add("arg", new Gson().toJsonTree(serializeArgument(arg)));
    JsonElement json = sendMessage("evaluateExpression", params);
    SerializedValue value = new Gson().fromJson(json.getAsJsonObject().get("value"), SerializedValue.class);
    return deserialize(value);
  }

  @Override
  public ElementHandle querySelector(String selector) {
    JsonObject params = new JsonObject();
    params.addProperty("selector", selector);
    JsonElement json = sendMessage("querySelector", params);
    JsonObject element = json.getAsJsonObject().getAsJsonObject("element");
    if (element == null) {
      return null;
    }
    return connection.getExistingObject(element.get("guid").getAsString());
  }

  @Override
  public List<ElementHandle> querySelectorAll(String selector) {
    return null;
  }

  @Override
  public Object evalOnSelector(String selector, String pageFunction, Object arg) {
    JsonObject params = new JsonObject();
    params.addProperty("selector", selector);
    params.addProperty("expression", pageFunction);
    params.addProperty("isFunction", isFunctionBody(pageFunction));
    params.add("arg", new Gson().toJsonTree(serializeArgument(arg)));
    JsonElement json = sendMessage("evalOnSelector", params);
    SerializedValue value = new Gson().fromJson(json.getAsJsonObject().get("value"), SerializedValue.class);
    return deserialize(value);
  }

  @Override
  public Object evalOnSelectorAll(String selector, String pageFunction, Object arg) {
    return null;
  }

  @Override
  public ElementHandle addScriptTag(AddScriptTagOptions options) {
    return null;
  }

  @Override
  public ElementHandle addStyleTag(AddStyleTagOptions options) {
    return null;
  }

  @Override
  public void check(String selector, CheckOptions options) {

  }

  @Override
  public List<Frame> childFrames() {
    return null;
  }


  @Override
  public void click(String selector, ClickOptions options) {
    if (options == null) {
      options = new ClickOptions();
    }
    JsonObject params = new Gson().toJsonTree(options).getAsJsonObject();
    params.addProperty("selector", selector);

    params.remove("button");
    if (options.button != null) {
      params.addProperty("button", Serialization.toProtocol(options.button));
    }

    params.remove("modifiers");
    if (options.modifiers != null) {
      params.add("modifiers", Serialization.toProtocol(options.modifiers));
    }

    sendMessage("click", params);
  }

  @Override
  public String content() {
    return null;
  }

  @Override
  public void dblclick(String selector, DblclickOptions options) {
    if (options == null) {
      options = new DblclickOptions();
    }
    JsonObject params = new Gson().toJsonTree(options).getAsJsonObject();
    params.addProperty("selector", selector);

    params.remove("button");
    if (options.button != null) {
      params.addProperty("button", Serialization.toProtocol(options.button));
    }

    params.remove("modifiers");
    if (options.modifiers != null) {
      params.add("modifiers", Serialization.toProtocol(options.modifiers));
    }

    sendMessage("dblclick", params);
  }

  @Override
  public void dispatchEvent(String selector, String type, Object eventInit, DispatchEventOptions options) {

  }

  @Override
  public Object evaluate(String pageFunction, Object arg) {
    return evaluate(pageFunction, arg, false);
  }

  @Override
  public JSHandle evaluateHandle(String pageFunction, Object arg) {
    JsonObject params = new JsonObject();
    params.addProperty("expression", pageFunction);
    params.addProperty("world", "main");
    params.addProperty("isFunction", isFunctionBody(pageFunction));
    params.add("arg", new Gson().toJsonTree(serializeArgument(arg)));
    JsonElement json = sendMessage("evaluateExpressionHandle", params);
    return connection.getExistingObject(json.getAsJsonObject().getAsJsonObject("handle").get("guid").getAsString());
  }

  @Override
  public void fill(String selector, String value, FillOptions options) {
    if (options == null) {
      options = new FillOptions();
    }
    JsonObject params = new Gson().toJsonTree(options).getAsJsonObject();
    params.addProperty("selector", selector);
    params.addProperty("value", value);
    sendMessage("fill", params);
  }

  @Override
  public void focus(String selector, FocusOptions options) {

  }

  @Override
  public ElementHandle frameElement() {
    return null;
  }

  @Override
  public String getAttribute(String selector, String name, GetAttributeOptions options) {
    return null;
  }


  private static String toProtocol(NavigateOptions.WaitUntil waitUntil) {
    return waitUntil.toString().toLowerCase();
  }

  @Override
  public ResponseImpl navigate(String url, NavigateOptions options) {
    if (options == null) {
      options = new NavigateOptions();
    }
    JsonObject params = new Gson().toJsonTree(options).getAsJsonObject();
    params.addProperty("url", url);
    if (options.waitUntil != null) {
      params.remove("waitUntil");
      params.addProperty("waitUntil", toProtocol(options.waitUntil));
    }
    JsonElement result = sendMessage("goto", params);
    JsonObject jsonResponse = result.getAsJsonObject().getAsJsonObject("response");
    if (jsonResponse == null) {
      return null;
    }
    return connection.getExistingObject(jsonResponse.get("guid").getAsString());
  }

  @Override
  public void hover(String selector, HoverOptions options) {

  }

  @Override
  public String innerHTML(String selector, InnerHTMLOptions options) {
    return null;
  }

  @Override
  public String innerText(String selector, InnerTextOptions options) {
    return null;
  }

  @Override
  public boolean isDetached() {
    return isDetached;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Page page() {
    return page;
  }

  @Override
  public Frame parentFrame() {
    return parentFrame;
  }

  @Override
  public void press(String selector, String key, PressOptions options) {

  }

  @Override
  public List<String> selectOption(String selector, String values, SelectOptionOptions options) {
    return null;
  }


  private static String toProtocol(SetContentOptions.WaitUntil waitUntil) {
    if (waitUntil == null) {
      waitUntil = SetContentOptions.WaitUntil.LOAD;
    }
    switch (waitUntil) {
      case DOMCONTENTLOADED: return "domcontentloaded";
      case LOAD: return "load";
      case NETWORKIDLE: return "networkidle";
      default: throw new RuntimeException("Unexpected value: " + waitUntil);
    }
  }

  @Override
  public void setContent(String html, SetContentOptions options) {
    if (options == null) {
      options = new SetContentOptions();
    }
    JsonObject params = new Gson().toJsonTree(options).getAsJsonObject();
    params.addProperty("html", html);
    params.remove("waitUntil");
    params.addProperty("waitUntil", toProtocol(options.waitUntil));
    sendMessage("setContent", params);
  }

  @Override
  public void setInputFiles(String selector, String files, SetInputFilesOptions options) {

  }

  @Override
  public String textContent(String selector, TextContentOptions options) {
    return null;
  }

  @Override
  public String title() {
    JsonElement json = sendMessage("title", new JsonObject());
    return json.getAsJsonObject().get("value").getAsString();
  }

  @Override
  public void type(String selector, String text, TypeOptions options) {

  }

  @Override
  public void uncheck(String selector, UncheckOptions options) {

  }

  @Override
  public String url() {
    return url;
  }

  @Override
  public JSHandle waitForFunction(String pageFunction, Object arg, WaitForFunctionOptions options) {
    return null;
  }

  @Override
  public void waitForLoadState(LoadState state, WaitForLoadStateOptions options) {
    if (state == null) {
      state = LOAD;
    }
    while (!loadStates.contains(state)) {
      // TODO: support timeout!
      connection.processOneMessage();
    }
  }

  @Override
  public Response waitForNavigation(WaitForNavigationOptions options) {
    return null;
  }

  @Override
  public ElementHandle waitForSelector(String selector, WaitForSelectorOptions options) {
    return null;
  }

  @Override
  public void waitForTimeout(int timeout) {

  }

  protected void handleEvent(String event, JsonObject params) {
    if ("loadstate".equals(event)) {
      JsonElement add = params.get("add");
      if (add != null) {
        loadStates.add(loadStateFromProtocol(add.getAsString()));
      }
      JsonElement remove = params.get("remove");
      if (remove != null) {
        loadStates.remove(loadStateFromProtocol(remove.getAsString()));
      }
    } else if ("navigated".equals(event)) {
      url = params.get("url").getAsString();
      name = params.get("name").getAsString();
    }
  }

}
