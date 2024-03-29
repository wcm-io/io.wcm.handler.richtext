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
package io.wcm.handler.richtext;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jdom2.Content;
import org.jdom2.output.Format;
import org.jdom2.output.Format.TextMode;
import org.jdom2.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

import io.wcm.wcm.commons.util.ToStringStyle;

/**
 * Holds information about a rich text processed and resolved by {@link RichTextHandler}.
 */
@ProviderType
public final class RichText {

  private static final Format JDOM_FORMAT = Format.getCompactFormat().setTextMode(TextMode.PRESERVE);

  private final @NotNull RichTextRequest richTextRequest;
  private final List<Content> content;

  /**
   * @param richTextRequest Rich text request
   * @param content Content
   */
  public RichText(@NotNull RichTextRequest richTextRequest, @Nullable List<Content> content) {
    this.richTextRequest = richTextRequest;
    this.content = content;
  }

  /**
   * @return Rich text request
   */
  public @NotNull RichTextRequest getRichTextRequest() {
    return this.richTextRequest;
  }

  /**
   * @return True if rich text is valid and not empty
   */
  public boolean isValid() {
    return this.content != null && !this.content.isEmpty();
  }

  /**
   * @return Formatted markup as DOM elements or empty collection.
   */
  public @Nullable Collection<Content> getContent() {
    return this.content;
  }

  /**
   * @return Formatted markup
   */
  public @Nullable String getMarkup() {
    if (!isValid()) {
      return null;
    }
    return new XMLOutputter(JDOM_FORMAT).outputString(this.content);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_OMIT_NULL_STYLE);
  }

}
