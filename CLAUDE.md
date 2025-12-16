# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **Bloomreach Content Export/Import (Content-ExIm)** Forge library, a comprehensive toolkit for importing and exporting content within Bloomreach Experience Manager (brXM).

**Project Identity:**
- Group ID: `org.onehippo.forge.content-exim`
- Version: 6.1.1-SNAPSHOT
- Parent: `hippo-cms7-release` (version 16.3.0)
- License: Apache 2.0
- Repository: https://github.com/bloomreach-forge/content-export-import
- Documentation: https://bloomreach-forge.github.io/content-export-import/
- Java Version: Java 17

## Architecture Overview

The Content-ExIm project provides higher-level features for content import and export using:

- **Hippo Workflow API**: For workflow-level document management
- **Hippo JCR POJO Binding Library**: For lower-level JCR mappings and bindings
- **Gallery Magick Image Processing**: For thumbnail image generation

### Project Modules

```
content-export-import/
├── core/                    # Core import/export functionality
│                           # JCR content access and processing
│                           # Content transformation and validation
│
├── repository-jaxrs/       # JAX-RS REST API for content operations
│                           # HTTP endpoints for import/export
│
├── demo/                   # Demo applications and examples
│                           # Sample import/export implementations
│                           # Integration examples
│
├── updater-examples/       # Content updater examples
│                           # Sample processors for content transformation
│
└── src/                    # Root-level source files
```

## Build Commands

### Build All Modules

```bash
# Build entire project (all modules)
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests

# Generate documentation locally
mvn clean install
mvn clean site

# Generate GitHub Pages documentation
mvn clean install
find docs -name "*.html" -exec rm {} \;
mvn -Pgithub.pages clean site
```

### Build Specific Modules

```bash
# Core module only
cd core && mvn clean install

# Repository JAX-RS module only
cd repository-jaxrs && mvn clean install

# Demo module only
cd demo && mvn clean install

# Updater examples only
cd updater-examples && mvn clean install
```

### Testing

```bash
# Run all unit tests
mvn test

# Run tests in specific module
cd core && mvn test
cd repository-jaxrs && mvn test
cd demo && mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Run specific test method
mvn test -Dtest=ClassName#methodName

# Run with debug output
mvn test -X
```

### Other Maven Goals

```bash
# Generate aggregate Javadocs
mvn javadoc:javadoc

# Generate site documentation
mvn site

# Code quality analysis
mvn verify
```

## Directory Structure

```
content-export-import/
├── core/                      # Core import/export engine
│   ├── src/main/java/        # Java source code
│   ├── src/test/java/        # Unit tests
│   └── pom.xml
│
├── repository-jaxrs/         # JAX-RS REST API
│   ├── src/main/java/        # REST endpoint implementations
│   ├── src/test/java/        # API tests
│   └── pom.xml
│
├── demo/                     # Demo applications
│   ├── src/main/java/        # Demo implementations
│   ├── src/test/java/        # Demo tests
│   └── pom.xml
│
├── updater-examples/         # Content updater examples
│   ├── src/main/java/        # Example updater implementations
│   └── pom.xml
│
├── src/                      # Root-level integration
├── pom.xml                   # Master POM
├── README.md                 # Project README
├── LICENSE                   # Apache 2.0 License
└── NOTICE                    # License notices
```

## Key Architecture Patterns

### Core Module
- **JCR Content Access**: Direct interaction with Bloomreach repository
- **Workflow Integration**: Document lifecycle management via Hippo Workflow API
- **POJO Binding**: Automatic JCR-to-Java object mapping via Hippo POJO Bind library
- **Content Validation**: Data integrity checking and transformation
- **Image Processing**: Thumbnail generation with Gallery Magick

### Repository JAX-RS Module
- **REST API Pattern**: HTTP endpoints for import/export operations
- **JSON Serialization**: Content representation in JSON format
- **Request/Response Mapping**: Conversion between HTTP and internal formats

### Demo Module
- **Reference Implementation**: Example content import/export flows
- **Integration Patterns**: Best practices for using the libraries
- **Test Data**: Sample content for demonstration

## Technology Stack

### Java Backend
- **JCR**: Apache Jackrabbit (via Bloomreach)
- **Workflow**: Hippo Workflow API
- **POJO Binding**: Hippo POJO Bind JCR library (3.1.0)
- **Image Processing**: Gallery Magick (4.1.0)
- **REST Services**: JAX-RS (likely Apache CXF)
- **File Systems**: Apache Commons VFS2 (2.10.0)
- **Build**: Apache Maven 3.x
- **Java**: Java 17+

### Testing Frameworks
- **JUnit**: Primary testing framework
- **Mocking**: Standard Java mocking libraries
- **Assertions**: AssertJ or similar

## Development Workflow

