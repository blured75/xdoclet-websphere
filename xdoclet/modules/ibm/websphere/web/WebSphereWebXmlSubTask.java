/*
 * Copyright (c) 2001, 2002 The XDoclet team
 * All rights reserved.
 */
package xdoclet.modules.ibm.websphere.web;

import xdoclet.XDocletException;

import xdoclet.XmlSubTask;
import xdoclet.util.Translator;

/**
 * This class is responsible for generating the WebSphere specific deployment descriptors. It generates <i>
 * ibm-web-bnd.xmi</i> and <i>ibm-web-ext.xmi</i> . The id attributes of various elements in the deployment descriptors
 * and the <i>web.xml</i> file must be applied as a seperate step with the task &lt;WebSphereWebXmlIds/&gt; after the
 * deployement descriptors have been generated. <p>
 *
 * <i>ibm-web-bnd.xmi</i> is responsible for:</p>
 * <ul>
 *   <li> Binding EJB references in <code>web.xml</code> to a JNDI name in the local namespace</li>
 *   <li> Binding resource references in <code>web.xml</code> to a JNDI name in the local namespace</li>
 * </ul>
 * <p>
 *
 * <i>ibm-web-ext.xmi</i> contains IBM specific extensions to the web.xml file and is responsible for specifying the
 * following:</p>
 * <ul>
 *   <li> A <i>Reload Interval</i> . Every 'reload interval' seconds, the web application's files are checked and
 *   reloaded if they have been modified</li>
 *   <li> A flag specifying whether <i>Reloading</i> is enabled</li>
 *   <li> The URI of an error page</li>
 *   <li> Enabling or disabling <i>File Serving</i> . If enabled, the application is allowed to serve static file types
 *   such as HTML and GIF. File serving can be disabled if, for example, the application contains only dynamic
 *   components. The default value is true.</li>
 *   <li> Enabling or disabling <i>Directory Browsing</i> . If enabled, the application may browse disk directories.
 *   Directory browsing can be disabled if, for example, you want to protect data. The default value is true.</li>
 *   <li> Enabling or disabling the serving of servlets by their classname. The default value is true</li>
 *   <li> Association of responses with a given MIME type to a given target (servlet?); the idea being to either
 *   transform or filter a response.</li>
 *   <li> <i>Page List</i> configuration. Page lists allow servlets, which have been configured to utilise page list
 *   support, to refer to resources by names which map onto URIs.</li>
 *   <li> <i>JSP Attribute</i> configuration. To quote the IBM documentation "<i>JSP attributes are used by the servlet
 *   that implements JSP processing behavior.</i> ". No doubt IBM have lots of undocumented parameters to the JSP
 *   processing engine that can passed using this feature.</li>
 *   <li> <i>File Serving Attribute</i> configuration. To quote the IBM documentation "<i>File-serving attributes are
 *   used by the servlet that implements file-serving behavior.</i> ". Another means to pass undocumented parameters,
 *   this time to the file serving servlet.</li>
 *   <li> <i>Invoker Attribute</i> configuration. To quote the IBM documentation "<i>Invoker attributes are used by the
 *   servlet that implements the invocation behavior.</i> ". <span style= "font-size: 70%">(I wonder if anyone at IBM
 *   knows what can be configured here)</span> </li>
 *   <li> <i>Servlet Cache</i> configuration.</li>
 *   <li> An <i>Additional ClassPath</i> that will be used to reference resources outside of those specified in the
 *   archive. <p>
 *
 *   The following help is taken from the IBM documentation:</p> <p>
 *
 *   <i>Specify the values relative to the root of the EAR file and separate the values with spaces. Absolute values
 *   that reference files or directories on the hard drive are ignored. To specify classes that are not in JAR files but
 *   are in the root of the EAR file, use a period and forward slash (./). Consider the following example directory
 *   structure in which the file myapp.ear contains a Web module named mywebapp.war. Additional classes reside in
 *   class1.jar and class2.zip. A class named xyz.class is not packaged in a JAR file but is in the root of the EAR
 *   file.</i> </p> <p>
 *
 *   <i>myapp.ear/mywebapp.war<br/>
 *   myapp.ear/class1.jar<br/>
 *   myapp.ear/class2.zip<br/>
 *   myapp.ear/xyz.class<br/>
 *   </i> </p> <p>
 *
 *   <i>Specify <tt>class1.jar class2.zip ./</tt> as the value of the Additional classpath property. (Name only the
 *   directory for .class files.)</i> </p> </li>
 * </ul>
 *
 *
 * @author        <a href="mailto:ed@whatsa.co.uk">Ed Ward</a>
 * @created       22 August 2002
 * @version       $Revision: 1.1 $
 * @ant.element   display-name="WebSphere" name="webspherewebxml" parent="xdoclet.modules.web.WebDocletTask"
 */
