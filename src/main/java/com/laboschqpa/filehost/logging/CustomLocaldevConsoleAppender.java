package com.laboschqpa.filehost.logging;

import com.janosgats.logging.flexibleappender.FlexibleAppender;
import com.janosgats.logging.flexibleappender.enableable.*;
import com.janosgats.logging.flexibleappender.loglinebuilder.AbstractLogLineBuilder;
import com.janosgats.logging.flexibleappender.loglinebuilder.specific.LocaldevConsoleLogLineBuilder;
import com.janosgats.logging.flexibleappender.loglineoutput.AbstractLogLineOutput;
import com.janosgats.logging.flexibleappender.loglineoutput.specific.StdOutLogLineOutput;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Plugin(name = "CustomLocaldevConsoleAppender", category = "Core", elementType = "appender", printObject = true)
public class CustomLocaldevConsoleAppender extends FlexibleAppender {

    private CustomLocaldevConsoleAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

        AbstractLogLineBuilder logLineBuilder = new LocaldevConsoleLogLineBuilder(dateTimeFormatter, 2);

        AbstractLogLineOutput logLineOutput = new StdOutLogLineOutput();

        super.setUpAppender(new AlwaysOnEnableable(), logLineBuilder, logLineOutput);
    }

    @PluginFactory
    public static CustomLocaldevConsoleAppender createAppender(
        @PluginAttribute("name") String name,
        @PluginElement("Layout") Layout<? extends Serializable> layout,
        @PluginElement("Filter") final Filter filter,
        @PluginAttribute("otherAttribute") String otherAttribute) {
        if (name == null) {
            System.out.println("No name provided for LocaldevConsoleAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();//A layout has to be provided to instantiate the appender
        }
        return new CustomLocaldevConsoleAppender(name, filter, layout, false, new Property[0]);
    }
}