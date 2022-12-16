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
package io.wcm.handler.richtext.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.handler.richtext.DefaultRewriteContentHandler;
import io.wcm.handler.richtext.RichText;
import io.wcm.handler.richtext.RichTextHandler;
import io.wcm.handler.richtext.RichTextNameConstants;
import io.wcm.handler.richtext.TextMode;
import io.wcm.handler.richtext.spi.RichTextHandlerConfig;
import io.wcm.handler.richtext.testcontext.AppAemContext;
import io.wcm.handler.richtext.util.RewriteContentHandler;
import io.wcm.sling.commons.adapter.AdaptTo;
import io.wcm.sling.commons.resource.ImmutableValueMap;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

/**
 * Test {@link RichTextHandler}
 */
@ExtendWith(AemContextExtension.class)
@SuppressWarnings("null")
class RichTextHandlerImplTest {

  final AemContext context = AppAemContext.newAemContext();

  private static final String RICHTEXT_FRAGMENT = "<p>Der <strong>Jodelkaiser</strong> "
      + "aus<span/> dem "
      + "<a href='#' data-link-type='external' data-link-external-ref='http://www.jodelkaiser.de' "
      + "data-link-window-target='_blank' data-link-window-features='[&quot;resizable&quot;,&quot;toolbar&quot;]'>Ötztal</a> "
      + "ist wieder daheim.</p>";

  private static final String RICHTEXT_FRAGMENT_LEGACY_DATA = "<p>Der <strong>Jodelkaiser</strong> "
      + "aus<span/> dem "
      + "<a href='#' data='{\"linkType\":\"external\",\"linkExternalRef\":\"http://www.jodelkaiser.de\","
      + "\"linkWindowTarget\":\"_blank\",\"linkWindowFeatures\":[\"resizable\",\"toolbar\"]}'>Ötztal</a> "
      + "ist wieder daheim.</p>";

  private static final String RICHTEXT_FRAGMENT_LEGACY_REL = "<p>Der <strong>Jodelkaiser</strong> "
      + "aus<span/> dem "
      + "<a href=\"http://www.jodelkaiser.de\" target=\"_blank\" rel=\"{linktype:'external',linkwindowfeatures:['resizable','toolbar']}\">Ötztal</a> "
      + "ist wieder daheim.</p>";

  private static final String RICHTEXT_FRAGMENT_REWRITTEN = "<p>Der <strong>Jodelkaiser</strong> "
      + "aus<span></span> dem "
      + "<a href=\"http://www.jodelkaiser.de\" target=\"_blank\">Ötztal</a> "
      + "ist wieder daheim.</p>";

  private static final String RICHTEXT_FRAGMENT_REWRITTEN_CUSTOM_REWRITER = "<P>der <STRONG>jodelkaiser</STRONG> "
      + "aus<SPAN></SPAN> dem "
      + "<A href=\"http://www.jodelkaiser.de\" target=\"_blank\">ötztal</A> "
      + "ist wieder daheim.</P>";

  private static final String RICHTEXT_FRAGMENT_NAMED_ANCHOR_NAME = "<p><a name=\"anchor1\"></a>Der Jodelkaiser</p>";
  private static final String RICHTEXT_FRAGMENT_NAMED_ANCHOR_NAME_REWRITTEN = "<p><a name=\"anchor1\"></a>Der Jodelkaiser</p>";

  private static final String RICHTEXT_FRAGMENT_NAMED_ANCHOR_ID = "<p><a id=\"anchor1\"></a>Der Jodelkaiser</p>";
  private static final String RICHTEXT_FRAGMENT_NAMED_ANCHOR_ID_REWRITTEN = "<p><a id=\"anchor1\"></a>Der Jodelkaiser</p>";

  private static final String PLAINTEXT_FRAGMENT = "Der Jodelkaiser\naus dem Ötztal\nist wieder daheim.";
  private static final String PLAINTEXT_FRAGMENT_REWRITTEN = "Der Jodelkaiser<br />aus dem Ötztal<br />ist wieder daheim.";

  protected Adaptable adaptable() {
    return context.request();
  }

  @Test
  void testNull() {
    RichTextHandler richTextHandler = AdaptTo.notNull(adaptable(), RichTextHandler.class);
    RichText richText = richTextHandler.get((String)null).build();
    assertNull(richText.getMarkup());
  }

  @Test
  void testContentIllegal() {
    RichTextHandler richTextHandler = AdaptTo.notNull(adaptable(), RichTextHandler.class);
    RichText richText = richTextHandler.get("<wurst").build();
    assertNull(richText.getMarkup());
  }

