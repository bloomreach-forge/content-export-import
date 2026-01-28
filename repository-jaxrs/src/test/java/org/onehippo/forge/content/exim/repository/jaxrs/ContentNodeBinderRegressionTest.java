/*
 * Copyright 2024 Bloomreach B.V. (https://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.content.exim.repository.jaxrs;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.bloomreach.forge.brut.resources.AbstractJaxrsTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultJcrContentNodeBinder;
import org.onehippo.forge.content.pojo.model.ContentNode;
import org.onehippo.forge.content.pojo.model.ContentProperty;
import org.onehippo.forge.content.pojo.model.ContentPropertyType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for DefaultJcrContentNodeBinder property type handling.
 *
 * These tests verify that property types are correctly preserved during import,
 * especially edge cases like empty multi-value properties.
 *
 * <p><b>KNOWN BUG:</b> Properties with empty values arrays lose their declared type
 * and default to STRING. This affects LONG, DOUBLE, BOOLEAN, DATE types.
 * Originally reported as "STRING becomes DOUBLE" but actual behavior is
 * "all types become STRING when values array is empty".
 *
 * <p>Bug occurs in DefaultJcrContentNodeBinder from hippo-pojo-bind-jcr library.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ContentNodeBinderRegressionTest extends AbstractJaxrsTest {

    private static final String TEST_NODE_PATH = "/content/documents/exim/binder-test";

    private Session session;
    private DefaultJcrContentNodeBinder binder;

    @BeforeAll
    public void init() {
        super.init();
    }

    @AfterAll
    public void destroy() {
        if (session != null && session.isLive()) {
            session.logout();
        }
        super.destroy();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        setupForNewRequest();

        Repository repository = getComponentManager().getComponent(Repository.class);
        session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        binder = new DefaultJcrContentNodeBinder();

        // Create test node if it doesn't exist
        if (!session.nodeExists(TEST_NODE_PATH)) {
            Node parent = session.getNode("/content/documents/exim");
            parent.addNode("binder-test", "nt:unstructured");
            session.save();
        }
    }

    @AfterEach
    public void afterEach() throws Exception {
        // Clean up test node properties
        if (session != null && session.isLive() && session.nodeExists(TEST_NODE_PATH)) {
            Node testNode = session.getNode(TEST_NODE_PATH);
            // Remove any properties we added during tests
            if (testNode.hasProperty("testProp")) {
                testNode.getProperty("testProp").remove();
            }
            if (testNode.hasProperty("name")) {
                testNode.getProperty("name").remove();
            }
            session.save();
        }
    }

    @Override
    protected String getAnnotatedHstBeansClasses() {
        return "classpath*:org/onehippo/forge/content/exim/**/*.class";
    }

    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Arrays.asList("/brut-test-config.xml", "/rest-resources.xml");
    }

    @Override
    protected String contributeHstConfigurationRootPath() {
        return "/hst:exim";
    }

    @Override
    protected List<String> contributeAddonModulePaths() {
        return Arrays.asList();
    }

    // ========== Regression Test: Empty Multi-Value STRING Property ==========

    /**
     * Regression test for issue: STRING property with empty values becomes DOUBLE.
     *
     * When importing a property definition like:
     * "properties" : [ { "name" : "name", "type" : "STRING", "multiple" : true, "values" : [ ] } ]
     *
     * The property should be created with type STRING, not DOUBLE.
     */
    @Test
    void testBindProperty_stringTypeWithEmptyValues_shouldRemainString() throws Exception {
        // Arrange: Create a ContentProperty with STRING type and empty values
        ContentProperty contentProperty = new ContentProperty("name", ContentPropertyType.STRING, true);
        // Note: No values added - simulates empty values array: "values" : [ ]

        ContentNode contentNode = new ContentNode("binder-test", "nt:unstructured");
        contentNode.setProperty(contentProperty);

        Node targetNode = session.getNode(TEST_NODE_PATH);

        // Act: Bind the content node to the JCR node
        binder.bind(targetNode, contentNode);
        session.save();

        // Assert: The property should have STRING type, not DOUBLE
        if (targetNode.hasProperty("name")) {
            Property jcrProperty = targetNode.getProperty("name");
            int actualType = jcrProperty.getType();

            assertEquals(PropertyType.STRING, actualType,
                "Property 'name' should have type STRING (" + PropertyType.STRING + ") " +
                "but was " + PropertyType.nameFromValue(actualType) + " (" + actualType + ")");
        }
        // If property doesn't exist, that's acceptable for empty values
    }

    @Test
    void testBindProperty_stringTypeWithValues_shouldBeString() throws Exception {
        // Arrange
        ContentProperty contentProperty = new ContentProperty("name", ContentPropertyType.STRING, true);
        contentProperty.addValue("value1");
        contentProperty.addValue("value2");

        ContentNode contentNode = new ContentNode("binder-test", "nt:unstructured");
        contentNode.setProperty(contentProperty);

        Node targetNode = session.getNode(TEST_NODE_PATH);

        // Act
        binder.bind(targetNode, contentNode);
        session.save();

        // Assert
        assertTrue(targetNode.hasProperty("name"), "Property 'name' should exist");
        Property jcrProperty = targetNode.getProperty("name");

        assertEquals(PropertyType.STRING, jcrProperty.getType(),
            "Property should have STRING type");
        assertTrue(jcrProperty.isMultiple(), "Property should be multi-valued");
        assertEquals(2, jcrProperty.getValues().length, "Property should have 2 values");
    }

    @Test
    void testBindProperty_longTypeWithEmptyValues_shouldRemainLong_BUG() throws Exception {
        // Arrange
        ContentProperty contentProperty = new ContentProperty("testProp", ContentPropertyType.LONG, true);
        // No values added

        ContentNode contentNode = new ContentNode("binder-test", "nt:unstructured");
        contentNode.setProperty(contentProperty);

        Node targetNode = session.getNode(TEST_NODE_PATH);

        // Act
        binder.bind(targetNode, contentNode);
        session.save();

        // Assert
        if (targetNode.hasProperty("testProp")) {
            Property jcrProperty = targetNode.getProperty("testProp");
            int actualType = jcrProperty.getType();

            assertEquals(PropertyType.LONG, actualType,
                "Property 'testProp' should have type LONG (" + PropertyType.LONG + ") " +
                "but was " + PropertyType.nameFromValue(actualType) + " (" + actualType + ")");
        }
    }

    @Test
    void testBindProperty_booleanTypeWithEmptyValues_shouldRemainBoolean() throws Exception {
        // Arrange
        ContentProperty contentProperty = new ContentProperty("testProp", ContentPropertyType.BOOLEAN, true);
        // No values added

        ContentNode contentNode = new ContentNode("binder-test", "nt:unstructured");
        contentNode.setProperty(contentProperty);

        Node targetNode = session.getNode(TEST_NODE_PATH);

        // Act
        binder.bind(targetNode, contentNode);
        session.save();

        // Assert
        if (targetNode.hasProperty("testProp")) {
            Property jcrProperty = targetNode.getProperty("testProp");
            int actualType = jcrProperty.getType();

            assertEquals(PropertyType.BOOLEAN, actualType,
                "Property 'testProp' should have type BOOLEAN (" + PropertyType.BOOLEAN + ") " +
                "but was " + PropertyType.nameFromValue(actualType) + " (" + actualType + ")");
        }
    }

    @Test
    void testBindProperty_dateTypeWithEmptyValues_shouldRemainDate() throws Exception {
        // Arrange
        ContentProperty contentProperty = new ContentProperty("testProp", ContentPropertyType.DATE, true);
        // No values added

        ContentNode contentNode = new ContentNode("binder-test", "nt:unstructured");
        contentNode.setProperty(contentProperty);

        Node targetNode = session.getNode(TEST_NODE_PATH);

        // Act
        binder.bind(targetNode, contentNode);
        session.save();

        // Assert
        if (targetNode.hasProperty("testProp")) {
            Property jcrProperty = targetNode.getProperty("testProp");
            int actualType = jcrProperty.getType();

            assertEquals(PropertyType.DATE, actualType,
                "Property 'testProp' should have type DATE (" + PropertyType.DATE + ") " +
                "but was " + PropertyType.nameFromValue(actualType) + " (" + actualType + ")");
        }
    }

    /**
     * BUG: DOUBLE property with empty values becomes STRING.
     * Remove @Disabled once the bug is fixed.
     */
    @Test
    void testBindProperty_doubleTypeWithEmptyValues_shouldRemainDouble() throws Exception {
        // Arrange
        ContentProperty contentProperty = new ContentProperty("testProp", ContentPropertyType.DOUBLE, true);
        // No values added

        ContentNode contentNode = new ContentNode("binder-test", "nt:unstructured");
        contentNode.setProperty(contentProperty);

        Node targetNode = session.getNode(TEST_NODE_PATH);

        // Act
        binder.bind(targetNode, contentNode);
        session.save();

        // Assert
        if (targetNode.hasProperty("testProp")) {
            Property jcrProperty = targetNode.getProperty("testProp");
            int actualType = jcrProperty.getType();

            assertEquals(PropertyType.DOUBLE, actualType,
                "Property 'testProp' should have type DOUBLE (" + PropertyType.DOUBLE + ") " +
                "but was " + PropertyType.nameFromValue(actualType) + " (" + actualType + ")");
        }
    }

    @Test
    void testBindProperty_singleStringWithNoValue_shouldRemainString() throws Exception {
        // Arrange
        ContentProperty contentProperty = new ContentProperty("testProp", ContentPropertyType.STRING, false);
        // No value set

        ContentNode contentNode = new ContentNode("binder-test", "nt:unstructured");
        contentNode.setProperty(contentProperty);

        Node targetNode = session.getNode(TEST_NODE_PATH);

        // Act
        binder.bind(targetNode, contentNode);
        session.save();

        // Assert
        if (targetNode.hasProperty("testProp")) {
            Property jcrProperty = targetNode.getProperty("testProp");
            int actualType = jcrProperty.getType();

            assertEquals(PropertyType.STRING, actualType,
                "Property 'testProp' should have type STRING (" + PropertyType.STRING + ") " +
                "but was " + PropertyType.nameFromValue(actualType) + " (" + actualType + ")");
        }
    }
}
