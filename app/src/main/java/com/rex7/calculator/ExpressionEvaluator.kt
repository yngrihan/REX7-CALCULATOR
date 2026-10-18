package com.rex7.calculator

import java.util.Stack

object ExpressionEvaluator {
    fun evaluate(expression: String): Double {
        val sanitized = expression.replace("×", "*").replace("÷", "/").replace("−", "-")
        val tokens = tokenize(sanitized)
        return evalRPN(toRPN(tokens))
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c in "+-*/%()" -> {
                    if (c == '-' && (tokens.isEmpty() || tokens.last() in "+-*/%(")) {
                        var num = "-"
                        i++
                        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                            num += expr[i]
                            i++
                        }
                        tokens.add(num)
                    } else {
                        tokens.add(c.toString())
                        i++
                    }
                }
                c.isDigit() || c == '.' -> {
                    var num = ""
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        num += expr[i]
                        i++
                    }
                    tokens.add(num)
                }
                else -> i++
            }
        }
        return tokens
    }

    private fun precedence(op: String): Int = when (op) {
        "+", "-" -> 1
        "*", "/", "%" -> 2
        else -> 0
    }

    private fun toRPN(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val ops = Stack<String>()
        for (token in tokens) {
            when {
                token.toDoubleOrNull() != null -> output.add(token)
                token == "(" -> ops.push(token)
                token == ")" -> {
                    while (ops.isNotEmpty() && ops.peek() != "(") {
                        output.add(ops.pop())
                    }
                    if (ops.isNotEmpty()) ops.pop()
                }
                else -> {
                    while (ops.isNotEmpty() && precedence(ops.peek()) >= precedence(token)) {
                        output.add(ops.pop())
                    }
                    ops.push(token)
                }
            }
        }
        while (ops.isNotEmpty()) {
            output.add(ops.pop())
        }
        return output
    }

    private fun evalRPN(tokens: List<String>): Double {
        val stack = Stack<Double>()
        for (token in tokens) {
            val num = token.toDoubleOrNull()
            if (num != null) {
                stack.push(num)
            } else {
                if (stack.size < 2) throw IllegalArgumentException("Invalid syntax")
                val b = stack.pop()
                val a = stack.pop()
                val res = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> {
                        if (b == 0.0) throw ArithmeticException("Division by zero")
                        a / b
                    }
                    "%" -> a % b
                    else -> 0.0
                }
                stack.push(res)
            }
        }
        return if (stack.isNotEmpty()) stack.pop() else 0.0
    }
}
