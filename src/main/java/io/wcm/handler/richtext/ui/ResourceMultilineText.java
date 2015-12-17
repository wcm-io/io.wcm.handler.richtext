/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
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
package io.wcm.handler.richtext.ui;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import io.wcm.handler.richtext.RichTextHandler;
import io.wcm.handler.richtext.RichTextNameConstants;
import io.wcm.handler.richtext.TextMode;

/**
 * Generic resource-based model for rendering plain text with line breaks.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class ResourceMultilineText {

  @RequestAttribute(optional = true)
  @Default(values = RichTextNameConstants.PN_TEXT)
  private String propertyName;

  @Self
  private RichTextHandler richTextHandler;
  @SlingObject
  private Resource resource;

  private String markup;

  @PostConstruct
  protected void activate() {
    String plainTextString = resource.getValueMap().get(propertyName, String.class);
    markup = richTextHandler.get(plainTextString).textMode(TextMode.PLAIN).buildMarkup();
  }

  /**
   * @return Text is valid
   */
  public boolean isValid() {
    return StringUtils.isNotBlank(getMarkup());
  }

  /**
   * @return Text markup
   */
  public String getMarkup() {
    return markup;
  }

}
