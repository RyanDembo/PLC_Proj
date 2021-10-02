package plc.project;

import jdk.nashorn.internal.parser.TokenType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        Ast.Expression exp = parseExpression();

        Ast.Statement.Expression expression = new Ast.Statement.Expression(exp);
        if(tokens.has(1)){
            if(match("=")){
                Ast.Expression exp1 = parseExpression();
                if(!match(";")){

                    if(tokens.has(0)) {
                        //System.out.println(tokens.get(0).getIndex());
                        throw new ParseException("Missing Semicolon", tokens.get(0).getIndex());
                    }
                    else{
                        //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                        throw new ParseException("Missing Semicolon", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    }
                }

                return new Ast.Statement.Assignment(exp, exp1);
            }else{
                throw new ParseException("invalid expression", tokens.get(-1).getIndex()+1);
            }

        }
        if(!match(";")){
            //System.out.println(tokens.get(0).getIndex());
            if(tokens.has(0)) {
                //System.out.println(tokens.get(0).getIndex());
                throw new ParseException("Missing Semicolon", tokens.get(0).getIndex());
            }
            else{
                //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                throw new ParseException("Missing Semicolon", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }
        }

        return expression;
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {

        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {

        Ast.Expression compExpression = parseComparisonExpression();

        if(tokens.has(0) && (match("&&") || match("||"))){
            String strOp = tokens.get(-1).getLiteral();

            if(!tokens.has(0)){
                throw new ParseException("invalid logical", tokens.get(-1).getIndex() + 1);
            }

            Ast.Expression.Binary expression = new Ast.Expression.Binary(strOp, compExpression, parseComparisonExpression());

            while(tokens.has(0) && (match("&&") || match("||"))){
                strOp = tokens.get(-1).getLiteral();
                if(tokens.has(0)){
                    expression = new Ast.Expression.Binary(strOp, expression, parseComparisonExpression());
                }else{
                    throw new ParseException("invalid logical", tokens.get(-1).getIndex() + 1);
                }
            }
            return expression;
        }
        return compExpression;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {

        Ast.Expression addExpression = parseAdditiveExpression();

        if(tokens.has(0) && (match("<") || match(">") || match("==") || match("!="))){
            String strOp = tokens.get(-1).getLiteral();

            if(!tokens.has(0)){
                throw new ParseException("invalid comparison", tokens.get(-1).getIndex() + 1);
            }

            Ast.Expression.Binary expression = new Ast.Expression.Binary(strOp, addExpression, parseAdditiveExpression());

            while(tokens.has(0) && (match("<") || match(">") || match("==") || match("!="))){
                strOp = tokens.get(-1).getLiteral();

                if(tokens.has(0)){
                    expression = new Ast.Expression.Binary(strOp, expression, parseAdditiveExpression());
                }else{
                    throw new ParseException("invalid comparison", tokens.get(-1).getIndex() + 1);
                }
            }
            return expression;
        }
        return addExpression;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression multExpression = parseMultiplicativeExpression();

        if(tokens.has(0) && (match("+") || match("-"))){
            String strOp = tokens.get(-1).getLiteral();

            if(!tokens.has(0)){
                throw new ParseException("invalid additive", tokens.get(-1).getIndex() + 1);
            }

            Ast.Expression.Binary expression = new Ast.Expression.Binary(strOp, multExpression, parseMultiplicativeExpression());

            while(tokens.has(0) && (match("+") || match("-"))){
                strOp = tokens.get(-1).getLiteral();
                if(tokens.has(0)){
                    expression = new Ast.Expression.Binary(strOp, expression, parseMultiplicativeExpression());
                }else{
                    throw new ParseException("invalid additive", tokens.get(-1).getIndex() + 1);
                }
            }
            return expression;
        }
        return multExpression;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression primExpression = parsePrimaryExpression();

        if(tokens.has(0) && (match("*") || match("/") || match("^"))){
            String strOp = tokens.get(-1).getLiteral();

            if(!tokens.has(0)){
                throw new ParseException("invalid multiplicative", tokens.get(-1).getIndex()+1);
            }

            Ast.Expression.Binary expression = new Ast.Expression.Binary(strOp, primExpression, parsePrimaryExpression());

            while(tokens.has(0) && (match("*") || match("/") || match("^"))){
                strOp = tokens.get(0).getLiteral();
                if(tokens.has(0)){
                    expression = new Ast.Expression.Binary(strOp, expression, parsePrimaryExpression());
                }else{
                    throw new ParseException("invalid multiplicative", tokens.get(-1).getIndex()+1);
                }
            }
            return expression;
        }
        return primExpression;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        if(match("NIL")){
            return new Ast.Expression.Literal(null);
        }
        else if(match("TRUE")){
            return new Ast.Expression.Literal(new Boolean(true));
        }
        else if(match("FALSE")){
            return new Ast.Expression.Literal(new Boolean(false));
        }
        else if(match(Token.Type.INTEGER)){
            String strInt = tokens.get(-1).getLiteral();
            //idk if 'new' necessary before BigInteger()
            return new Ast.Expression.Literal(new BigInteger(strInt));
        }
        else if(match(Token.Type.DECIMAL)){
            String strDec = tokens.get(-1).getLiteral();

            return new Ast.Expression.Literal(new BigDecimal(strDec));
        }
        else if(match(Token.Type.CHARACTER)){
            String Lit = tokens.get(-1).getLiteral();
                // HELP
            Lit = Lit.replace("\'", "");
            Lit = Lit.replace("\\", "");

            Lit = Lit.replace("\\b", "\b");
            Lit = Lit.replace("\\n", "\n");
            Lit = Lit.replace("\\r", "\r");
            Lit = Lit.replace("\\t", "\t");
            Lit = Lit.replace("\\'", "\'");
            Lit = Lit.replace("\\\"", "\""); // IDK
            Lit = Lit.replace("\\\\", "\\");

            char c = Lit.charAt(0);
            return new Ast.Expression.Literal(new Character(c));
        }
        else if(match(Token.Type.STRING)){
            String Lit = tokens.get(-1).getLiteral();
            // HELP could be error in removing \" characters inside string (not at ends) TODO
            Lit = Lit.replace("\"", "");



            Lit = Lit.replace("\\b", "\b");
            Lit = Lit.replace("\\n", "\n");
            Lit = Lit.replace("\\r", "\r");
            Lit = Lit.replace("\\t", "\t");
            Lit = Lit.replace("\\'", "\'");
            Lit = Lit.replace("\\\"", "\""); // IDK
            Lit = Lit.replace("\\\\", "\\");


            return new Ast.Expression.Literal(new String(Lit));
        }
        else if(match("(")){
            Ast.Expression.Group exp = new Ast.Expression.Group(parseExpression());
            if(!match(")")){
                //not 100% sure about index offset

                //if theres a token where ) should be
                if(tokens.has(0)){
                    //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    throw new ParseException("Expected closing parenthesis.", tokens.get(0).getIndex());

                }
                else{
                    //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    throw new ParseException("Expected closing parenthesis.", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }


            }

            return exp;
        }
        else if(match(Token.Type.IDENTIFIER)){
            String name = tokens.get(-1).getLiteral();


            if(match("(")){
                //function

                List<Ast.Expression> args = new ArrayList<>(); // may be wrong idk list must be initilized
                // CAN WE USE ARRAYLIST

                if(match(")")){
                    return new Ast.Expression.Function(name, args);
                                        //Collections.emptyList?

                }
                //must have at least 1 arg

                Ast.Expression exp = parseExpression();
                args.add(exp);
                //funct(stuff,
                while(tokens.has(1) && (!match(")"))){
                    if(!match(",")){
                        //System.out.println(tokens.get(0).getIndex());
                        throw new ParseException("Expected comma separating arguments.", tokens.get(0).getIndex());
                    }

                    Ast.Expression exp1 = parseExpression();
                    args.add(exp1);

                }


                if(!")".equals(tokens.get(0).getLiteral())){
                    //function doesnt close ()
                    throw new ParseException("Expected closing parenthesis.", tokens.get(0).getIndex());
                }
                return new Ast.Expression.Function(name, args);

            }
            else if(match("[")){
                if(match("]")){
                    return new Ast.Expression.Access(Optional.empty(), name);
                }
                Ast.Expression exp = parseExpression();
                if(!match("]")){
                    throw new ParseException("Expected closing bracket.", tokens.get(-1).getIndex()+1);
                }
                //not sure if necessary
                return new Ast.Expression.Access(Optional.of(exp), name);
            }

            return new Ast.Expression.Access(Optional.empty(), name);
        }

        else {
            // when inputtng ? , only one index (0)
            if(tokens.has(0)){
                //System.out.println(tokens.get(0).getIndex());
                throw new ParseException("Invalid primary expession", tokens.get(0).getIndex());
            }
            else{
                //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                throw new ParseException("Missing primary expession", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }

        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for(int i = 0; i < patterns.length; i++){
            if(!tokens.has(i)){
                return false;
            } else if(patterns[i] instanceof Token.Type){
                if(patterns[i] != tokens.get(i).getType()){
                    return false;
                }
            } else if(patterns[i] instanceof String){
                if(!patterns[i].equals(tokens.get(i).getLiteral())){
                    return false;
                }
            } else{
                throw new AssertionError("invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;

    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if(peek){
            for (int i = 0; i < patterns.length; i++){
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
