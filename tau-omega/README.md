# Omega is a serverless execution engine for Tau

User defined code execution is critical for any rule based platform i.e. when the rule
matches execute the code defined by the user.

This can allow for building simple apps that are nothing more than a chain of rules
responding to events and executing custom code.

There are 2 types of executors in Omega:
1. script based executors
2. code based executors

# Supported Languages

1. Javascript
2. JRuby
3. Jython (planned)

Note: Currently anything compliant with JSR223 can be added to the execution platform for script executor.

# Script documentation

All scripts MUST return boolean value indicating whether or not the function was able to successfully execute.
Script has event and logger variables available to be used for processing.

*Note: There is no constraints around function name or arguments, users can define the names and formatting of the script. The only requirement is the function MUST return a boolean value.*

Following variables are available into the execution context:

1. event
2. logger

# Examples:

### Javascript:
```javascript
function f()
{ 
	logger.log('hello');
	return true;
}

// NOTE: function f returns a boolean value
f();
```

### JRuby:
```ruby
def f(logger)
	logger.log('hello')
	return true
end

# NOTE: function f returns a boolean value
f(logger)
```