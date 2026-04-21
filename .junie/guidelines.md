# Brewday Development Guidelines

This document provides essential information for developers working on the Brewday project.

## Build and Configuration Instructions

### Prerequisites
- JDK 14 (the project is configured to bundle JDK 14.0.2)
- JavaFX SDK 11 (the project is configured to bundle JavaFX SDK 11.0.2)
- Apache Ant for building
- Launch4j for creating Windows executable

### Building the Project
The project uses Apache Ant as its build system. The main build file is `build.xml` in the project root.

To build the project:
1. Ensure the paths in `build.xml` are correctly set for your environment:
   - `jre.to.bundle` - Path to JDK 14
   - `jfx.to.bundle` - Path to JavaFX SDK 11
   - `launch4j.dir` - Path to Launch4j installation

2. Run the build using Ant:
   ```
   ant dist
   ```

This will:
- Compile the Java source code
- Create the Brewday JAR file
- Package the application with all dependencies
- Create a distributable ZIP file

### Configuration
The application uses a configuration file `brewday.cfg` which can be found in the project root. The main configuration option is the database location:

```
mclachlan.brewday.db = path/to/database
```

## Testing Information

### Testing Approach
The project uses a simple manual testing approach rather than a formal testing framework. Test classes are located in `src/main/java/mclachlan/brewday/test/` and contain test methods that can be executed individually.

### Running Tests
To run tests:
1. Open the test class you want to run (e.g., `TestEquations.java`)
2. Uncomment the test methods you want to run in the `main` method
3. Run the class as a Java application
4. Check the console output for test results

### Adding New Tests
To add a new test:
1. Create a new test method in an existing test class or create a new test class
2. Follow the pattern of existing tests:
   - Use descriptive method names prefixed with "test"
   - Print test description and results to the console
   - Use assertions or manual verification of results
3. Add the test method to the main method (commented out by default)

### Example Test
Here's a simple example of how to create and run a test for the Brewday project:

```java
// TestExample.java
package mclachlan.brewday.test;

import mclachlan.brewday.math.*;

public class TestExample {
    public static void testSimpleCalculation() {
        System.out.println("TestExample.testSimpleCalculation");
        
        // Setup test data
        VolumeUnit volume = new VolumeUnit(10, Quantity.Unit.LITRES);
        TemperatureUnit initialTemp = new TemperatureUnit(20, Quantity.Unit.CELSIUS);
        TemperatureUnit targetTemp = new TemperatureUnit(70, Quantity.Unit.CELSIUS);
        PowerUnit heatingPower = new PowerUnit(2, Quantity.Unit.KILOWATT, false);
        
        // Execute the calculation
        TimeUnit heatingTime = Equations.calcHeatingTime(volume, initialTemp, targetTemp, heatingPower);
        
        // Verify the result
        System.out.println("Heating time: " + heatingTime.get(Quantity.Unit.MINUTES) + " minutes");
        
        // You could add assertions here if using a formal testing framework
    }
    
    public static void main(String[] args) throws Exception {
        // Uncomment the test you want to run
        testSimpleCalculation();
    }
}
```

## Additional Development Information

### Project Structure
- `src/main/java` - Java source code
- `data` - Application data files and resources
- `lib` - External libraries
- `test_data` - Test data files and database

### Code Style
- The project uses standard Java code style
- Classes are organized in packages by functionality
- Mathematical calculations are encapsulated in the `Equations` class
- Physical quantities are represented by specialized classes in the `math` package

### Debugging
- The application logs to the `log` directory
- Each run creates a timestamped log file
- The database is stored as JSON files, which can be examined directly for debugging

### Database
The backend uses JSON files for storage. The database location is configured in `brewday.cfg`. The database contains:
- Recipes
- Batches
- Inventory
- Reference data (ingredients, styles, etc.)