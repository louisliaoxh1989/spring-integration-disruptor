package org.springframework.integration.disruptor.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.disruptor.config.workflow.DisruptorWorkflowFactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

public final class DisruptorWorkflowParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(final Element element, final ParserContext parserContext) {
		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DisruptorWorkflowFactoryBean.class);
		this.parsePublisherChannelNames(element, parserContext, builder);
		this.parseHandlerGroups(element, parserContext, builder);
		return builder.getBeanDefinition();
	}

	private void parseHandlerGroups(final Element element, final ParserContext parserContext, final BeanDefinitionBuilder builder) {
		final Element handlerGroupsElement = DomUtils.getChildElementByTagName(element, "handler-groups");
		if (handlerGroupsElement != null) {
			final List<Element> handlerGroupElements = DomUtils.getChildElementsByTagName(handlerGroupsElement, "handler-group");
			if (handlerGroupElements.size() > 0) {
				final Map<String, HandlerGroup> handlerGroups = this.parseHandlerGroups(handlerGroupElements, parserContext, builder);
				builder.addPropertyValue("handlerGroups", handlerGroups);
			} else {
				parserContext.getReaderContext().error("At least 1 'handler-group' is mandatory for 'handler-groups'", handlerGroupElements);
			}
		} else {
			parserContext.getReaderContext().error("'handler-groups' element is mandatory for 'workflow'", element);
		}
	}

	private Map<String, HandlerGroup> parseHandlerGroups(final List<Element> handlerGroupElements, final ParserContext parserContext,
			final BeanDefinitionBuilder builder) {
		final List<HandlerGroup> handlerGroups = new ArrayList<HandlerGroup>();
		for (final Element handlerGroupElement : handlerGroupElements) {
			final HandlerGroup handlerGroup = this.parseHandlerGroupElement(handlerGroupElement, parserContext, builder);
			handlerGroups.add(handlerGroup);
		}
		final Map<String, HandlerGroup> handlerGroupMap = new HashMap<String, HandlerGroup>();
		for (final HandlerGroup handlerGroup : handlerGroups) {
			handlerGroupMap.put(handlerGroup.getName(), handlerGroup);
		}
		return handlerGroupMap;
	}

	private HandlerGroup parseHandlerGroupElement(final Element handlerGroupElement, final ParserContext parserContext, final BeanDefinitionBuilder builder) {

		final String group = handlerGroupElement.getAttribute("group");
		if (!StringUtils.hasText(group)) {
			parserContext.getReaderContext().error("'group' attribute is mandatory for 'handler-group'", handlerGroupElement);
		}

		String waitFor = handlerGroupElement.getAttribute("wait-for");
		if (!StringUtils.hasText(waitFor)) {
			waitFor = "ring-buffer";
		}

		final List<String> dependencies = Arrays.asList(waitFor.split(","));

		final List<String> handlerBeanNames = new ArrayList<String>();
		final List<Element> handlerElements = DomUtils.getChildElementsByTagName(handlerGroupElement, "handler");
		if (handlerElements.size() > 0) {
			for (final Element handlerElement : handlerElements) {
				final String refAttribute = handlerElement.getAttribute("ref");
				if (StringUtils.hasText(refAttribute)) {
					handlerBeanNames.add(refAttribute);
				} else {
					parserContext.getReaderContext().error("'ref' attribute is mandatory for 'handler'", handlerElement);
				}
			}

		} else {
			parserContext.getReaderContext().error("At least 1 'handler' is mandatory for 'handler-group'", handlerGroupElement);
		}

		final HandlerGroup handlerGroup = new HandlerGroup();
		handlerGroup.setName(group);
		handlerGroup.setDependencies(dependencies);
		handlerGroup.setHandlerBeanNames(handlerBeanNames);

		return handlerGroup;

	}

	private void parsePublisherChannelNames(final Element element, final ParserContext parserContext, final BeanDefinitionBuilder builder) {
		final Element parent = DomUtils.getChildElementByTagName(element, "publisher-channels");
		if (parent != null) {
			final Set<String> publisherChannelNames = this.parsePublisherChannelNames(parent, parserContext);
			builder.addPropertyValue("publisherChannelNames", publisherChannelNames);
		}
	}

	private Set<String> parsePublisherChannelNames(final Element parent, final ParserContext parserContext) {
		final Set<String> publisherChannelNames = new HashSet<String>();
		final List<Element> children = DomUtils.getChildElementsByTagName(parent, "publisher-channel");
		for (final Element child : children) {
			final String publisherChannelRef = child.getAttribute("ref");
			if (StringUtils.hasText(publisherChannelRef)) {
				publisherChannelNames.add(publisherChannelRef);
			} else {
				parserContext.getReaderContext().error("'ref' attribute is mandatory for 'publisher-channel'", child);
			}
		}
		return publisherChannelNames;
	}

}
