import unittest
from fib import fib


class TestFibonacci(unittest.TestCase):
    def test_fibonacci_1(self):
        self.assertEqual(fib(1), 1)

    def test_fibonacci_2(self):
        self.assertEqual(fib(2), 1)

    def test_fibonacci_3(self):
        self.assertEqual(fib(3), 2)

    def test_fibonacci_5(self):
        self.assertEqual(fib(5), 5)

    def test_fibonacci_10(self):
        self.assertEqual(fib(10), 55)

    # def test_fibonacci_negative(self):
    #     self.assertEqual(fib(-1), "Incorrect input")


if __name__ == '__main__':
    unittest.main()
