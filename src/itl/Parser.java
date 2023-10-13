package itl;

import static itl.TokenType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Parser {

    public static class ParseError extends RuntimeException { }


    public final List<Token> tokens;
    public int current = 0;

    // Take in tokens
    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while(!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    public Expr expression() {
        return assignment();
    }

    public Stmt declaration() {
        try {
            if(match(FUNCTION)) return function("function");
            if(match(VAR)) return varDeclaration();

            return statement();
        } catch(ParseError error) {
            synchronize();
            return null;
        }
    }

    public Stmt statement() {
        if(match(FOR)) return forStatement();
        if(match(IF)) return ifStatement();
        if(match(PRINT)) return printStatement();
        if(match(RETURN)) return returnStatement();
        if(match(WHILE)) return whileStatement();
        if(match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '('");

        // Initialiser

        Stmt initialiser;
        if(match(SEMICOLON)) {
            initialiser = null;
        } else if (match(VAR)) {
            initialiser = varDeclaration();
        } else {
            initialiser = expressionStatement();
        }

        Expr condition = null;
        if(!check(SEMICOLON)) {
            condition = expression();
        }

        consume(SEMICOLON, "Expect ';' after loop condition");

        Expr increment = null;
        if(!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after clauses");
        Stmt body = statement();


        // Takes the block body, and append the increment statement to the end
        if(increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        // Use a while loop as the underlying loop
        // If no condition -> means infinite loop
        if(condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        // Add intialioser to the start of the block
        if(initialiser != null) {
            body = new Stmt.Block(Arrays.asList(initialiser, body));
        }

        return body;
    }


    public Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.Expression(expr);
    }

    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");

        consume(LEFT_PAREN, "Expected a '(' after " + kind + " name.");

        List<Token> parameters = new ArrayList<>();
        if(!check(RIGHT_PAREN)) {
            do {
                if(parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 operators for some reason. Dont ask me");
                }

                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while(match(COMMA));
        }

        consume(RIGHT_PAREN, "Expected a ')' after parameters");

        consume(LEFT_BRACE, "Expected a '{' before " + kind + " name.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);

    }

    public List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while(!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '{' after block.");
        return statements;
    }

    public Expr assignment() {
        Expr expr = or();

        if(match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if(expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

         //  OR -> 
         //  AND ( "or" AND ) 

        while(match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while(match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    // Use {  as delimiter instead?

    private Stmt ifStatement() {


        // Code for brackets
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        // Match() or peek() ?
        // if(!check(LEFT_BRACE)) ITL.error(peek(), "Expected Right Paren after if statement");

        // if(peek().type != RIGHT_BRACE) ITL.error(peek(), "booya");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if(match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    public Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    public Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if(!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expected a ';' after return value");
        return new Stmt.Return(keyword, value);
    }

    public Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name");

        Expr initializer = null;
        if(match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '('");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect '('");

        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    public Expr equality() {
        Expr expr = comparison();
        while(match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    public Expr comparison() {
        Expr expr = term();
        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    public Expr term() {
        Expr expr = factor();
        while(match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    public Expr factor() {
        Expr expr = unary();
        while(match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    public Expr unary() {
        if(match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguements = new ArrayList<>();
        // If isnt rp, then is arguement, add to list
        if(!check(RIGHT_PAREN)) {
            do {
                if(arguements.size() >= 255) error(peek(), "Can't have that many arguements dude");
                arguements.add(expression());
            } while(match(COMMA));
        }

        // Consume paren
        Token paren = consume(RIGHT_PAREN, "Expected ')' after arguements");

        return new Expr.Call(callee, paren, arguements);
    }

    private Expr call() {
        Expr expr = primary();

        while(true) {
            if(match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }

        return expr;
    }



    public Expr primary() {
        if(match(FALSE)) return new Expr.Literal(false);
        if(match(TRUE)) return new Expr.Literal(true);
        if(match(NIL)) return new Expr.Literal(null);

        if(match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if(match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if(match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression");

    }

    // consumes token if it has any of the given types
    public boolean match(TokenType... types) {
        for(TokenType type : types) {
            if(check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    // Like match, but checks for error
    public Token consume(TokenType type, String message) {
        if(check(type)) return advance();
        throw error(peek(), message);
    }

    // return true if current is type : Doesnt consume
    public boolean check(TokenType type) {
        if(isAtEnd()) return false;
        return peek().type == type;
    }

    public Token advance() {
        if(!isAtEnd()) current++;
        return previous();
    }

    // If no more tokens
    public boolean isAtEnd() {
        return peek().type == EOF;
    }

    // next to be consumed
    public Token peek() {
        return tokens.get(current);
    }

    // Most recently consumed
    public Token previous() {
        return tokens.get(current - 1);
    }

    public ParseError error(Token token, String message) {
        Main.error(token, message);
        return new ParseError();
    }

    public void synchronize() {
        advance();

        while(!isAtEnd()) {
            if(previous().type == SEMICOLON) return;

            switch(peek().type) {
                case CLASS:
                case FUNCTION:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default:
                    break;
            }

            advance();
        }
    }



}
