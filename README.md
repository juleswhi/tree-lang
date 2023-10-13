# **_Interpreted Tree Language_**

Interpreted.

Dynamically typed.

Easy syntax.


# Get started!

[Download] the jvm first!

## Printing

```py
print "Hello, World!";
```

_Note: statements are NOT case sensitive, this will also work_

_For simplicity, we will stick to lowercase for the rest of this guide_

```py
PRINT "Hello, World!";
```

## Variables

To declare a variable, use `var`;

```py
var x = 1;
```

**_itl_** is dynamically typed, so no need to worry about types:

```py
var x = "Hello, World";
```

You can redeclare variables in the same scope:

```py
var x = 5;
var x = 10;

print x; 
# 10
```

You can also make your own scope by using `{ }`:

```py
var x = 5;

{
    var x = 10;
    print x;
}

print x;

# 10
# 5
```

## Control Flow

`if` statements look like this:

```py
if ( true ) { }
```

Brackets can also be used to separate concerns

```py
if ( ( 1 + 2 ) == 3 ) { }
```

In addition, you can also use one-liners:

```py
if ( true ) print "hello!";
```

For multiple branches, use the `else if` or `else` keywords:

```py
if ( false ) {} 
else if ( !true ) {}
else { 
    print "Hello, World!"; 
}
```




## `AND` and `OR`

To evaluate truth, you can use the `AND` or `OR` operators:

In **_itl_**, only `nil` and `false` are _not_ truthy.

Everything else is truthy.

```py
print "Tree" or 1; 
# "Tree"
# Both are truthy, so return the first statement
```

```py
print nil or "Lang"; 
# "Lang"
# nil is falsey, and "Lang" is truthy
```

### Looping

To do a `while` loop:

```cs
while ( condition ) {
    # Code
}
```

In practice, that looks like:

```py
var x = 1;
while ( x < 100) {
    print x;
    x = x + 1;
}
# 1 2 3 ... 98 99
```

To do a `for` loop:

```py
for ( initialisation; condition; increment ) {
    # Code here
}
```

In practice, that looks like:

```py
for( var x = 0; x < 100; x = x + 1) {
    print x;
}
# 0 1 2 3 ... 98 99
```


## Methods

`Methods` are easy in **_itl_**, heres how you do them:

```py
function name(parameters) { }
```

_Note: functions have to be **Above** any function calls_

In practice, that looks like this:
```py
function fib(n) {
  if (n <= 1) return n;
  return fib(n - 2) + fib(n - 1);
}

for (var i = 0; i < 20; i = i + 1) {
  print fib(i);
}
# Print the fibonacci numbers
```

Functions can also be places inside other functions.

This is because blah blah blah.

```py
function do_something() {
    function do_something_else() {
        print "Hello, World!";
    }
    do_something_else();
}

do_something();

```
