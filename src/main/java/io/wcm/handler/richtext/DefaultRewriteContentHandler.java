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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;

import io.wcm.handler.link.Link;
import io.wcm.handler.link.LinkHandler;
import io.wcm.handler.link.LinkNameConstants;
import io.wcm.handler.link.SyntheticLinkResource;
import io.wcm.handler.link.spi.LinkHandlerConfig;
import io.wcm.handler.link.spi.LinkType;
import io.wcm.handler.link.type.InternalLinkType;
import io.wcm.handler.link.type.MediaLinkType;
import io.wcm.handler.media.Media;
import io.wcm.handler.media.MediaHandler;
import io.wcm.handler.richtext.impl.DataPropertyUtil;
import io.wcm.handler.richtext.util.RewriteContentHandler;
import io.wcm.sling.commons.adapter.AdaptTo;
import io.wcm.wcm.commons.contenttype.FileExtension;

/**
 * Default implementation of {@link RewriteContentHandler}.
 */
@Model(adaptables = { SlingHttpServletRequest.class, Resource.class })
@SuppressWarnings("deprecation")
public final class DefaultRewriteContentHandler implements RewriteContentHandler {

  @Self
  private Adaptable adaptable;
  @SlingObject
  private ResourceResolver resourceResolver;
  @Self
  private LinkHandler linkHandler;
  @Self
  private LinkHandlerConfig linkHandlerConfig;
  @Self
  private MediaHandler mediaHandler;

  private static final Logger log = LoggerFactory.getLogger(DefaultRewriteContentHandler.class);

  /**
   * List of all tag names that should not be rendered "self-closing" to avoid interpretation errors in browsers
   */
  private static final Set<String> NONSELFCLOSING_TAGS = Set.of(
      "div",
      "span",
      "strong",
      "em",
      "b",
      "i",
      "ul",
      "ol",
      "li"
      );

  /**
   * Checks if the given element has to be rewritten.
   * Is called for every child single element of the parent given to rewriteContent method.
   * @param element Element to check
   * @return null if nothing is to do with this element.
   *         Return empty list to remove this element.
   *         Return list with other content to replace element with new content.
   */
  @Override
  @SuppressWarnings({ "PMD.ReturnEmptyCollectionRatherThanNull", "java:S1168" })
  public List<Content> rewriteElement(@NotNull Element element) {

    // rewrite anchor elements
    if (StringUtils.equalsIgnoreCase(element.getName(), "a")) {
      return rewriteAnchor(element);
    }

    // rewrite image elements
    else if (StringUtils.equalsIgnoreCase(element.getName(), "img")) {
      return rewriteImage(element);
    }

    // detect BR elements and turn those into "self-closing" elements
    // since the otherwise generated <br> </br> structures are illegal and
    // are not handled correctly by Internet Explorers
    else if (StringUtils.equalsIgnoreCase(element.getName(), "br")) {
      if (!element.getContent().isEmpty()) {
        element.removeContent();
      }
      return null;
    }

    // detect empty elements and insert at least an empty string to avoid "self-closing" elements
    // that are not handled correctly by most browsers
    else if (NONSELFCLOSING_TAGS.contains(StringUtils.lowerCase(element.getName()))) {
      if (element.getContent().isEmpty()) {
        element.setText("");
      }
      return null;
    }

    return null;
  }

  /**
   * Checks if the given anchor element has to be rewritten.
   * @param element Element to check
   * @return null if nothing is to do with this element.
   *         Return empty list to remove this element.
   *         Return list with other content to replace element with new content.
   */
  private List<Content> rewriteAnchor(Element element) {

    // detect empty anchor elements and insert at least an empty string to avoid "self-closing" elements
    // that are not handled correctly by most browsers
    if (element.getContent().isEmpty()) {
      element.setText("");
    }

    // resolve link metadata from DOM element
    Link link = getAnchorLink(element);

    // build anchor for link metadata
    Element anchorElement = buildAnchorElement(link, element);

    // Replace anchor tag or remove anchor tag if invalid - add any sub-content in every case
    List<Content> content = new ArrayList<>();
    if (anchorElement != null) {
      anchorElement.addContent(element.cloneContent());
      content.add(anchorElement);
    }
    else {
      content.addAll(element.getContent());
    }
    return content;
  }

