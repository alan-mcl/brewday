# Brewday Improvement Tasks

## Architecture and Design
[ ] Implement a formal layered architecture with clear separation of concerns (data, business logic, UI)
[ ] Refactor the singleton pattern usage to improve testability and reduce tight coupling
[ ] Implement a proper dependency injection system to reduce direct dependencies on Database and Brewday singletons
[ ] Create a formal event system for communication between components instead of direct method calls
[ ] Implement a proper plugin architecture for extensibility (e.g., for new calculation models, water profiles)
[ ] Refactor the process flow visualization to use a proper directed acyclic graph (DAG) implementation

## Database and Data Management
[ ] Implement database versioning and migration system for easier upgrades
[ ] Add data validation layer to ensure data integrity
[ ] Improve error handling and recovery for database operations
[ ] Complete the Git backend implementation for better synchronization
[ ] Implement additional storage backends (Dropbox, Google Drive) as mentioned in todo list
[ ] Add database backup scheduling and management
[ ] Implement proper transaction support for database operations

## Testing and Quality Assurance
[ ] Implement a proper testing framework (JUnit) instead of manual test classes
[ ] Create unit tests for core calculation methods in the Equations class
[ ] Implement integration tests for process flows and recipes
[ ] Add automated UI tests using TestFX or similar
[ ] Implement continuous integration to run tests automatically
[ ] Add code coverage reporting to identify untested code
[ ] Create a formal QA process for new features and bug fixes

## Performance Optimization
[ ] Profile the application to identify performance bottlenecks
[ ] Optimize complex calculations in the Equations class
[ ] Implement caching for frequently used calculations
[ ] Optimize database operations, especially for large datasets
[ ] Improve UI rendering performance for complex recipe visualizations
[ ] Reduce memory usage by implementing more efficient data structures
[ ] Optimize startup time by lazy-loading non-essential components

## User Interface Improvements
[ ] Implement responsive design for better support of different screen sizes
[ ] Add more tooltips and contextual help as mentioned in todo list
[ ] Improve accessibility features (keyboard navigation, screen reader support)
[ ] Implement user-selectable units of measurement as mentioned in todo list
[ ] Add dark mode theme support
[ ] Improve the recipe visualization with interactive elements
[ ] Implement drag-and-drop for process steps and ingredient additions

## Documentation
[ ] Create comprehensive JavaDoc for all public classes and methods
[ ] Develop user documentation with tutorials and examples
[ ] Add inline code comments for complex algorithms and calculations
[ ] Create developer documentation for extending the application
[ ] Document the database schema and data models
[ ] Create a style guide for UI components and interactions
[ ] Document the build and release process

## Feature Enhancements
[ ] Implement grain bill percentage adjuster tool as mentioned in todo list
[ ] Add keg line length calculator as mentioned in todo list
[ ] Support multiple yeast additions and yeast blends as mentioned in todo list
[ ] Implement freeze concentrate step for eisbocks as mentioned in todo list
[ ] Add support for hop extracts and liquid forms as mentioned in todo list
[ ] Implement additional mash pH models (ZpH, Kaiser Water) as mentioned in todo list
[ ] Add yeast viability/cell count estimation as mentioned in todo list

## Bug Fixes
[ ] Fix NPX JavaFX control skin issue mentioned in todo list
[ ] Fix bug where copying stand step doesn't copy ingredients
[ ] Address carbonation impact on ABV calculation as mentioned in todo list
[ ] Fix any issues with pH calculations in different models
[ ] Resolve any UI rendering issues in different themes
[ ] Fix potential memory leaks in long-running operations
[ ] Address any thread safety issues in concurrent operations

## Code Refactoring
[ ] Break up large classes (Equations.java, JfxUi.java) into smaller, more focused classes
[ ] Reduce code duplication, especially in calculation methods
[ ] Improve naming conventions for better code readability
[ ] Refactor complex methods to improve maintainability
[ ] Apply consistent error handling patterns throughout the codebase
[ ] Modernize code to use Java 14 features where appropriate
[ ] Implement design patterns where they would improve code structure

## Build and Deployment
[ ] Modernize the build system (consider Gradle or Maven instead of Ant)
[ ] Implement automated release process
[ ] Add support for continuous deployment
[ ] Improve dependency management
[ ] Create installation packages for different platforms
[ ] Implement auto-update functionality
[ ] Optimize the bundled JRE and JavaFX for smaller distribution size