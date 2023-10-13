package itl;

import java.util.List;
import java.io.*;
import itl.Stmt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{

    // This holds a fixed ref to outermost env
    final Environment globals = new Environment();
    public Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
        globals.define("clock", new ItlCallable() {
            // Takes no arugments
            @Override
            public int arity() { return 0; }

            @Override 
            public Object call(Interpreter interpreter, List<Object> arguements) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("sendToTree", new ItlCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguements) {

                try {
                    Process process = Runtime.getRuntime().exec(new String[] { "powershell.exe", "echo", (String)arguements.get(0),"> ", "coords.txt" });

                    while(process.isAlive()) { }

                    Process python = Runtime.getRuntime().exec(new String[] { "powershell.exe", "python3.11.exe", "wsClient.py"});
                    while(python.isAlive()) {}
                    return arguements.get(0);
                } catch(IOException e) {
                    e.printStackTrace();
                }

                return (String)arguements.get(0);
            }

            @Override
            public String toString() { return "<native fn>"; } 

        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for(Stmt statement : statements) {
                execute(statement);
            }
        } catch(RuntimeError error) {
            Main.runtimeError(error);
        }
    }


    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        // If left -> true, dont need check right
        if(expr.operator.type == TokenType.OR) {
            if(isTruthy(left)) return left;
        // Same as above but inversed
        } else {
            if(!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch(expr.operator.type) {

            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);

            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PLUS:
                checkNumberOperands(expr.operator, left, right);
                if(left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                if(left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }

            default:
            return new Object();

        }
    }
    

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        // just indentifier which gets expr from name
        Object callee = evaluate(expr.callee);

        List<Object> arguements = new ArrayList<>();
        for (Expr arguement : expr.arguements) {
            arguements.add(evaluate(arguement));
        }

        if(!(callee instanceof ItlCallable)) {
            throw new RuntimeError(expr.paren, "You can only call functions and classes");
        }

        ItlCallable function = (ItlCallable)callee;

        // arity -> num arguements
        if(arguements.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " number of arguments, but instead got: " + arguements.size());
        }

        return function.call(this, arguements);
    }

    public void checkNumberOperands(Token operator, Object left, Object right) {
        if(left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch(expr.operator.type) {
            case MINUS: 
            checkNumberOperand(expr.operator, right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
            default: return null;
        }
    }

    public void checkNumberOperand(Token operator, Object operand) {
        if(operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    public boolean isTruthy(Object object) {
        // NIL and FASLE are falsey
        if(object == null) return false;
        if(object instanceof Boolean) return (boolean)object;
        return true;
    }

    public boolean isEqual(Object a, Object b) {
        if(a == null && b == null) return true;
        if(a == null) return false;

        return a.equals(b);
    }

    public String stringify(Object object) {
        if(object == null) return "nil";
        if(object instanceof Double) {
            String text = object.toString();
            if(text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }

    public Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    public void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for(Stmt statement : statements) {
                execute(statement);
            } 
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        ItlFunction function = new ItlFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if(isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if(stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }


    @Override
    public Void visitPrintStmt(Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }


    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if(stmt.value != null) value = evaluate(stmt.value);
        throw new Return(value);
    }
    
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if(stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while(isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if(distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);
        if(distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }

        return value;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

}
