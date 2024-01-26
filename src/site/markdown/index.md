## About RichText Handler

Rich text processing and markup generation.

[![Maven Central](https://img.shields.io/maven-central/v/io.wcm/io.wcm.handler.richtext)](https://repo1.maven.org/maven2/io/wcm/io.wcm.handler.richtext/)


### Documentation

* [Usage][usage]
* [General concepts][general-concepts]
* [RTE link plugin][rte-link-plugin]
* [API documentation][apidocs]
* [Changelog][changelog]


### Overview

The RichText Handler provides:

* Parse and format rich text produced by the AEM RichText Editor
* Detect inline links and images and format them using [Link Handler][link-handler] and [Media Handler][media-handler]
* Support XHTML and Plain Text
* Generic Sling Models for usage in views: [Sling Models][ui-package]
* [RTE link plugin][rte-link-plugin] that provides all features of the default link handler link dialog.

Read the [general concepts][general-concepts] to get an overview of the functionality.


### AEM Version Support Matrix

|RichText Handler version |AEM version supported
|-------------------------|----------------------
|2.0.x or higher          |AEM 6.5.17+, AEMaaCS
|1.6.x or higher          |AEM 6.5.7+, AEMaaCS
|1.5.x                    |AEM 6.4.5+, AEMaaCS
|1.4.x                    |AEM 6.3.3+, AEM 6.4.5+
|1.2.x - 1.3.x            |AEM 6.2+
|1.0.x - 1.1.x            |AEM 6.1+
|0.x                      |AEM 6.0+


### Dependencies

To use this module you have to deploy also:

|---|---|---|
| [wcm.io Sling Commons](https://repo1.maven.org/maven2/io/wcm/io.wcm.sling.commons/) | [![Maven Central](https://img.shields.io/maven-central/v/io.wcm/io.wcm.sling.commons)](https://repo1.maven.org/maven2/io/wcm/io.wcm.sling.commons/) |
| [wcm.io AEM Sling Models Extensions](https://repo1.maven.org/maven2/io/wcm/io.wcm.sling.models/) | [![Maven Central](https://img.shields.io/maven-central/v/io.wcm/io.wcm.sling.models)](https://repo1.maven.org/maven2/io/wcm/io.wcm.sling.models/) |
| [wcm.io WCM Commons](https://repo1.maven.org/maven2/io/wcm/io.wcm.wcm.commons/) | [![Maven Central](https://img.shields.io/maven-central/v/io.wcm/io.wcm.wcm.commons)](https://repo1.maven.org/maven2/io/wcm/io.wcm.wcm.commons/) |
| [wcm.io WCM Granite UI Extensions](https://repo1.maven.org/maven2/io/wcm/io.wcm.wcm.ui.granite/) | [![Maven Central](https://img.shields.io/maven-central/v/io.wcm/io.wcm.wcm.ui.granite)](https://repo1.maven.org/maven2/io/wcm/io.wcm.wcm.ui.granite/) |
| [wcm.io Handler Commons](https://repo1.maven.org/maven2/io/wcm/io.wcm.handler.commons/) | [![Maven Central](https://img.shields.io/maven-central/v/io.wcm/io.wcm.handler.commons)](https://repo1.maven.org/maven2/io/wcm/io.wcm.handler.commons/) |
| [wcm.io URL Handler](https://repo1.maven.org/maven2/io/wcm/io.wcm.handler.url/) | [![Maven Central](https://img.shields.io/maven-central/v/io.wcm/io.wcm.handler.url)](https://repo1.maven.org/maven2/io/wcm/io.wcm.handler.url/) |
| [wcm.io Media Handler](https://repo1.maven.org/maven2/io/wcm/io.wcm.handler.media/) | [![Maven Central](https://img.shields.io/maven-central/v/io.wcm/io.wcm.handler.media)](https://repo1.maven.org/maven2/io/wcm/io.wcm.handler.media/) |
| [wcm.io Link Handler](https://repo1.maven.org/maven2/io/wcm/io.wcm.handler.link/) | [![Maven Central](https://img.shields.io/maven-central/v/io.wcm/io.wcm.handler.link)](https://repo1.maven.org/maven2/io/wcm/io.wcm.handler.link/) |


### GitHub Repository

Sources: https://github.com/wcm-io/io.wcm.handler.richtext


[usage]: usage.html
[general-concepts]: general-concepts.html
[rte-link-plugin]: rte-link-plugin.html
[apidocs]: apidocs/
[changelog]: changes-report.html
[link-handler]: ../link/
[media-handler]: ../media/
[ui-package]: apidocs/io/wcm/handler/richtext/ui/package-summary.html
