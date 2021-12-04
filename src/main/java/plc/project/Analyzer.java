package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        //throw new UnsupportedOperationException();  // TODO
        Environment.Function Main = scope.lookupFunction("main",0);

        if(!(Main.getReturnType().equals(Environment.Type.INTEGER))){
            throw new RuntimeException("main must return an integer");
        }

        for(Ast.Global glob : ast.getGlobals()){
            visit(glob);
        }
        for(Ast.Function funct : ast.getFunctions()){
            visit(funct);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        //throw new UnsupportedOperationException();  // TODO
        if(ast.getValue().isPresent()){
            visit(ast.getValue().get());
            if(!(ast.getValue().get().getType().equals(Environment.getType(ast.getTypeName())))){
                throw new RuntimeException("value not assignable");
            }
        }
        scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), ast.getMutable(), Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        //throw new UnsupportedOperationException();  // TODO
        List<Environment.Type> paramTypes = new ArrayList<>();
        for(String pt : ast.getParameterTypeNames() ){
            paramTypes.add(Environment.getType(pt));
        }

        Environment.Type rType;
        if(ast.getReturnTypeName().isPresent()){
            rType = Environment.getType(ast.getReturnTypeName().get());
            scope.defineFunction(ast.getName(), ast.getName(), paramTypes, rType, args -> Environment.NIL);
            ast.setFunction(scope.lookupFunction(ast.getName(), 0));

            try{
                scope = new Scope(scope);
                scope.defineVariable("RETURN", false, Environment.create(Environment.getType(ast.getReturnTypeName().get())));
                for(String param : ast.getParameters()){
                    scope.defineVariable(param, true, Environment.NIL);
                }
                for(Ast.Statement stmt : ast.getStatements()){
                    visit(stmt);
                }
            }
            finally{
                scope = scope.getParent();
            }

            //scope.defineVariable("RETURN", false, Environment.create(Environment.getType(ast.getReturnTypeName().get())));
        }else{
            rType = Environment.Type.NIL;
            scope.defineFunction(ast.getName(), ast.getName(), paramTypes, rType, args -> Environment.NIL);
            ast.setFunction(scope.lookupFunction(ast.getName(), 0));
            try{

                scope = new Scope(scope);
                scope.defineVariable("RETURN", false, Environment.NIL);
                //scope.defineVariable("RETURN", false, Environment.create(Environment.getType(ast.getReturnTypeName().get())));
                for(String param : ast.getParameters()){
                    scope.defineVariable(param, true, Environment.NIL);
                }
                for(Ast.Statement stmt : ast.getStatements()){
                    visit(stmt);
                }
            }
            finally{
                scope = scope.getParent();
            }
        }
        ast.setFunction(scope.lookupFunction(ast.getName(), 0));
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        //throw new UnsupportedOperationException();  // TODO
        if(!(ast.getExpression() instanceof Ast.Expression.Function)){
            throw new RuntimeException("not an expression.function");
        }
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        //throw new UnsupportedOperationException();  // TODO
        Environment.Type t;

        if(ast.getTypeName().isPresent()){
            t = Environment.getType(ast.getTypeName().get());
            if(ast.getValue().isPresent()){
                visit(ast.getValue().get());
                if(!(ast.getValue().get().getType().equals(Environment.getType(ast.getTypeName().get())))){
                    throw new RuntimeException("value not assignable");
                }
            }
            scope.defineVariable(ast.getName(), ast.getName(), t, true, Environment.NIL);
            ast.setVariable(scope.lookupVariable(ast.getName()));
        }
        else if(ast.getValue().isPresent()){
            visit(ast.getValue().get());
            t = ast.getValue().get().getType();
            scope.defineVariable(ast.getName(), ast.getName(), t, true, Environment.NIL);
            ast.setVariable(scope.lookupVariable(ast.getName()));
        } else {
            throw new RuntimeException("neither variable nor value type defined");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        //throw new UnsupportedOperationException();  // TODO
        if(!(ast.getReceiver() instanceof Ast.Expression.Access)){
            throw new RuntimeException("Receiver not access expression");
        }

        visit(ast.getReceiver());
        visit(ast.getValue());

        if(ast.getReceiver().getType() == Environment.Type.BOOLEAN){
            requireAssignable(ast.getValue().getType(), Environment.Type.BOOLEAN);
        }
        else if(ast.getReceiver().getType() == Environment.Type.INTEGER){
            requireAssignable(ast.getValue().getType(), Environment.Type.INTEGER);
        }
        else if(ast.getReceiver().getType() == Environment.Type.DECIMAL){
            requireAssignable(ast.getValue().getType(), Environment.Type.DECIMAL);
        }
        else if(ast.getReceiver().getType() == Environment.Type.CHARACTER){
            requireAssignable(ast.getValue().getType(), Environment.Type.CHARACTER);
        }
        else if(ast.getReceiver().getType() == Environment.Type.STRING){
            requireAssignable(ast.getValue().getType(), Environment.Type.STRING);
        }
        else if(ast.getReceiver().getType() == Environment.Type.ANY){
            requireAssignable(ast.getValue().getType(), Environment.Type.ANY);
        }
        else {
            requireAssignable(ast.getValue().getType(), Environment.Type.NIL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        //throw new UnsupportedOperationException();  // TODO
        visit(ast.getCondition());
        if(!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)){
            throw new RuntimeException("condition is not a boolean");
        }
        if(ast.getThenStatements().size() == 0){
            throw new RuntimeException("No Then Statements");
        }

        try{
            scope = new Scope(scope);
            for(Ast.Statement stmt : ast.getThenStatements() ){
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        try{
            scope = new Scope(scope);
            for(Ast.Statement stmt : ast.getElseStatements() ){
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        //throw new UnsupportedOperationException();  // TODO
        Ast.Expression condition = ast.getCondition();

        List<Ast.Statement.Case> switchCases = ast.getCases();
        if(switchCases.get(switchCases.size()-1).getValue().isPresent()){
            throw new RuntimeException("value in last case");
        }

        visit(condition);
        scope = new Scope(scope);
        try{
            for(Ast.Statement.Case c : switchCases){
                visit(c);
                if(c.getValue().isPresent()){
                    visit(c.getValue().get());
                    requireAssignable(c.getValue().get().getType(), condition.getType());
                }
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        //throw new UnsupportedOperationException();  // TODO
        try{
            //new scope       //parent scope
            scope = new Scope(scope);

            for(Ast.Statement stmt : ast.getStatements()){
                visit(stmt); //visits all statements
            }
        }
        finally{
            scope = scope.getParent(); // returns to parent scope
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        //throw new UnsupportedOperationException();  // TODO
        visit(ast.getValue());
        Environment.Type t = scope.lookupVariable("RETURN").getType();
        requireAssignable(t, ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        // throw new UnsupportedOperationException();  // TODO
        if(ast.getLiteral() instanceof Boolean){
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if(ast.getLiteral() instanceof String){
            ast.setType(Environment.Type.STRING);
        }
        else if(ast.getLiteral() instanceof Character){
            ast.setType(Environment.Type.CHARACTER);
        }
        else if(ast.getLiteral() instanceof BigInteger){
            BigInteger Max = new BigInteger(String.valueOf(Integer.MAX_VALUE));

            BigInteger Min = new BigInteger(String.valueOf(Integer.MIN_VALUE));

            if((((BigInteger) ast.getLiteral()).compareTo(Min) == -1) || (((BigInteger) ast.getLiteral()).compareTo(Max) == 1)){
                throw new RuntimeException("Integer out of Range");

            }

            ast.setType(Environment.Type.INTEGER);
        }
        else if(ast.getLiteral() instanceof BigDecimal){
            double test = ((BigDecimal) ast.getLiteral()).doubleValue();

            if(test == Double.NEGATIVE_INFINITY || test == Double.POSITIVE_INFINITY){
                throw new RuntimeException("Decimal out of Range");
            }

            ast.setType(Environment.Type.DECIMAL);
        }
        else if(ast.getLiteral() instanceof Environment.PlcObject){
            if(ast.getLiteral().equals(Environment.NIL)){
                ast.setType(Environment.Type.NIL);
            }
            else{
                throw new RuntimeException("Literal has nonexistent type");
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        //throw new UnsupportedOperationException();  // TODO
        if(ast.getExpression() instanceof Ast.Expression.Binary){
            visit(ast.getExpression());

            ast.setType(ast.getExpression().getType());
        }
        else{
            throw new RuntimeException("Grouped expression not Binary");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        //throw new UnsupportedOperationException();  // TODO
        visit(ast.getRight());
        visit(ast.getLeft());
        switch (ast.getOperator()){
            case "&&":
            case "||":{
                //not using requireAssignable to be exact w boolean type (annoying)

                if(!ast.getRight().getType().equals(Environment.Type.BOOLEAN)){
                    throw new RuntimeException("Right value not Boolean");
                }
                if(!ast.getLeft().getType().equals(Environment.Type.BOOLEAN)){
                    throw new RuntimeException("Left value not Boolean");
                }

                ast.setType(Environment.Type.BOOLEAN);
                break;
            }
            case "<":
            case ">":
            case "==":
            case "!=":{
                if(!ast.getRight().getType().equals(Environment.Type.COMPARABLE)){
                    throw new RuntimeException("Right value not comparable");
                }
                if(!ast.getLeft().getType().equals(Environment.Type.COMPARABLE)){
                    throw new RuntimeException("Left value not comparable");
                }
                //TEST
                //if(!ast.getLeft().getClass().equals(ast.getRight().getClass())){
                //  throw new RuntimeException("Operand types dont match");
                //}
                if(!ast.getLeft().getType().equals(ast.getRight().getType())){
                    throw new RuntimeException("Types not equivalent");
                }
                ast.setType(Environment.Type.BOOLEAN);
                break;
            }
            case "+":{
                if(ast.getRight().getType().equals(Environment.Type.STRING) || ast.getLeft().getType().equals(Environment.Type.STRING)){
                    ast.setType(Environment.Type.STRING);
                }
                else if(ast.getLeft().getType().equals(Environment.Type.INTEGER)){
                    if(ast.getRight().getType().equals(Environment.Type.INTEGER)){
                        ast.setType(Environment.Type.INTEGER);
                    }
                    else{
                        throw new RuntimeException("mismatching numerical types");
                    }
                }
                else if(ast.getLeft().getType().equals(Environment.Type.DECIMAL)){
                    if(ast.getRight().getType().equals(Environment.Type.DECIMAL)){
                        ast.setType(Environment.Type.DECIMAL);
                    }
                    else{
                        throw new RuntimeException("mismatching numerical types");
                    }
                }
                else{
                    throw new RuntimeException("invalid addition types");
                } break;
            }
            case "-":
            case "*":
            case "/":{
                if(ast.getLeft().getType().equals(Environment.Type.INTEGER)){
                    if(ast.getRight().getType().equals(Environment.Type.INTEGER)){
                        ast.setType(Environment.Type.INTEGER);
                    }
                    else{
                        throw new RuntimeException("mismatching numerical types");
                    }
                }
                else if(ast.getLeft().getType().equals(Environment.Type.DECIMAL)){
                    if(ast.getRight().getType().equals(Environment.Type.DECIMAL)){
                        ast.setType(Environment.Type.DECIMAL);
                    }
                    else{
                        throw new RuntimeException("mismatching numerical types");
                    }
                }
                else{
                    throw new RuntimeException("invalid numerical types");
                } break;
            }
            case "^":{
                if(ast.getLeft().getType().equals(Environment.Type.INTEGER)){
                    if(ast.getRight().getType().equals(Environment.Type.INTEGER)){
                        ast.setType(Environment.Type.INTEGER);
                    }
                    else{
                        throw new RuntimeException("RHS not an int");
                    }
                }
                else if(ast.getLeft().getType().equals(Environment.Type.DECIMAL)){
                    if(ast.getRight().getType().equals(Environment.Type.INTEGER)){
                        ast.setType(Environment.Type.DECIMAL);
                    }
                    else{
                        throw new RuntimeException("RHS not an int");
                    }
                }
                else{
                    throw new RuntimeException("invalid numerical types");
                }
                break;
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        //throw new UnsupportedOperationException();  // TODO

        ast.setVariable(scope.lookupVariable(ast.getName()));

        if(ast.getOffset().isPresent()){
            visit(ast.getOffset().get());
            if(ast.getOffset().get().getType() != Environment.Type.INTEGER){
                throw new RuntimeException("offset type not integer");
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        //throw new UnsupportedOperationException();  // TODO
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));

        List<Ast.Expression> args = ast.getArguments();
        List<Environment.Type> pTypes = ast.getFunction().getParameterTypes();

        if(args.size() != pTypes.size()){
            throw new RuntimeException("Parameter/argument size mismatch");
        }

        for(int i = 0; i < pTypes.size(); i++){
            visit(args.get(i));
            requireAssignable(pTypes.get(i), args.get(i).getType());

        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        //throw new UnsupportedOperationException();  // TODO
        //Environment.Type t = scope.lookupVariable(ast.)

        for(Ast.Expression expr : ast.getValues()){
            visit(expr);
            requireAssignable(expr.getType(), ast.getType());
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(target.equals(type)){
            return;
        }
        else if(target.getName().equals("Any")){
            return;
        }
        else if(target.getName().equals("Comparable")){
            if(type.getName().equals("Integer") || type.getName().equals("Decimal") || type.getName().equals("Character") ||
                    type.getName().equals("String")){

                return;
            }
            else{
                throw new RuntimeException("Not Comparable Type");
            }
        }
        else{
            throw new RuntimeException("Not Assignable Type");
        }
    }

}