public class WebSphereWebXmlSubTask extends XmlSubTask
{
    /**
     * A config paramater value: true
     */
    private final static String TRUE = "true";

    /**
     * A config parameter value: false
     */
    private final static String FALSE = "false";

    /**
     * The template file for the bindings. The template generates a stylesheet, which when styled with a web.xml file
     * (modified with id attributes) will produce the ibm-web-bnd.xmi bindings deployment descriptor
     */
    private final static String
        BINDINGS_TEMPLATE_FILE = "resources/ibm-web-bnd_xmi.xdt";

    /**
     * The name of the IBM bindings deployment descriptor - ibm-web-bnd.xmi
     */
    private final static String
        GENERATED_BINDINGS_FILE_NAME = "ibm-web-bnd.xmi";

    /**
     * The template file for the extensions. The template generates a stylesheet, which when styled with a web.xml file
     * (modified with id attributes) will produce the ibm-web-ext.xmi extensions deployment descriptor
     */
    private final static String
        EXTENSIONS_TEMPLATE_FILE = "resources/ibm-web-ext_xmi.xdt";

    /**
     * The name of the IBM bindings deployment descriptor - ibm-web-bnd.xmi
     */
    private final static String
        GENERATED_EXTENSIONS_FILE_NAME = "ibm-web-ext.xmi";

    /**
     * Flag indicating whether we've styled the web.xml file
     */
    private boolean hasTransformedWebXml = false;

    /**
     * The virtual host name. Used by the container to locate external resources and EJBs required by a module or
     * application. The value is not mandatory. "A virtual host is a configuration enabling a single host machine to
     * resemble multiple host machines. This property allows you to bind the application to a virtual host in order to
     * enable execution on that virtual host."
     */
    private String  virtualHostName = "";

    /**
     * A Reload Interval . Every 'reload interval' seconds, the web application's files are checked and reloaded if they
     * have been modified. Specifying a value indicates that reloading is enabled and the reloadingEnabled flag in the
     * deployment descriptor will be set 'true' accordingly.
     */
    private String  reloadInterval = "";

    /**
     * Specifies a file name for the default error page. If no other error page is specified in the application, this
     * error page is used.
     */
    private String  defaultErrorPage = "";

    /**
     * Specifies whether file serving is enabled. File serving allows the application to serve static file types, such
     * as HTML and GIF. File serving can be disabled if, for example, the application contains only dynamic components.
     * The default value is true.
     */
    private String  fileServingEnabled = "true";

    /**
     * Specifies whether directory browsing is enabled. Directory browsing allows the application to browse disk
     * directories. Directory browsing can be disabled if, for example, you want to protect data. The default value is
     * true.
     */
    private String  directoryBrowsingEnabled = "true";

    /**
     * Specifies whether a servlet can be served by requesting its class name. Usually, servlets are served only through
     * a URI reference. The class name is the actual name of the servlet on disk. For example, a file named
     * SnoopServlet.java compiles into SnoopServlet.class. (This is the class name.) SnoopServlet.class is normally
     * invoked by specifying snoop in the URI. However, if Serve Servlets by Classname is enabled, the servlet is
     * invoked by specifying SnoopServlet. The default value is true.
     */
    private String  serveServletsByClassnameEnabled = "true";

    /**
     * Specifies an additional class path that will be used to reference resources outside of those specified in the
     * archive.
     */
    private String  additionalClassPath = "";

    public WebSphereWebXmlSubTask()
    {
    }

    /**
     * Returns the virtual host name configuration paramater.
     *
     * @return
     * @see      #virtualHostName
     */
    public String getVirtualHostName()
    {
        return virtualHostName;
    }

    /**
     * @return   the reload interval
     * @see      #reloadInterval
     */
    public String getReloadInterval()
    {
        return reloadInterval;
    }

    /**
     * Returns 'true' if we have a reload interval specified, else 'false' is returned.
     *
     * @return
     * @see      #reloadInterval
     */
    public String getReloadingEnabled()
    {
        if ("".equals(reloadInterval)) {
            return FALSE;
        }

        return TRUE;
    }

    /**
     * @return
     * @see      #defaultErrorPage
     */
    public String getDefaultErrorPage()
    {
        return defaultErrorPage;
    }

    /**
     * @return
     * @see      #fileServingEnabled
     */
    public String getFileServingEnabled()
    {
        return fileServingEnabled;
    }

    /**
     * @return
     * @see      #directoryBrowsingEnabled
     */
    public String getDirectoryBrowsingEnabled()
    {
        return directoryBrowsingEnabled;
    }

    /**
     * @return
     * @see      #serveServletsByClassnameEnabled
     */
    public String getServeServletsByClassnameEnabled()
    {
        return serveServletsByClassnameEnabled;
    }

    /**
     * @return
     * @see      #additionalClassPath
     */
    public String getAdditionalClassPath()
    {
        return additionalClassPath;
    }

