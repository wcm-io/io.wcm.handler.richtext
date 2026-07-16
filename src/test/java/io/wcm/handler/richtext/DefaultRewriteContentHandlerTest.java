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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.output.Format;
import org.jdom2.output.Format.TextMode;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.handler.richtext.testcontext.AppAemContext;
import io.wcm.sling.commons.adapter.AdaptTo;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

/**
 * Test {@link DefaultRewriteContentHandler}.
 */
@ExtendWith(AemContextExtension.class)
class DefaultRewriteContentHandlerTest {

  // same output format as used by RichText#getMarkup()
  private static final XMLOutputter XML_OUTPUTTER = new XMLOutputter(Format.getCompactFormat().setTextMode(TextMode.PRESERVE));

  final AemContext context = AppAemContext.newAemContext();

  private DefaultRewriteContentHandler underTest;

  @BeforeEach
  void setUp() {
    underTest = AdaptTo.notNull(context.request(), DefaultRewriteContentHandler.class);
  }

  @Test
  void testSelfClosingTagWithContent() {
    // void elements (e.g. br) must have any content removed to stay self-closing
    Element element = new Element("br");
    element.addContent(new Text("some text"));

    List<Content> result = underTest.rewriteElement(element);

    assertNull(result);
    assertTrue(element.getContent().isEmpty());
    assertEquals("<br />", XML_OUTPUTTER.outputString(element));
  }

  @Test
  void testSelfClosingTagWithoutContent() {
    // void elements without content stay untouched (remain self-closing)
    Element element = new Element("br");

    List<Content> result = underTest.rewriteElement(element);

    assertNull(result);
    assertTrue(element.getContent().isEmpty());
    assertEquals("<br />", XML_OUTPUTTER.outputString(element));
  }

  @Test
  void testNonSelfClosingTagWithContent() {
    // non-void elements with content are left untouched
    Element element = new Element("div");
    element.addContent(new Text("some text"));

    List<Content> result = underTest.rewriteElement(element);

    assertNull(result);
    assertEquals(1, element.getContentSize());
    assertEquals("some text", element.getText());
    assertEquals("<div>some text</div>", XML_OUTPUTTER.outputString(element));
  }

  @Test
  void testNonSelfClosingTagWithoutContent() {
    // non-void elements without content get an empty text node to avoid being rendered self-closing
    Element element = new Element("div");

    List<Content> result = underTest.rewriteElement(element);

    assertNull(result);
    assertEquals(1, element.getContentSize());
    assertEquals("", element.getText());
    assertEquals("<div></div>", XML_OUTPUTTER.outputString(element));
  }

}