  /**
   * Extracts link metadata from the DOM elements attributes and resolves them to a {@link Link} object.
   * @param element DOM element
   * @return Link metadata
   */
  private Link getAnchorLink(Element element) {
    Resource currentResource = getCurrentResource();
    if (currentResource == null) {
      return linkHandler.invalid();
    }

    SyntheticLinkResource resource = new SyntheticLinkResource(resourceResolver,
        currentResource.getPath() + "/$link$");
    ValueMap resourceProps = resource.getValueMap();

    // get link metadata from data element
    boolean foundMetadata = getAnchorMetadataFromData(resourceProps, element);
    if (!foundMetadata) {
      // support for legacy metadata stored in single "data" attribute
      foundMetadata = getAnchorLegacyMetadataFromSingleData(resourceProps, element);
      if (!foundMetadata) {
        // support for legacy metadata stored in rel attribute
        getAnchorLegacyMetadataFromRel(resourceProps, element);
      }
    }

    // build anchor via linkhandler
    return linkHandler.get(resource).build();
  }

  /**
   * Builds anchor element for given link metadata.
   * @param link Link metadata
   * @param element Original element
   * @return Anchor element or null if link is invalid
   */
  private Element buildAnchorElement(Link link, Element element) {
    if (link.isValid()) {
      return link.getAnchor();
    }
    else if ((element.getAttributeValue("id") != null || element.getAttributeValue("name") != null) && element.getAttributeValue("src") == null) {
      // not a valid link, but it seems to be a named anchor - keep it
      // support both id attribute (valid in HTML4+HTML5) and the name attribute (only valid in HTML4)
      return element;
    }
    else {
      return null;
    }
  }

  /**
   * Support data structures where link metadata is stored in mutliple HTML5 data-* attributes.
   * @param resourceProps ValueMap to write link metadata to
   * @param element Link element
   * @return true if any metadata attribute was found
   */
  private boolean getAnchorMetadataFromData(ValueMap resourceProps, Element element) {
    boolean foundAny = false;

    List<Attribute> attributes = element.getAttributes();
    for (Attribute attribute : attributes) {
      if (DataPropertyUtil.isHtml5DataName(attribute.getName())) {
        String value = attribute.getValue();
        if (StringUtils.isNotEmpty(value)) {
          String property = DataPropertyUtil.toHeadlessCamelCaseName(attribute.getName());
          if (StringUtils.startsWith(value, "[") && StringUtils.endsWith(value, "]")) {
            try {
              JSONArray jsonArray = new JSONArray(value);
              String[] values = new String[jsonArray.length()];
              for (int i = 0; i < jsonArray.length(); i++) {
                values[i] = jsonArray.optString(i);
              }
              resourceProps.put(property, values);
            }
            catch (JSONException ex) {
              // ignore
            }
          }
          else {
            resourceProps.put(property, value);
          }
          foundAny = true;
        }
      }
    }

    return foundAny;
  }

  /**
   * Support legacy data structures where link metadata is stored as JSON fragment in single HTML5 data attribute.
   * @param resourceProps ValueMap to write link metadata to
   * @param element Link element
   */
  private boolean getAnchorLegacyMetadataFromSingleData(ValueMap resourceProps, Element element) {
    boolean foundAny = false;

    JSONObject metadata = null;
    Attribute dataAttribute = element.getAttribute("data");
    if (dataAttribute != null) {
      String metadataString = dataAttribute.getValue();
      if (StringUtils.isNotEmpty(metadataString)) {
        try {
          metadata = new JSONObject(metadataString);
        }
        catch (JSONException ex) {
          log.debug("Invalid link metadata: {}", metadataString, ex);
        }
      }
    }
    if (metadata != null) {
      JSONArray names = metadata.names();
      for (int i = 0; i < names.length(); i++) {
        String name = names.optString(i);
        resourceProps.put(name, metadata.opt(name));
        foundAny = true;
      }
    }

    return foundAny;
  }

