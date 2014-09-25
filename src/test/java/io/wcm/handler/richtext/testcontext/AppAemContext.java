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

import io.wcm.config.spi.ApplicationProvider;
import io.wcm.config.spi.ConfigurationFinderStrategy;
import io.wcm.config.spi.ParameterProvider;
import io.wcm.handler.url.UrlParams;
import io.wcm.handler.url.impl.UrlHandlerParameterProviderImpl;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;
import io.wcm.testing.mock.sling.ResourceResolverType;
import io.wcm.testing.mock.wcmio.config.MockConfig;

import java.io.IOException;

import org.apache.sling.api.resource.PersistenceException;

import com.google.common.collect.ImmutableMap;

/**
 * Sets up {@link AemContext} for unit tests in this application.
 */
public final class AppAemContext {

  /**
   * Media formats path
   */
  public static final String MEDIAFORMATS_PATH = "/apps/test/mediaformat";

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
    return new AemContext(new SetUpCallback(null), ResourceResolverType.RESOURCERESOLVER_MOCK);
  }

  public static AemContext newAemContext(AemContextCallback callback) {
    return new AemContext(new SetUpCallback(callback), ResourceResolverType.RESOURCERESOLVER_MOCK);
  }

  /**
   * Custom set up rules required in all unit tests.
   */
  private static final class SetUpCallback implements AemContextCallback {

    private final AemContextCallback testCallback;

    public SetUpCallback(AemContextCallback testCallback) {
      this.testCallback = testCallback;
    }

    @Override
    public void execute(AemContext context) throws PersistenceException, IOException {

      // call test-specific callback first
      if (testCallback != null) {
        testCallback.execute(context);
      }

      // URL handler-specific parameter definitions
      context.registerService(ParameterProvider.class, new UrlHandlerParameterProviderImpl());

      // application provider
      context.registerService(ApplicationProvider.class,
          MockConfig.applicationProvider(UrlParams.APPLICATION_ID, "/content"),
          ImmutableMap.<String, Object>builder()
          .build());

      // configuration finder strategy
      context.registerService(ConfigurationFinderStrategy.class,
          MockConfig.configurationFinderStrategyAbsoluteParent(UrlParams.APPLICATION_ID,
              DummyUrlHandlerConfig.SITE_ROOT_LEVEL));

      // wcm.io configuration
      MockConfig.setUp(context);

      // sling models registration
      context.addModelsForPackage("io.wcm.handler.url");
      context.addModelsForPackage("io.wcm.handler.media");
      context.addModelsForPackage("io.wcm.handler.link");
      context.addModelsForPackage("io.wcm.handler.richtext");

      // create current page in site context
      context.currentPage(context.create().page(ROOTPATH_CONTENT,
          DummyAppTemplate.CONTENT.getTemplatePath()));

      // default site config
      MockConfig.writeConfiguration(context, ROOTPATH_CONTENT,
          ImmutableMap.<String, Object>builder()
          .put(UrlParams.SITE_URL.getName(), "http://www.dummysite.org")
          .put(UrlParams.SITE_URL_SECURE.getName(), "https://www.dummysite.org")
          .put(UrlParams.SITE_URL_AUTHOR.getName(), "https://author.dummysite.org")
          .build());
    }

  }

}