    /**
     * Sets the virtual host name configuration parameter.
     *
     * @param name  the virtual host name
     * @see         #virtualHostName
     */
    public void setVirtualHostName(String name)
    {
        virtualHostName = name;
    }

    /**
     * @param reloadInterval
     * @see                   #reloadInterval
     */
    public void setReloadInterval(String reloadInterval)
    {
        try {
            Integer.parseInt(reloadInterval);
            this.reloadInterval = reloadInterval;
        }
        catch (NumberFormatException e) {
            throw new NumberFormatException(
                Translator.getString(XDocletModulesIbmWebsphereWebMessages.class,
                XDocletModulesIbmWebsphereWebMessages.INVALID_CONFIG_VALUE,
                new String[]{reloadInterval, "reloadInterval"}));
        }
    }

    /**
     * We need an implementation of this method else the framework does not see 'reloadingEnabled' as a java bean
     * property (ie. read-only properties don't seem to work).
     *
     * @param reloadingEnabled
     * @see                     #reloadInterval
     */
    public void setReloadingEnabled(String reloadingEnabled)
    {
    }

    /**
     * @param defaultErrorPage
     * @see                     #defaultErrorPage
     */
    public void setDefaultErrorPage(String defaultErrorPage)
    {
        this.defaultErrorPage = defaultErrorPage;
    }

    /**
     * @param fileServingEnabled
     * @see                       #fileServingEnabled
     */
    public void setFileServingEnabled(String fileServingEnabled)
    {
        if (fileServingEnabled == null) {
            fileServingEnabled = "";
        }

        if (TRUE.equals(fileServingEnabled) ||
            FALSE.equals(fileServingEnabled) ||
            "".equals(fileServingEnabled)) {
            this.fileServingEnabled = fileServingEnabled;
        }
        else {
            throw new RuntimeException(
                Translator.getString(
                XDocletModulesIbmWebsphereWebMessages.class,
                XDocletModulesIbmWebsphereWebMessages.INVALID_CONFIG_VALUE,
                new String[]{fileServingEnabled, "fileServingEnabled"}));
        }
    }

    /**
     * @param directoryBrowsingEnabled
     * @see                             #directoryBrowsingEnabled
     */
    public void setDirectoryBrowsingEnabled(String directoryBrowsingEnabled)
    {
        if (directoryBrowsingEnabled == null) {
            directoryBrowsingEnabled = "";
        }

        if (TRUE.equals(directoryBrowsingEnabled) ||
            FALSE.equals(directoryBrowsingEnabled) ||
            "".equals(directoryBrowsingEnabled)) {
            this.directoryBrowsingEnabled = directoryBrowsingEnabled;
        }
        else {
            throw new RuntimeException(
                Translator.getString(
                XDocletModulesIbmWebsphereWebMessages.class,
                XDocletModulesIbmWebsphereWebMessages.INVALID_CONFIG_VALUE,
                new String[]{directoryBrowsingEnabled, "directoryBrowsingEnabled"}));
        }
    }

    /**
     * @param serveServletsByClassnameEnabled
     * @see                                    #serveServletsByClassnameEnabled
     */
    public void setServeServletsByClassnameEnabled(String serveServletsByClassnameEnabled)
    {
        if (serveServletsByClassnameEnabled == null) {
            serveServletsByClassnameEnabled = "";
        }

        if (TRUE.equals(serveServletsByClassnameEnabled) ||
            FALSE.equals(serveServletsByClassnameEnabled) ||
            "".equals(serveServletsByClassnameEnabled)) {
            this.serveServletsByClassnameEnabled = serveServletsByClassnameEnabled;
        }
        else {
            throw new RuntimeException(
                Translator.getString(
                XDocletModulesIbmWebsphereWebMessages.class,
                XDocletModulesIbmWebsphereWebMessages.INVALID_CONFIG_VALUE,
                new String[]{serveServletsByClassnameEnabled, "serveServletsByClassnameEnabled"}));
        }
    }

    /**
     * @param additionalClassPath
     * @see                        #additionalClassPath
     */
    public void setAdditionalClassPath(String additionalClassPath)
    {
        this.additionalClassPath = additionalClassPath;
    }

    /**
     * Called by xdoclet to execute the subtask.
     *
     * @exception XDocletException
     */
    public void execute() throws XDocletException
    {
        setTemplateURL(getClass().getResource(BINDINGS_TEMPLATE_FILE));
        setDestinationFile(GENERATED_BINDINGS_FILE_NAME);
        startProcess();

        setTemplateURL(getClass().getResource(EXTENSIONS_TEMPLATE_FILE));
        setDestinationFile(GENERATED_EXTENSIONS_FILE_NAME);
        startProcess();
    }

    /**
     * Describe what the method does
     *
     * @exception XDocletException  Describe the exception
     */
    protected void engineStarted() throws XDocletException
    {
    }
}
