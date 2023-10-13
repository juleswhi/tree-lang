package itl;

import static itl.TokenType.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Scanner {
    public final String source;
    public final List<Token> tokens = new ArrayList<>();
    
    public int start = 0;
    public int current = 0;
    public int line = 1;

    public static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("function",    FUNCTION);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while(!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    public void scanToken() {
        char c = advance();
        switch(c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '/': addToken(SLASH); break;
            // Should check if = is actually == and not just =
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            // Dont call addToken() because ignore comments
            case '#':
                while (peek() != '\n' && !isAtEnd())
                    advance();
                break;

            // Also skip over whitespace and newlines
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;

            case '"': string(); break;

            // reports error and sets hadError to true
            default:
                if(isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                }
                else {
                    Main.error(line, "Unexpeced character.");
                }
                break;
        }
    }

    public void identifier() {
        while(isAlphaNumberic(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text.toLowerCase());
        if(type == null) type = IDENTIFIER;
        addToken(type);
    }


    public void number() {
        // consiume digits until not number
        while(isDigit(peek())) advance();

        // if dot and after dot is a number, keep advancing
        // dont allow trailing dots
        if(peek() == '.' && isDigit(peekNext())) {
            advance();

            while(isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));


    }


    public void string() {
        // Consume chars until "
        while(peek() != '"' && !isAtEnd()) {
            if(peek() == '\n') line++;
            advance();
        }

        if(isAtEnd()) {
            Main.error(line, "Unterminated string");
            return;
        }

        // consime the closing "
        advance();

        // Trim quots
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    // Only consume char if matches
    public boolean match(char expected) {
        if(isAtEnd()) return false;
        if(source.charAt(current) != expected) return false;
        
        current++;
        return true;
    }

    // advance but doesnt consume next char
    public char peek() {
        if(isAtEnd()) return '\0';
        return source.charAt(current);
    }

    public char peekNext() {
        if(current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    // Make sure identifiers are using the correct letters we want
    public boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    public boolean isAlphaNumberic(char c) {
        return isAlpha(c) || isDigit(c);
    }

    public boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public boolean isAtEnd() {
        return current >= source.length();
    }

    // consume next char in sf
    public char advance() {
        return source.charAt(current++);
    }

    public void addToken(TokenType type) {
        addToken(type, null);
    }

    // add token to list
    public void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