### Git Branch Strategy
- **Main branch**: `develop` (active development)
- **Release branches**: For release versions
- **Feature branches**: For new features and fixes

### Maven Conventions
- **Parent POM**: Uses `hippo-cms7-release` for version management
- **Module Naming**: `content-exim-{name}` convention
- **API First**: Public interfaces in dedicated modules

## Common Development Tasks

### Working with Core Module

**1. Adding Content Import Functionality:**
- Define content mapping in core module
- Create POJO binding annotations
- Implement content validation logic
- Add workflow integration if needed
- Write unit tests for the import process

**2. Adding Export Functionality:**
- Define content extraction logic
- Implement serialization to output format (XML, JSON, etc.)
- Add content filtering and transformation
- Include thumbnail/asset handling
- Test with various content types

**3. Creating REST API Endpoints:**
- Define JAX-RS resource classes in repository-jaxrs
- Map HTTP requests to core functionality
- Implement request validation and error handling
- Document endpoints with Javadoc

**4. Building Content Updaters:**
- Extend updater-examples with custom logic
- Implement content transformation rules
- Add validation for transformed content
- Test with sample data from demo module

### Testing Approaches

**Unit Tests:**
- Test core logic in isolation
- Mock JCR sessions and content
- Verify transformations and validations
- Test POJO binding mappings

**Integration Tests:**
- Test with actual content
- Verify workflow integration
- Test end-to-end import/export flows
- Validate REST API endpoints

## Documentation

### Local Generation

Generate and view documentation locally:
```bash
mvn clean install
mvn clean site
# Open target/site/index.html in a browser
```

### Online Documentation
- GitHub Pages: https://bloomreach-forge.github.io/content-export-import/
- Generated from master branch
- Includes API reference and usage guides

### Key Documentation Areas
- Import/Export API reference
- REST endpoint documentation
- POJO binding configuration
- Content updater examples
- Integration guides with brXM

## Important Development Notes

### Best Practices
- **Content Validation**: Always validate imported content against business rules
- **Error Handling**: Gracefully handle malformed input and missing dependencies
- **Performance**: Consider batch operations for large content imports
- **Backward Compatibility**: Maintain compatibility across versions when possible
- **Testing**: Comprehensive testing with various content types and structures

### Common Pitfalls
- Don't skip content validation to improve performance
- Always handle missing or null properties gracefully
- Test with actual Bloomreach content types
- Consider workflow state transitions during import
- Validate permissions before attempting content operations
- Handle large binary assets efficiently

### Security Considerations
- Validate input at API boundaries
- Respect JCR security and permissions
- Sanitize content during import to prevent injection
- Secure REST endpoints appropriately
- Never commit credentials or sensitive data

## Quick Start for Common Scenarios

### Scenario 1: Fix a Bug in Core Module
```bash
cd core
# Read relevant files using Read tool
# Make changes using Edit tool
mvn test -Dtest=RelevantTest
mvn clean install
```

### Scenario 2: Add a New REST Endpoint
```bash
cd repository-jaxrs
# Create new JAX-RS resource class
# Implement endpoint methods
mvn test
mvn clean install
```

### Scenario 3: Create a Content Updater
```bash
cd updater-examples
# Create new updater class implementing content transformation
# Add configuration and validation logic
mvn test
mvn clean install
```

### Scenario 4: Full Project Build
```bash
# From project root
mvn clean install -DskipTests  # Fast build
# Or with tests
mvn clean install
```

## Module Dependencies

The modules typically depend on each other in this order:
1. **core** - Independent, core functionality
2. **repository-jaxrs** - Depends on core
3. **updater-examples** - Depends on core
4. **demo** - May depend on core and others

## Running Tests

```bash
# Run all tests
mvn test

# Run tests in specific module
mvn test -pl core

# Run specific test class
mvn test -Dtest=ImportTest

# Run specific test method
mvn test -Dtest=ImportTest#testImportContent

# Run with debug output
mvn test -X
```

## Integration with Bloomreach Experience Manager

The Content-ExIm library integrates with brXM in several ways:

1. **JCR Repository**: Reads/writes content to the brXM JCR repository
2. **Workflow API**: Manages document lifecycle and transitions
3. **Configuration**: Uses brXM configuration for content types and validation rules
4. **Security**: Respects brXM user permissions and security policies
5. **Plugins**: Can be extended with custom plugins for specific content types

## Building for Production

```bash
# Full clean build with tests
mvn clean install

# Build with all profiles
mvn clean install -Pall-profiles

# Generate documentation for release
mvn clean site
mvn -Pgithub.pages clean site
```

## License

Apache License 2.0 - Open Source

## Getting Help

- **Issues**: https://issues.onehippo.com/projects/FORGE
- **GitHub**: https://github.com/bloomreach-forge/content-export-import
- **Documentation**: https://bloomreach-forge.github.io/content-export-import/
