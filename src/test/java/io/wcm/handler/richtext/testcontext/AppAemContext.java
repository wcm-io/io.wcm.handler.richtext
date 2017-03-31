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
package io.wcm.handler.richtext.testcontext;

import static io.wcm.testing.mock.wcmio.caconfig.ContextPlugins.WCMIO_CACONFIG;
import static io.wcm.testing.mock.wcmio.sling.ContextPlugins.WCMIO_SLING;
import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;

import io.wcm.handler.link.impl.DefaultLinkHandlerConfig;
import io.wcm.handler.link.impl.LinkHandlerConfigAdapterFactory;
import io.wcm.handler.media.format.impl.MediaFormatProviderManagerImpl;
import io.wcm.handler.media.impl.DefaultMediaHandlerConfig;
import io.wcm.handler.media.impl.MediaHandlerConfigAdapterFactory;
import io.wcm.handler.media.spi.MediaHandlerConfig;
import io.wcm.handler.url.SiteConfig;
import io.wcm.handler.url.impl.DefaultUrlHandlerConfig;
import io.wcm.handler.url.impl.UrlHandlerConfigAdapterFactory;
import io.wcm.handler.url.spi.UrlHandlerConfig;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextBuilder;
import io.wcm.testing.mock.aem.junit.AemContextCallback;
import io.wcm.testing.mock.wcmio.caconfig.MockCAConfig;

/**
 * Sets up {@link AemContext} for unit tests in this application.
 */
public final class AppAemContext {

  /**
   * DAM root path
   */
  public static final String DAM_PATH = "/content/dam/test";

  /**
   * Content root path
   */
  public static final String ROOTPATH_CONTENT = "/content/unittest/de_test/brand/de";

  private AppAemContext() {
    // static methods only
  }

  public static AemContext newAemContext() {
    return newAemContext(null);
  }

  public static AemContext newAemContext(AemContextCallback callback) {
    return new AemContextBuilder()
        .plugin(CACONFIG)
        .plugin(WCMIO_SLING, WCMIO_CACONFIG)
        .afterSetUp(callback)
        .afterSetUp(SETUP_CALLBACK)
        .build();
  }

  /**
   * Custom set up rules required in all unit tests.
   */
  private static final AemContextCallback SETUP_CALLBACK = new AemContextCallback() {
    @Override
    public void execute(AemContext context) throws Exception {

      // handler SPI
      context.registerInjectActivateService(new UrlHandlerConfigAdapterFactory());
      context.registerInjectActivateService(new DefaultUrlHandlerConfig());
      context.registerService(UrlHandlerConfig.class, new DummyUrlHandlerConfig());
      context.registerInjectActivateService(new MediaHandlerConfigAdapterFactory());
      context.registerInjectActivateService(new DefaultMediaHandlerConfig());
      context.registerService(MediaHandlerConfig.class, new DummyMediaHandlerConfig());
      context.registerInjectActivateService(new LinkHandlerConfigAdapterFactory());
      context.registerInjectActivateService(new DefaultLinkHandlerConfig());

      // context path strategy
      MockCAConfig.contextPathStrategyAbsoluteParent(context, DummyUrlHandlerConfig.SITE_ROOT_LEVEL);

      // media formats
      context.registerInjectActivateService(new MediaFormatProviderManagerImpl());

      // sling models registration
      context.addModelsForPackage("io.wcm.handler.richtext");

      // create current page in site context
      context.currentPage(context.create().page(ROOTPATH_CONTENT,
          DummyAppTemplate.CONTENT.getTemplatePath()));

      // default site config
      MockCAConfig.writeConfiguration(context, ROOTPATH_CONTENT, SiteConfig.class.getName(),
          "siteUrl", "http://www.dummysite.org",
          "siteUrlSecure", "https://www.dummysite.org",
          "siteUrlAuthor", "https://author.dummysite.org");
    }
  };

}