  @Test
  void testContent() {
    RichTextHandler richTextHandler = AdaptTo.notNull(adaptable(), RichTextHandler.class);
    RichText richText = richTextHandler.get(RICHTEXT_FRAGMENT).build();
    assertEquals(RICHTEXT_FRAGMENT_REWRITTEN, richText.getMarkup());
  }

  @Test
  void testContent_LegacyData() {
    RichTextHandler richTextHandler = AdaptTo.notNull(adaptable(), RichTextHandler.class);
    RichText richText = richTextHandler.get(RICHTEXT_FRAGMENT_LEGACY_DATA).build();
    assertEquals(RICHTEXT_FRAGMENT_REWRITTEN, richText.getMarkup());
  }

  @Test
  void testContent_LegacyRel() {
    RichTextHandler richTextHandler = AdaptTo.notNull(adaptable(), RichTextHandler.class);
    RichText richText = richTextHandler.get(RICHTEXT_FRAGMENT_LEGACY_REL).build();
    assertEquals(RICHTEXT_FRAGMENT_REWRITTEN, richText.getMarkup());
  }

  @Test
  void testContent_NamedAnchor_Name() {
    RichTextHandler richTextHandler = AdaptTo.notNull(adaptable(), RichTextHandler.class);
    RichText richText = richTextHandler.get(RICHTEXT_FRAGMENT_NAMED_ANCHOR_NAME).build();
    assertEquals(RICHTEXT_FRAGMENT_NAMED_ANCHOR_NAME_REWRITTEN, richText.getMarkup());
  }

  @Test
  void testContent_NamedAnchor_Id() {
    RichTextHandler richTextHandler = AdaptTo.notNull(adaptable(), RichTextHandler.class);
    RichText richText = richTextHandler.get(RICHTEXT_FRAGMENT_NAMED_ANCHOR_ID).build();
    assertEquals(RICHTEXT_FRAGMENT_NAMED_ANCHOR_ID_REWRITTEN, richText.getMarkup());
  }

  @Test
  void testPlainTextContent() {
    RichTextHandler richTextHandler = AdaptTo.notNull(adaptable(), RichTextHandler.class);
    RichText richText = richTextHandler.get(PLAINTEXT_FRAGMENT).textMode(TextMode.PLAIN).build();
    assertEquals(PLAINTEXT_FRAGMENT_REWRITTEN, richText.getMarkup());
  }

  @Test
  void testContentFromResource() {
    RichTextHandler richTextHandler = AdaptTo.notNull(adaptable(), RichTextHandler.class);

    Resource resource = context.create().resource("/text/resource",
        ImmutableValueMap.of(RichTextNameConstants.PN_TEXT, RICHTEXT_FRAGMENT));
    RichText richText = richTextHandler.get(resource).build();
    assertEquals(RICHTEXT_FRAGMENT_REWRITTEN, richText.getMarkup());
  }

  @Test
  void testPlainTextContentFromResource() {
    RichTextHandler richTextHandler = AdaptTo.notNull(adaptable(), RichTextHandler.class);

    Resource resource = context.create().resource("/text/resource",
        ImmutableValueMap.of(RichTextNameConstants.PN_TEXT, PLAINTEXT_FRAGMENT,
            RichTextNameConstants.PN_TEXT_IS_RICH, false));
    RichText richText = richTextHandler.get(resource).build();
    assertEquals(PLAINTEXT_FRAGMENT_REWRITTEN, richText.getMarkup());
  }

  @Test
  void testContentWithCustomRewriterContentHandler() {
    context.registerService(RichTextHandlerConfig.class, new RichTextHandlerConfig() {
      @Override
      public List<Class<? extends RewriteContentHandler>> getRewriteContentHandlers() {
        return List.of(DefaultRewriteContentHandler.class, CustomRewriteContentHandler.class);
      }
    });

    RichTextHandler richTextHandler = AdaptTo.notNull(adaptable(), RichTextHandler.class);
    RichText richText = richTextHandler.get(RICHTEXT_FRAGMENT).build();
    assertEquals(RICHTEXT_FRAGMENT_REWRITTEN_CUSTOM_REWRITER, richText.getMarkup());
  }


  @Model(adaptables = { SlingHttpServletRequest.class, Resource.class })
  public static class CustomRewriteContentHandler implements RewriteContentHandler {

    @Override
    public List<Content> rewriteElement(Element element) {
      element.setName(element.getName().toUpperCase());
      return null;
    }

    @Override
    public List<Content> rewriteText(Text text) {
      return List.of(new Text(text.getText().toLowerCase()));
    }

  }

}
