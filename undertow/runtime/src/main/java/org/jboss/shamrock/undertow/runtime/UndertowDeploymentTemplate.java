package org.jboss.shamrock.undertow.runtime;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.jboss.shamrock.runtime.ContextObject;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.runtime.StartupContext;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;

/**
 * Provides the runtime methods to bootstrap Undertow. This class is present in the final uber-jar,
 * and is invoked from generated bytecode
 */
public class UndertowDeploymentTemplate {

    @ContextObject("deploymentInfo")
    public DeploymentInfo createDeployment(String name) {
        DeploymentInfo d = new DeploymentInfo();
        d.setClassLoader(getClass().getClassLoader());
        d.setDeploymentName(name);
        d.setContextPath("/");
        ClassLoader cl = UndertowDeploymentTemplate.class.getClassLoader();
        if(cl != null) {
            d.setClassLoader(cl);
        } else {
            //remove once graal release with CL support is availible
            d.setClassLoader(new ClassLoader() {
            });
        }
        return d;
    }

    public <T> InstanceFactory<T> createInstanceFactory(InjectionInstance<T> injectionInstance) {
        return new ShamrockInstanceFactory<T>(injectionInstance);
    }

    public void registerServlet(@ContextObject("deploymentInfo") DeploymentInfo info, String name, String servletClass, boolean asyncSupported, InstanceFactory<? extends Servlet> instanceFactory) throws Exception {
        ServletInfo servletInfo = new ServletInfo(name, (Class<? extends Servlet>) Class.forName(servletClass), instanceFactory);
        info.addServlet(servletInfo);
        servletInfo.setAsyncSupported(asyncSupported);
    }

    public void addServletMapping(@ContextObject("deploymentInfo") DeploymentInfo info, String name, String mapping) throws Exception {
        ServletInfo sv = info.getServlets().get(name);
        sv.addMapping(mapping);
    }

    public void addServletContextParameter(@ContextObject("deploymentInfo") DeploymentInfo info, String name, String value) {
        info.addInitParameter(name, value);
    }

    public void deploy(StartupContext startupContext, @ContextObject("deploymentInfo") DeploymentInfo info) throws ServletException {
        ServletContainer servletContainer = Servlets.defaultContainer();
        DeploymentManager manager = servletContainer.addDeployment(info);
        manager.deploy();
        Undertow val = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(manager.start())
                .build();
        val.start();
        startupContext.putValue("undertow", val);
    }

}
