package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {
    // throw parse except everywhere where it cant be lexed
    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        boolean quotesOpened = false;
        List<Token> tokenList = new ArrayList<Token> ();
        char curr = chars.get(0); //setting current character to beginning

        //until reached end of input str
        while(chars.index < chars.input.length()){
            //if next chars are whitespace, match to nothing, advance char stream

            //Important: match and peek only check 1 char at a time
            if(peek(" |\b|\n|\r|\t")){
                //updating index instead of advancing to not change the length
                chars.index++;
            }
            else{
                tokenList.add(lexToken());
            }
        }
        return tokenList;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {

        if(peek("@?[A-Za-z]")){
            return lexIdentifier();
        }
        //int
        else if(peek("-|[1-9]|0")){
            return lexNumber();
        }
        //dec
        else if(peek("-?(0|[1-9][0-9]+)\\.[0-9]+")){
            return lexNumber();
        }
        else if(peek("[']([^'\\n\\r\\\\]|\\\\[bnrt'\"\\\\])[']")){
            return  lexCharacter();
        }
        else if(peek("\"")){
            return lexString();
        }
        else if(peek("[^a-zA-Z0-9\"]")){
            return lexOperator();
        }
        else if(peek("\b|\n|\r|\t")){
            //THROW AN ERROR
            lexEscape();
            throw new ParseException("Parse exception at index: " + Integer.toString(chars.index), chars.index);
        }
        else{
            //THROW AN ERROR
            throw new ParseException("Parse exception at index: " + Integer.toString(chars.index), chars.index);
        }
    }

    public Token lexIdentifier() {
        //here we emit token i think

        while(peek("@?[A-Za-z0-9_-]*")){
            match("@?[A-Za-z0-9_-]*");
        }

        return chars.emit(Token.Type.IDENTIFIER);

        //throw new UnsupportedOperationException();
    }

    public Token lexNumber() {
        if(peek("0")){
            match("0");

            if(peek("0|-|[1-9]")){
                throw new ParseException("Parse exception", chars.index);
            }
            else if(peek("\\.")){
                match("\\.");
                if(peek("[0-9]")){
                    while(peek("[0-9]")){
                        match("[0-9]");
                    }
                    return chars.emit(Token.Type.DECIMAL);
                } else {
                    throw new ParseException("Parse exception", chars.index);
                }

            }
            return chars.emit(Token.Type.INTEGER);
        }

        if (peek("-|[1-9]")) {
            while(peek("-|[1-9]")){
                match("-|[1-9]");
            }
            if(peek("\\.")){
                match("\\.");
                if(peek("[0-9]")){
                    while(peek("[0-9]")){
                        match("[0-9]");
                    }
                    return chars.emit(Token.Type.DECIMAL);
                } else {
                    throw new ParseException("Parse exception", chars.index);
                }
            }

            return chars.emit(Token.Type.INTEGER);
        }
        else {
            throw new ParseException("Parse exception", chars.index);
        }
    }

    public Token lexCharacter() {
        while(peek("[']([^'\\n\\r\\\\]|'\\'[bnrt'\"\\\\])[']")){
            match("[']([^'\\n\\r\\\\]|'\\'[bnrt'\"\\\\])[']");
        }

        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        // use lexEscape() when escape character is detected
        boolean quotesOpen = false;
        if(peek("\"")){
            quotesOpen = true;
            match("\"");
        }

        while(peek("[^\"]")){
            if(peek("\b|\n|\r|\t")){
                lexEscape();
            } else if(peek("\\\\")){
                match("\\\\");
                if(peek("[bnrt]")){
                    lexEscape();
                } else {
                    throw new ParseException("Parse exception: invalid escape", chars.index);
                }
            } else {
                match("[^\"]");
            }
        }

        //second quotes in empty string
        if(peek("\"")){
            quotesOpen = false;
            match("\"");
        }

        if(quotesOpen){
            throw new ParseException("Parse exception: unterminated quotes", chars.index);
        } else {
            return chars.emit(Token.Type.STRING);
        }
    }

    public void lexEscape() {
        while(peek("\b|\n|\r|\t")){
            match("\b|\n|\r|\t");
        }
    }

    public Token lexOperator() {

        while(peek("[^a-zA-Z0-9\"]")){
            // may be more efficent using advance()
            if(peek("\b|\n|\r|\t")){
                lexEscape();
                throw new ParseException("Parse exception: escape in operator", chars.index);
            }
            else if(!peek("[<>!]")){
                match("[^a-zA-Z0-9\"]");
                return chars.emit(Token.Type.OPERATOR);
            }
            match("[^a-zA-Z0-9\"]");
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for(int i = 0; i< patterns.length; i++){
            //chars from charStream
            if(!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])){
                return false;
            }
        }
        return true;
    }



    //test
    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if(peek) {

            for(int i = 0; i< patterns.length; i++){
                chars.advance();
            }

        }
        return peek;

        //throw new UnsupportedOperationException(); //TODO (in Lecture)
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}