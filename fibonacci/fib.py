import sys


def fib(n):
    a, b = 0, 1
    for __ in range(n):
        a, b = b, a+b
    return a


def main():
    args = sys.argv[1:]

    if len(args) != 1:
        raise Exception("Number of args != 1")

    n = int(args[0])
    if n <= 0 or n > 500:
        raise Exception("Incorrect input")
    else:
        return fib(n)


if __name__ == "__main__":
    print(main())
