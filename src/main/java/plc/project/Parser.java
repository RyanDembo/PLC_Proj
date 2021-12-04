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
        List<Ast.Global> globes = new ArrayList<>();
        List<Ast.Function> functs = new ArrayList<>();

        while(tokens.has(0) && (peek("LIST") || peek("VAR") || peek("VAL"))){
            globes.add(parseGlobal());
        }
        while(tokens.has(0) && (peek("FUN")) ){
            functs.add(parseFunction());
        }

        if(tokens.has(0)){
            throw new ParseException("Expected end of code. Related: Functions go after global variables, no exceptions", tokens.get(0).getIndex());
        }
        return new Ast.Source(globes, functs);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO

        if(match("LIST")){
            Ast.Global list = parseList();
            if(!match(";")){
                if(tokens.has(0)){
                    //System.out.println(tokens.get(0).getIndex());
                    throw new ParseException("semicolon expected", tokens.get(0).getIndex());

                }
                else{
                    // System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    throw new ParseException("semicolon expected", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }
            }
            return list;
        }
        else if(match("VAR")){
            Ast.Global mutable = parseMutable();
            if(!match(";")){
                if(tokens.has(0)){
                    //System.out.println(tokens.get(0).getIndex());
                    throw new ParseException("semicolon expected", tokens.get(0).getIndex());

                }
                else{
                    // System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    throw new ParseException("semicolon expected", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }
            }
            return mutable;
        }
        else if(match("VAL")){
            Ast.Global immutable = parseImmutable();
            if(!match(";")){
                //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                //throw new ParseException("semicolon expected", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                if(tokens.has(0)){
                    //System.out.println(tokens.get(0).getIndex());
                    throw new ParseException("semicolon expected", tokens.get(0).getIndex());

                }
                else{
                   // System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    throw new ParseException("semicolon expected", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }
            }
            return immutable;
        }else{
            throw new ParseException("expected list, mutable, or immutable", 0);
        }

    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO

        if(!(tokens.has(0) && tokens.get(0).getType().equals(Token.Type.IDENTIFIER))){
            throw new ParseException("Expected Identifier", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }
        String id = tokens.get(0).getLiteral();
        tokens.advance();

        if(!match("=")){
            throw new ParseException("missing =", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        if(!match("[")){
            throw new ParseException("open bracket expected", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        if(!tokens.has(0)){
            throw new ParseException("expected expression", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        List<Ast.Expression> expressions = new ArrayList<>();
        expressions.add(parseExpression());

        while(tokens.has(0) && (!match("]"))){


            if(!match(",")){
                //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                //throw new ParseException("comma expected", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                if(tokens.has(0)){
                    //System.out.println(tokens.get(0).getIndex());
                    throw new ParseException("Expected comma or closing bracket", tokens.get(0).getIndex());

                }
                else{
                    //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    throw new ParseException("Expected comma or closing bracket", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }
            }
            expressions.add(parseExpression());
        }

        /*
        if(!match("]")){
            throw new ParseException("closing bracket expected", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }
*/

        if(!"]".equals(tokens.get(-1).getLiteral())){
            //list doesnt close []
            //
            //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            throw new ParseException("closing bracket expected", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        Ast.Expression.PlcList exprList = new Ast.Expression.PlcList(expressions);
        return new Ast.Global(id, true, Optional.of(exprList));

    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO

        if(!(tokens.has(0) && tokens.get(0).getType().equals(Token.Type.IDENTIFIER))){
            throw new ParseException("Expected Identifier", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        String id = tokens.get(0).getLiteral();
        tokens.advance();

        if(tokens.has(0)){
            if(match(":")){
                String type = tokens.get(0).getLiteral();
                tokens.advance();

                if(!match("=")){
                    throw new ParseException("missing =", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }

                if(!tokens.has(0)){
                    throw new ParseException("missing expression", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }

                Ast.Expression expr = parseExpression();

                return new Ast.Global(id,  type,true, Optional.of(expr));

            }

            if(!match("=")){
                throw new ParseException("missing =", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }
            if(!tokens.has(0)){
                throw new ParseException("missing expression", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }

            Ast.Expression expr = parseExpression();

            return new Ast.Global(id, true, Optional.of(expr));

        }else{
            return new Ast.Global(id, true, Optional.empty());
        }

    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO

        if(!(tokens.has(0) && tokens.get(0).getType().equals(Token.Type.IDENTIFIER))){
            throw new ParseException("Expected Identifier", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        String id = tokens.get(0).getLiteral();
        tokens.advance();

        if(tokens.has(0) && match(":")){
            String type = tokens.get(0).getLiteral();
            tokens.advance();

            if(!match("=")){
                throw new ParseException("missing =", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }

            if(!tokens.has(0)){
                throw new ParseException("missing expression", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }

            Ast.Expression expr = parseExpression();

            return new Ast.Global(id,  type,false, Optional.of(expr));

        }

        if(!match("=")){
            throw new ParseException("missing =", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        if(!tokens.has(0)){
            throw new ParseException("missing expression", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        Ast.Expression expr = parseExpression();

        return new Ast.Global(id, false, Optional.of(expr));

    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Function parseFunction() throws ParseException {
        //whole function may be bust if !match doesn't work as expected
        if(!match("FUN")){
            if(tokens.has(0)){
                throw new ParseException("Expected 'FUN' keyword", tokens.get(0).getIndex());
            }
            else{
                //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                throw new ParseException("Expected 'FUN' keyword", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }
        }

        if(!match(Token.Type.IDENTIFIER)){
            if(tokens.has(0)){
                throw new ParseException("Expected Identifier", tokens.get(0).getIndex());
            }
            else{
                System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                throw new ParseException("Expected Identifier", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }
        }

        String fname = tokens.get(-1).getLiteral();
        String returnType = "";
        boolean hasType = false;

        if(!match("(")){
            if(tokens.has(0)){

                throw new ParseException("Expected opening parenthesis.", tokens.get(0).getIndex());

            }
            else{
                //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                throw new ParseException("Expected opening parenthesis.", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }
        }
        List<String> args = new ArrayList<>();
        List<String> paramTypes = new ArrayList<>();

        if(match(")")){
            //no arguments

            if(tokens.has(0) && match(":")){
                hasType = true;
                returnType = tokens.get(0).getLiteral();
                tokens.advance();
            }

            if(!match("DO")){
                if(tokens.has(0)){
                    throw new ParseException("Expected 'DO' keyword", tokens.get(0).getIndex());
                }
                else{
                    //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    throw new ParseException("Expected 'DO' keyword", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }
            }

            List<Ast.Statement> stats = parseBlock();

            if(!match("END")){
                if(tokens.has(0)){
                    throw new ParseException("Expected 'END' keyword", tokens.get(0).getIndex());
                }
                else{
                    //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    throw new ParseException("Expected 'END' keyword", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }
            }

            if(hasType){
                return new Ast.Function(fname, args, paramTypes, Optional.of(returnType), stats);
            }else{
                return new Ast.Function(fname, args, stats);
            }

        }
        else{
            //must have at least 1 arg
            // may need to use has(0)
            if(!match(Token.Type.IDENTIFIER)){
                if(tokens.has(0)){
                    throw new ParseException("Expected Identifier", tokens.get(0).getIndex());
                }
                else{
                    //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    throw new ParseException("Expected Identifier", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }
            }

            String Aname = tokens.get(-1).getLiteral();
            args.add(Aname);

            if(tokens.has(0) && match(":")){
                String p = tokens.get(0).getLiteral();
                paramTypes.add(p);
            }


            //funct(stuff,
            while(tokens.has(1) && (!match(")"))){
                if(!match(",")){
                    //System.out.println(tokens.get(0).getIndex());
                    throw new ParseException("Expected comma separating arguments.", tokens.get(0).getIndex());
                }

                if(!match(Token.Type.IDENTIFIER)){
                    if(tokens.has(0)){
                        throw new ParseException("Expected Identifier", tokens.get(0).getIndex());
                    }
                    else{
                        //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                        throw new ParseException("Expected Identifier", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    }
                }
                String Aname2 = tokens.get(-1).getLiteral();
                args.add(Aname2);

                if(tokens.has(0) && match(":")){
                    String p = tokens.get(0).getLiteral();
                    paramTypes.add(p);
                }
            }


            if(!tokens.has(0) || !")".equals(tokens.get(0).getLiteral())){
                //function doesnt close ()
                //funct(stuff
                //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                throw new ParseException("Expected closing parenthesis.", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }
            //return new Ast.Function(fname, args, stats);

            //List<Ast.Statement> stats = parseBlock();

            if(!match("DO")){
                if(tokens.has(0)){
                    throw new ParseException("Expected 'DO' keyword", tokens.get(0).getIndex());
                }
                else{
                    //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    throw new ParseException("Expected 'DO' keyword", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }
            }

            List<Ast.Statement> stats = parseBlock();

            if(!match("END")){
                if(tokens.has(0)){
                    throw new ParseException("Expected 'END' keyword", tokens.get(0).getIndex());
                }
                else{
                    //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    throw new ParseException("Expected 'END' keyword", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }
            }

            return new Ast.Function(fname, args, stats);

        }


    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> stats = new ArrayList<>();

        // not sure if covers zero statement case completely
        if(!tokens.has(0)){
            return stats;
        }
        if(peek("END")){
            return stats;
        }

        stats.add(parseStatement());

        //not sure if will work properly
        while(!peek("END") && !peek("CASE") && !peek("DEFAULT") && !peek("ELSE")){
            stats.add(parseStatement());
        }

        return stats;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */


    public Ast.Statement parseStatement() throws ParseException {

        if(match("LET")) {
            return parseDeclarationStatement();
        }
        else if(match("SWITCH")) {
            return parseSwitchStatement();
        }
        else if(match("IF")) {
            return parseIfStatement();
        }
        else if(match("WHILE")) {
            return parseWhileStatement();
        }
        else if(match("RETURN")) {
            return parseReturnStatement();
        }else{
            Ast.Expression exp = parseExpression();

            Ast.Statement.Expression expression = new Ast.Statement.Expression(exp);
            if(tokens.has(0)){

                if(match(";")){
                    return expression;
                }

                if(match("=")){

                    if(!tokens.has(0)){
                        //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                        throw new ParseException("Missing RHS expression", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    }

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
                    throw new ParseException("invalid expression", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
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

    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        if(tokens.has(0) && tokens.get(0).getType().equals(Token.Type.IDENTIFIER)){
            String name = tokens.get(0).getLiteral();
            tokens.advance();

            if(tokens.has(0) && match(":")){
                String type = tokens.get(0).getLiteral();
                tokens.advance();

                if(tokens.has(0) && match("=")){
                    Ast.Expression exp1 = parseExpression();
                    if(!match(";")){
                        throw new ParseException("Missing Semicolon", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    }
                    return new Ast.Statement.Declaration(name, Optional.of(type), Optional.of(exp1));
                }

                return new Ast.Statement.Declaration(name, Optional.of(type), Optional.empty());

            }

            if(tokens.has(0) && match("=")){
                Ast.Expression exp1 = parseExpression();
                if(!match(";")){
                    throw new ParseException("Missing Semicolon", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }
                return new Ast.Statement.Declaration(name, Optional.of(exp1));
            }

            if(!match(";")){
                throw new ParseException("Missing Semicolon", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }

            return new Ast.Statement.Declaration(name, Optional.empty());

        }else{
            throw new ParseException("invalid declaration", tokens.get(0).getIndex());
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        Ast.Expression cond = parseExpression();

        if(match("DO")){
            List<Ast.Statement> thenStatements = parseBlock();
            List<Ast.Statement> elseStatements = new ArrayList<>();
            if(tokens.has(0) && match("ELSE")){
                if(tokens.has(0)){
                    elseStatements = parseBlock();
                }else{
                    //throw new ParseException("missing else statements", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    if(tokens.has(0)){
                        //System.out.println(tokens.get(0).getIndex());
                        throw new ParseException("missing else statements", tokens.get(0).getIndex());
                    }
                    else{
                        //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                        throw new ParseException("missing else statements", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    }
                }
            }
            Ast.Statement.If ifStatement = new Ast.Statement.If(cond, thenStatements, elseStatements);

            if(!match("END")){
                //throw new ParseException("missing 'END'", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                if(tokens.has(0)){
                    //System.out.println(tokens.get(0).getIndex());
                    throw new ParseException("missing 'END'", tokens.get(0).getIndex());
                }
                else{
                    //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    throw new ParseException("missing 'END'", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }
            }

            return ifStatement;

        }else{

            if(tokens.has(0)){
                //System.out.println(tokens.get(0).getIndex());
                throw new ParseException("missing 'DO'", tokens.get(0).getIndex());
            }
            else{
                //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                throw new ParseException("missing 'DO'", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }



        }
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {

        List<Ast.Statement.Case> cases = new ArrayList<>();

        if(!tokens.has(0)){
            throw new ParseException("missing switch expression", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        Ast.Expression switchExpr = parseExpression();

        while(match("CASE")){
            cases.add(parseCaseStatement());
        }

        if(match("DEFAULT")){
            Ast.Statement.Case defaultCase = new Ast.Statement.Case(Optional.empty(), parseBlock());
            cases.add(defaultCase);
        }else{
            throw new ParseException("missing 'DEFAULT'", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        if(!match("END")){
            throw new ParseException("missing 'END'", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        return new Ast.Statement.Switch(switchExpr, cases);
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {

        if(!tokens.has(0)){
            throw new ParseException("missing case expression", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        Ast.Expression expr = parseExpression();

        if(!match(":")){
            throw new ParseException("missing colon", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        List<Ast.Statement> statements = parseBlock();

        return new Ast.Statement.Case(Optional.of(expr), statements);

    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {

        if(!tokens.has(0)){
            throw new ParseException("missing while expression", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        Ast.Expression whileExpr = parseExpression();

        if(!match("DO")){
            throw new ParseException("missing 'DO'", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        List<Ast.Statement> stats = parseBlock();

        if(!match("END")){
            throw new ParseException("missing 'END'", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        return new Ast.Statement.While(whileExpr, stats);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {

        if(!tokens.has(0)){
            throw new ParseException("missing return expression", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        Ast.Expression expr = parseExpression();
        Ast.Statement.Return r = new Ast.Statement.Return(expr);

        if(!match(";")){
            throw new ParseException("missing semicolon", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
        }

        return r;
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
                throw new ParseException("missing logical RHS", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }

            Ast.Expression.Binary expression = new Ast.Expression.Binary(strOp, compExpression, parseComparisonExpression());

            while(tokens.has(0) && (match("&&") || match("||"))){
                strOp = tokens.get(-1).getLiteral();
                if(tokens.has(0)){
                    expression = new Ast.Expression.Binary(strOp, expression, parseComparisonExpression());
                }else{
                    throw new ParseException("missing logical RHS", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
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
                throw new ParseException("missing comparison RHS", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }

            Ast.Expression.Binary expression = new Ast.Expression.Binary(strOp, addExpression, parseAdditiveExpression());

            while(tokens.has(0) && (match("<") || match(">") || match("==") || match("!="))){
                strOp = tokens.get(-1).getLiteral();

                if(tokens.has(0)){
                    expression = new Ast.Expression.Binary(strOp, expression, parseAdditiveExpression());
                }else{
                    throw new ParseException("missing comparison RHS", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
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
                throw new ParseException("missing additive RHS", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }

            Ast.Expression.Binary expression = new Ast.Expression.Binary(strOp, multExpression, parseMultiplicativeExpression());

            while(tokens.has(0) && (match("+") || match("-"))){
                strOp = tokens.get(-1).getLiteral();
                if(tokens.has(0)){
                    expression = new Ast.Expression.Binary(strOp, expression, parseMultiplicativeExpression());
                }else{
                    throw new ParseException("missing additive RHS", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
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

                throw new ParseException("missing multiplicative RHS", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
            }

            Ast.Expression.Binary expression = new Ast.Expression.Binary(strOp, primExpression, parsePrimaryExpression());

            while(tokens.has(0) && (match("*") || match("/") || match("^"))){
                strOp = tokens.get(0).getLiteral();
                if(tokens.has(0)){
                    expression = new Ast.Expression.Binary(strOp, expression, parsePrimaryExpression());
                }else{

                    throw new ParseException("missing multiplicative RHS", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
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
                while(tokens.has(0) && (!match(")"))){
                    if(!match(",")){
                        //System.out.println(tokens.get(0).getIndex());
                        throw new ParseException("Expected comma separating arguments.", tokens.get(0).getIndex());
                    }

                    Ast.Expression exp1 = parseExpression();
                    args.add(exp1);

                }


                if(!")".equals(tokens.get(-1).getLiteral())){
                    //function doesnt close ()
                    //funct(stuff
                    //System.out.println(tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                    throw new ParseException("Expected closing parenthesis.", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
                }
                return new Ast.Expression.Function(name, args);

            }
            else if(match("[")){
                if(match("]")){
                    return new Ast.Expression.Access(Optional.empty(), name);
                }
                Ast.Expression exp = parseExpression();
                if(!match("]")){
                    throw new ParseException("Expected closing bracket.", tokens.get(-1).getIndex()+tokens.get(-1).getLiteral().length());
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