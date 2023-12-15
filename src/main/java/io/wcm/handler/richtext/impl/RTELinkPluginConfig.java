/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
 * %%
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
 * #L%
 */
package io.wcm.handler.richtext.impl;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.wcm.handler.link.spi.LinkHandlerConfig;
import io.wcm.handler.link.spi.LinkType;
import io.wcm.sling.commons.adapter.AdaptTo;
import io.wcm.wcm.commons.contenttype.ContentType;
import io.wcm.wcm.commons.contenttype.FileExtension;

/**
 * Servlet providing RTE link plugin configuration in context of the referenced content page.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = NameConstants.NT_PAGE,
    selectors = RTELinkPluginConfig.SELECTOR,
    extensions = FileExtension.JSON,
    methods = HttpConstants.METHOD_GET)
public class RTELinkPluginConfig extends SlingSafeMethodsServlet {
  private static final long serialVersionUID = 1L;

  static final String SELECTOR = "wcmio-handler-richtext-rte-plugins-links-config";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {

    Resource resource = request.getResource();
    PageManager pageManager = AdaptTo.notNull(request.getResourceResolver(), PageManager.class);
    Page page = pageManager.getContainingPage(resource);
    I18n i18n = new I18n(request);

    LinkHandlerConfig linkHandlerConfig = AdaptTo.notNull(resource, LinkHandlerConfig.class);
    List<LinkType> linkTypes = linkHandlerConfig.getLinkTypes().stream()
        .map(linkTypeClass -> AdaptTo.notNull(resource, linkTypeClass))
        .filter(LinkType::hasRichTextPlugin)
        .collect(Collectors.toList());

    Result result = new Result();

    Map<String, LinkTypeConfig> linkTypesConfigs = new LinkedHashMap<>();
    for (LinkType linkType : linkTypes) {
      LinkTypeConfig linkTypeConfig = new LinkTypeConfig();
      linkTypeConfig.value = linkType.getId();
      linkTypeConfig.text = getI18nText("io.wcm.handler.link.components.granite.form.linkRefContainer." + linkType.getId() + ".type", i18n);
      linkTypesConfigs.put(linkType.getId(), linkTypeConfig);
    }
    result.linkTypes = linkTypesConfigs;

    Map<String, String> rootPaths = new LinkedHashMap<>();
    for (LinkType linkType : linkTypes) {
      String rootPath = linkHandlerConfig.getLinkRootPath(page, linkType.getId());
      if (rootPath != null) {
        rootPaths.put(linkType.getId(), rootPath);
      }
    }

    result.linkTypes = linkTypesConfigs;
    result.rootPaths = rootPaths;
    response.setContentType(ContentType.JSON);
    response.getWriter().write(OBJECT_MAPPER.writeValueAsString(result));
  }

  private String getI18nText(String key, I18n i18n) {
    try {
      return i18n.get(key);
    }
    catch (MissingResourceException ex) {
      return key;
    }
  }

  @JsonInclude(Include.NON_NULL)
  static class Result {

    private Map<String, LinkTypeConfig> linkTypes;
    private Map<String, String> rootPaths;

    public Map<String, LinkTypeConfig> getLinkTypes() {
      return this.linkTypes;
    }

    public Map<String, String> getRootPaths() {
      return this.rootPaths;
    }
  }

  @JsonInclude(Include.NON_NULL)
  static class LinkTypeConfig {
    private String value;
    private String text;

    public String getValue() {
      return this.value;
    }

    public String getText() {
      return this.text;
    }
  }

}
