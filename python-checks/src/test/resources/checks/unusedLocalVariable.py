unread_global = 1

def f(unread_param):
    global unread_global
    unread_global = 1
    unread_local = 1 # Noncompliant
    unread_local = 2 # Noncompliant
    read_local = 1
    print(read_local)
    read_in_nested_function = 1
    def nested_function():
        print(read_in_nested_function)

def using_locals(a, b):
  c = a + b
  # "locals" will include the "c" value
  return locals()

def string_interpolation():
    value1 = 1
    value2 = 2
    value3 = 3 # Noncompliant
    value4 = 4 # Noncompliant (previously FN)
    value5 = 1
    foo(F'{value5} foo')
    value6 = '' # Noncompliant FP
    print(f"{'}' + value6}")
    return f'{value1}, {2*value2}, value3bis, value4'

def function_with_lambdas():
    print([(lambda unread_lambda_param: 2)(i) for i in range(10)])
    x = 42 # Noncompliant
    print([(lambda x: x*x)(i) for i in range(10)])
    y = 42
    print([(lambda x: x*x + y)(i) for i in range(10)])
    {y**2 for a in range(3) if lambda x: x > 1 and y > 1} # Noncompliant
#             ^

def using_tuples():
    x, y = (1, 2)
    print x
    (a, b) = (1, 2)
    print b

    for name, b in foo():
        pass
    for (c, d) in foo():
        pass

def for_loops():
    for _ in range(10):
        do_something()
    for j in range(10): # Noncompliant
        do_something()
    for i in range(10):
        do_something(i)

def unused_import():
    import foo        # OK, should be handled in a dedicated rule
    from x import y   # OK, should be handled in a dedicated rule
