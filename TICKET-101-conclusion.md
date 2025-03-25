# TICKET-101 conclusion

## Positive aspects
- The project structure in general follows the best practices
- Good separation of concerns
- Extensive unittests
- Descriptive custom exceptions

## Places for improvement

### Business logic errors
- Maximum loan period is set to 60 in the config, but the requirements specify it as 48
- DecisionEngine does not take the credit score (as described by specification) into consideration when determing the desicion

### Wrong variable scope
- DecisionResponse should not be object-level variable, instead a new DecisionResponse shall be created each request
- creditModifier in DecisionEngine should not be object-level, it should be scoped to the calculateApprovedLoan method

### Wrong usage of Spring Boot annotations
- DecisionResponse should not have @Component as it's a simple DTO

### Unnessesary boilerplate
- DecisionEngineController has lots of repeating code that can be simplified
- The custom exception classes can be simplified by not overriding the get methods and changing the constructors to pass the arguments to the superclass constructor
- Some boilerplate can be removed with Lombok (by using @AllArgsContructor etc.)

### Nitpicks
- Logging would be nice
- Inconsistent creation of the ResponseEntity object in DecisionEngineController
- Decision, DecisionResponse and DecisionRequest are all DTO-s, I would put them in a seperate module called "dto"
- Decision and DecisionResponse are basically the same class, why need them both?
- DecisionEngineController is in a module called "endpoint", I would rename the module to "controller" to follow best practices

## Most important shortcoming
The decision engine does not determine the loan based on the credit score and the constraints as specified in the requirements
