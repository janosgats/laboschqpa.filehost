package com.laboschqpa.filehost.logging;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.janosgats.logging.flexibleappender.FlexibleAppender;
import com.janosgats.logging.flexibleappender.enableable.AlwaysOnEnableable;
import com.janosgats.logging.flexibleappender.helper.LoggingHelper;
import com.janosgats.logging.flexibleappender.loglinebuilder.AbstractLogLineBuilder;
import com.janosgats.logging.flexibleappender.loglinebuilder.DateTimeFormatterLogLineBuilder;
import com.janosgats.logging.flexibleappender.loglineoutput.AbstractLogLineOutput;
import com.janosgats.logging.flexibleappender.loglineoutput.specific.StdOutLogLineOutput;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Plugin(name = "CustomGoogleCloudConsoleAppender", category = "Core", elementType = "appender", printObject = true)
public class CustomGoogleCloudConsoleAppender extends FlexibleAppender {

    private CustomGoogleCloudConsoleAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC);

        AbstractLogLineBuilder logLineBuilder = new LogLineBuilder(dateTimeFormatter);

        AbstractLogLineOutput logLineOutput = new StdOutLogLineOutput();

        super.setUpAppender(new AlwaysOnEnableable(), logLineBuilder, logLineOutput);
    }

    @PluginFactory
    public static CustomGoogleCloudConsoleAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("otherAttribute") String otherAttribute) {
        if (name == null) {
            System.out.println("No name provided for GoogleCloudConsoleAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();//A layout has to be provided to instantiate the appender
        }
        return new CustomGoogleCloudConsoleAppender(name, filter, layout, false, new Property[0]);
    }

    private static class LogLineBuilder extends DateTimeFormatterLogLineBuilder {
        public LogLineBuilder(DateTimeFormatter dateTimeFormatter) {
            super(dateTimeFormatter);
        }

        @Override
        public String buildLogLine(LogEvent logEvent) {
            final ObjectNode logNode = new ObjectNode(JsonNodeFactory.instance);

            logNode.put("timestamp", getFormattedDateTimeStringFromLogEvent(logEvent));

            logNode.put("level", logEvent.getLevel().toString());
            logNode.put("severity", logEvent.getLevel().toString());
            logNode.put("message", logEvent.getMessage().getFormattedMessage());
            logNode.put("loggerName", logEvent.getLoggerName());


            logNode.put("threadId", String.valueOf(logEvent.getThreadId()));
            logNode.put("threadName", logEvent.getThreadName());
            logNode.put("threadPriority", String.valueOf(logEvent.getThreadPriority()));
            if (logEvent.getMarker() != null)
                logNode.put("marker", logEvent.getMarker().getName());

            if (logEvent.getThrown() != null)
                logNode.put("thrown", LoggingHelper.getStackTraceAsString(logEvent.getThrown()));

            final ObjectNode contextNode = new ObjectNode(JsonNodeFactory.instance);
            logEvent.getContextData().forEach((key, val) -> {
                try {
                    contextNode.put(key, val.toString());
                } catch (Exception e) {
                    contextNode.put(key, "Exception while serializing this field: " + e.getClass().getName() + " - " + e.getMessage());
                }
            });
            logNode.set("context", contextNode);

            return logNode.toString();
        }
    }
}