  /**
   * Support legacy data structures where link metadata is stored as JSON fragment in rel attribute.
   * @param resourceProps ValueMap to write link metadata to
   * @param element Link element
   */
  private void getAnchorLegacyMetadataFromRel(ValueMap resourceProps, Element element) {
    // Check href attribute - do not change elements with no href or links to anchor names
    String href = element.getAttributeValue("href");
    String linkWindowTarget = element.getAttributeValue("target");
    if (href == null || href.startsWith("#")) {
      return;
    }

    // get link metadata from rel element
    JSONObject metadata = null;
    String metadataString = element.getAttributeValue("rel");
    if (StringUtils.isNotEmpty(metadataString)) {
      try {
        metadata = new JSONObject(metadataString);
      }
      catch (JSONException ex) {
        log.debug("Invalid link metadata: {}", metadataString, ex);
      }
    }
    if (metadata == null) {
      metadata = new JSONObject();
    }

    // transform link metadata to virtual JCR resource with JCR properties
    JSONArray metadataPropertyNames = metadata.names();
    if (metadataPropertyNames != null) {
      for (int i = 0; i < metadataPropertyNames.length(); i++) {
        String metadataPropertyName = metadataPropertyNames.optString(i);

        // check if value is array
        JSONArray valueArray = metadata.optJSONArray(metadataPropertyName);
        if (valueArray != null) {
          // store array values
          List<String> values = new ArrayList<>();
          for (int j = 0; j < valueArray.length(); j++) {
            values.add(valueArray.optString(j));
          }
          resourceProps.put(metadataPropertyName, values.toArray(new String[0]));
        }
        else {
          // store simple value
          Object value = metadata.opt(metadataPropertyName);
          if (value != null) {
            resourceProps.put(metadataPropertyName, value);
          }
        }
      }
    }

    // detect link type
    LinkType linkType = null;
    String linkTypeString = resourceProps.get(LinkNameConstants.PN_LINK_TYPE, String.class);
    for (Class<? extends LinkType> candidateClass : linkHandlerConfig.getLinkTypes()) {
      LinkType candidate = AdaptTo.notNull(adaptable, candidateClass);
      if (StringUtils.isNotEmpty(linkTypeString)) {
        if (StringUtils.equals(linkTypeString, candidate.getId())) {
          linkType = candidate;
          break;
        }
      }
      else if (candidate.accepts(href)) {
        linkType = candidate;
        break;
      }
    }
    if (linkType == null) {
      // skip further processing if link type was not detected
      return;
    }

    // workaround: strip off ".html" extension if it was added automatically by the RTE
    if (linkType instanceof InternalLinkType || linkType instanceof MediaLinkType) {
      String htmlSuffix = "." + FileExtension.HTML;
      if (StringUtils.endsWith(href, htmlSuffix)) {
        href = StringUtils.substringBeforeLast(href, htmlSuffix);
      }
    }

    // store link reference (property depending on link type)
    resourceProps.put(linkType.getPrimaryLinkRefProperty(), href);
    resourceProps.put(LinkNameConstants.PN_LINK_WINDOW_TARGET, linkWindowTarget);

  }

  /**
   * Checks if the given image element has to be rewritten.
   * @param element Element to check
   * @return null if nothing is to do with this element.
   *         Return empty list to remove this element.
   *         Return list with other content to replace element with new content.
   */
  private List<Content> rewriteImage(Element element) {

    // resolve media metadata from DOM element
    Media media = getImageMedia(element);

    // build image for media metadata
    Element imageElement = buildImageElement(media, element);

    // return modified element
    List<Content> content = new ArrayList<>();
    if (imageElement != null) {
      content.add(imageElement);
    }
    return content;
  }

  /**
   * Extracts media metadata from the DOM element attributes and resolves them to a {@link Media} object.
   * @param element DOM element
   * @return Media metadata
   */
  private Media getImageMedia(Element element) {
    String ref = element.getAttributeValue("src");
    if (StringUtils.isNotEmpty(ref)) {
      ref = unexternalizeImageRef(ref);
    }
    return mediaHandler.get(ref).build();
  }

  /**
   * Builds image element for given media metadata.
   * @param media Media metadata
   * @param element Original element
   * @return Image element or null if media reference is invalid
   */
  private Element buildImageElement(Media media, Element element) {
    if (media.isValid()) {
      element.setAttribute("src", media.getUrl());
    }
    return element;
  }

  /**
   * Converts the RTE externalized form of media reference to internal form.
   * @param ref Externalize media reference
   * @return Internal media reference
   */
  private String unexternalizeImageRef(String ref) {
    String unexternalizedRef = ref;

    if (StringUtils.isNotEmpty(unexternalizedRef)) {

      // decode if required
      unexternalizedRef = decodeIfEncoded(unexternalizedRef);

      // TODO: implementation has to be aligned with MediaSource implementations!
      // remove default servlet extension that is needed for inline images in RTE
      unexternalizedRef = StringUtils.removeEnd(unexternalizedRef, "/" + JcrConstants.JCR_CONTENT + ".default");
      unexternalizedRef = StringUtils.removeEnd(unexternalizedRef, "/_jcr_content.default");
    }

    return unexternalizedRef;
  }

  /**
   * URL-decode value if required.
   * @param value Probably encoded value.
   * @return Decoded value
   */
  private String decodeIfEncoded(String value) {
    if (StringUtils.contains(value, "%")) {
      try {
        return URLDecoder.decode(value, CharEncoding.UTF_8);
      }
      catch (UnsupportedEncodingException ex) {
        throw new RuntimeException(ex);
      }
    }
    return value;
  }

  @Override
  @SuppressWarnings({ "PMD.ReturnEmptyCollectionRatherThanNull", "java:S1168" })
  public List<Content> rewriteText(@NotNull Text text) {
    // nothing to do with text element
    return null;
  }

  private @Nullable Resource getCurrentResource() {
    if (adaptable instanceof Resource) {
      return (Resource)adaptable;
    }
    if (adaptable instanceof SlingHttpServletRequest) {
      return ((SlingHttpServletRequest)adaptable).getResource();
    }
    return null;
  }

}
